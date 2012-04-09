/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.server.listener;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.timer.HashedWheelTimer;
import com.taobao.gecko.service.timer.Timeout;
import com.taobao.gecko.service.timer.Timer;
import com.taobao.gecko.service.timer.TimerTask;


public class ConnectionMetaDataTimerListener implements ConnectionLifeCycleListener {

    private static final Log log = LogFactory.getLog(ConnectionMetaDataTimerListener.class);

    private final Timer timer = new HashedWheelTimer();

    private final static long delay = 60L;

    public static String CONN_META_DATA_ATTR = ConnectionMetaDataTimerListener.class.getName() + "_conn_metadata_attr";
    public static String CONN_METADATA_TIMEOUT_ATTR = ConnectionMetaDataTimerListener.class.getName()
            + "_conn_metadata_timeout_attr";


    public void onConnectionCreated(final Connection conn) {
        try {
            Timeout timeout = timer.newTimeout(new TimerTask() {

                public void run(Timeout timeout) throws Exception {
                    // 如果连接没有推送元，则断开连接
                    if (!timeout.isCancelled() && conn.isConnected()) {
                        if (conn.getAttribute(CONN_META_DATA_ATTR) == null) {
                            log.error("连接" + conn.getRemoteSocketAddress() + "没有推送clientInterests，主动关闭连接");
                            conn.close(false);
                        }
                    }

                }
            }, delay, TimeUnit.SECONDS);
            // 已经添加了，取消最新的
            if (conn.setAttributeIfAbsent(CONN_METADATA_TIMEOUT_ATTR, timeout) != null) {
                timeout.cancel();
            }
        }
        catch (Throwable t) {
            try {
                conn.close(false);
            }
            catch (NotifyRemotingException e) {
                // ignore
            }
            log.error("添加定时器失败", t);
        }

    }


    public void onConnectionReady(Connection conn) {
    }


    public void onConnectionClosed(Connection conn) {
        Timeout timeout = (Timeout) conn.getAttribute(CONN_METADATA_TIMEOUT_ATTR);
        if (timeout != null) {
            timeout.cancel();
        }
    }

}
