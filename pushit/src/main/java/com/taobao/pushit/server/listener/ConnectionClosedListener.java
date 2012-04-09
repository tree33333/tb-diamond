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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.pushit.commons.Constants;
import com.taobao.pushit.network.NotifyCommand;



public class ConnectionClosedListener implements ConnectionLifeCycleListener {

    private static final Log log = LogFactory.getLog(ConnectionClosedListener.class);

    private final RemotingServer server;


    public ConnectionClosedListener(RemotingServer server) {
        this.server = server;
    }


    public void onConnectionCreated(Connection conn) {
    }


    public void onConnectionReady(Connection conn) {
    }


    /**
     * 当连接和server断开时通知感兴趣的订阅者
     */
    public void onConnectionClosed(Connection conn) {
        String message = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        NotifyCommand notify = new NotifyCommand(Constants.REAPER_ID, Constants.DEFAULT_GROUP, message);
        try {
            server.sendToGroupAllConnections(Constants.REAPER_REMOTING_GROUP, notify);
        }
        catch (Exception e) {
            log.error("对外通知关闭连接事件失败", e);
        }
    }
}
