/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.server;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.timer.Timeout;
import com.taobao.pushit.commons.ClientInterest;
import com.taobao.pushit.network.InterestCommand;
import com.taobao.pushit.network.NotifyCommand;
import com.taobao.pushit.server.listener.ConnectionMetaDataTimerListener;


/**
 * Push服务，将notify command推送给对应的客户端
 * 
 * @author boyan
 * @Date 2011-5-19
 * 
 */
public class PushService {
    private final RemotingServer remotingServer;

    private static final Log log = LogFactory.getLog(PushService.class);


    public PushService(RemotingServer remotingServer) {
        super();
        this.remotingServer = remotingServer;
    }


    /**
     * 将通知广播给客户端
     * 
     * @param notifyCommand
     */
    public void broadcastClients(NotifyCommand notifyCommand) {
        // 使用ThreadLocal保存encode后的buf做缓存，避免复制
        notifyCommand.setUseLocalCache(true);
        try {
            remotingServer.sendToGroupAllConnections(getRemotingGroup(notifyCommand.getDataId(),
                notifyCommand.getGroup()), notifyCommand);
        }
        catch (NotifyRemotingException e) {
            log.error("Push notify failed", e);
        }
        finally {
            // 切记清除缓存
            NotifyCommand.resetLocalBuf();
        }
    }


    private String getRemotingGroup(String dataId, String group) {
        return dataId + "-" + group;
    }


    public void handleInterests(InterestCommand cmd, Connection conn) {
        List<ClientInterest> clientInterests = cmd.getClientInterests();
        if (clientInterests != null) {
            for (ClientInterest interest : clientInterests) {
                log.info("Add connection " + RemotingUtils.getAddrString(conn.getRemoteSocketAddress()) + " to group "
                        + getRemotingGroup(interest.dataId, interest.group));
                remotingServer.getRemotingContext().addConnectionToGroup(
                    getRemotingGroup(interest.dataId, interest.group), conn);
                
                // 标记已经接收到元信息
                conn.setAttribute(ConnectionMetaDataTimerListener.CONN_META_DATA_ATTR, Boolean.TRUE);
                // 取消定时器
                Timeout timeout =
                        (Timeout) conn.getAttribute(ConnectionMetaDataTimerListener.CONN_METADATA_TIMEOUT_ATTR);
                if (timeout != null) {
                    timeout.cancel();
                }
            }
        }
    }
}
