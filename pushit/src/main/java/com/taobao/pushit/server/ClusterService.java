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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.StringUtils;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.pushit.commons.AddrUtils;
import com.taobao.pushit.network.BroadCastCommand;
import com.taobao.pushit.network.PushitWireFormatType;



/**
 * 集群管理器
 * 
 * @author boyan
 * @Date 2011-5-19
 * 
 */
public class ClusterService {
    private RemotingClient remotingClient;

    static final Log log = LogFactory.getLog(ClusterService.class);

    private volatile Set<String> clusterSet = new HashSet<String>();


    public ClusterService(String hosts) throws IOException {
        initRemotingClient();
        processClusterChanged(hosts);
    }

    private void initRemotingClient() throws IOException {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new PushitWireFormatType());
        clientConfig.setIdleTime(-1); // 禁止心跳检测
        clientConfig.setConnectTimeout(3000);
        remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        try {
            remotingClient.start();
        }
        catch (NotifyRemotingException e) {
            throw new IOException("Start remoting client failed", e);
        }
    }


    public void stop() {
        try {
            if (remotingClient != null) {
                remotingClient.stop();
            }
        }
        catch (NotifyRemotingException e) {
            log.error("关闭remotingClient出错", e);
        }
    }


    private synchronized void processClusterChanged(String serverList) {
        if (StringUtils.isBlank(serverList)) {
            return;
        }
        String[] hosts = serverList.split(",");
        Set<String> newSet = new HashSet<String>();
        Set<String> closeSet = new HashSet<String>();
        Set<String> connectSet = new HashSet<String>();
        for (String host : hosts) {
            final String url = AddrUtils.getUrlFromHost(host);
            newSet.add(url);
            if (!clusterSet.contains(url)) {
                connectSet.add(url);
            }
        }
        for (String url : clusterSet) {
            if (!newSet.contains(url)) {
                closeSet.add(url);
            }
        }
        for (String url : closeSet) {
            try {
                log.info("[Cluster]Closing  " + url);
                remotingClient.close(url, false);
            }
            catch (NotifyRemotingException e) {
                log.error("关闭" + url + "出错", e);
            }
        }
        for (String url : connectSet) {
            try {
                log.info("[Cluster]Connecting to  " + url);
                remotingClient.connect(url);
                remotingClient.awaitReadyInterrupt(url);
            }
            catch (NotifyRemotingException e) {
                log.error("连接" + url + "出错", e);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        clusterSet = newSet;
    }


    /**
     * 往集群里的所有机器发送命令
     * 
     * @param cmd
     */
    public void broadcastClusterWithoutMe(BroadCastCommand cmd, String me) {
        Set<String> copySet = clusterSet;
        for (String url : copySet) {
            if (!me.equals(url)) {
                try {
                    remotingClient.sendToGroup(url, cmd);
                }
                catch (NotifyRemotingException e) {
                    log.error("发送给服务器" + url + "出错", e);
                }
            }
        }
    }

}
