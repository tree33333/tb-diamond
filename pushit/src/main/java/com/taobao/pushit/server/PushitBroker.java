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
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.StringUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.pushit.network.BroadCastCommand;
import com.taobao.pushit.network.InterestCommand;
import com.taobao.pushit.network.NotifyCommand;
import com.taobao.pushit.network.PushitWireFormatType;
import com.taobao.pushit.server.listener.ConnectionClosedListener;
import com.taobao.pushit.server.listener.ConnectionNumberListener;


/**
 * Push服务器
 * 
 * @author boyan
 * @Date 2011-5-19
 * 
 */
public class PushitBroker {
    private final class BroadcastProcessor implements RequestProcessor<BroadCastCommand> {
        public ThreadPoolExecutor getExecutor() {
            return requestExecutor;
        }


        public void handleRequest(BroadCastCommand request, Connection conn) {
            // 集群内广播，发送给客户端
            pushService.broadcastClients(new NotifyCommand(request.getDataId(), request.getGroup(), request
                .getMessage()));
        }
    }

    private final class ClientInterestsProcessor implements RequestProcessor<InterestCommand> {
        public ThreadPoolExecutor getExecutor() {
            return requestExecutor;
        }


        public void handleRequest(InterestCommand request, Connection conn) {
            pushService.handleInterests(request, conn);
        }
    }

    private final class NotifyCommandProecssor implements RequestProcessor<NotifyCommand> {
        public ThreadPoolExecutor getExecutor() {
            return requestExecutor;
        }


        public void handleRequest(NotifyCommand request, Connection conn) {
            // 先广播给集群
            clusterService.broadcastClusterWithoutMe(new BroadCastCommand(request.getDataId(), request.getGroup(),
                request.getMessage()), myUrl);
            // 发送给客户端
            pushService.broadcastClients(request);
        }
    }

    static final Log log = LogFactory.getLog(PushitBroker.class);
    private final RemotingServer remotingServer;
    private final ThreadPoolExecutor requestExecutor;
    public static final int DEFAULT_PORT = 8609; // my wife's birthday

    public static final int DEFAULT_CONN_THRESHOLD = 2;
    public static final int DEFAULT_IPCOUNT_CHECK_INTERVAL = 15;
    public static final int DEFAULT_IPCOUNT_THRESHOLD = 3000;

    static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private final PushService pushService;
    private final ClusterService clusterService;
    private String myUrl;


    public PushitBroker(Properties props) throws IOException {
        int port = DEFAULT_PORT;
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        if (!StringUtils.isBlank(props.getProperty("port"))) {
            port = Integer.parseInt(props.getProperty("port"));
        }
        if (!StringUtils.isBlank(props.getProperty("requestThreadPoolSize"))) {
            threadPoolSize = Integer.parseInt(props.getProperty("requestThreadPoolSize"));
        }
        requestExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(port);
        serverConfig.setWireFormatType(new PushitWireFormatType());
        remotingServer = RemotingFactory.newRemotingServer(serverConfig);
        pushService = new PushService(remotingServer);
        String hosts = props.getProperty("cluster_hosts");
        if (StringUtils.isBlank(hosts)) {
            log.warn("没有配置集群服务器列表,集群功能将不可用");
        }
        clusterService = new ClusterService(hosts);
        remotingServer.registerProcessor(NotifyCommand.class, new NotifyCommandProecssor());
        remotingServer.registerProcessor(InterestCommand.class, new ClientInterestsProcessor());
        remotingServer.registerProcessor(BroadCastCommand.class, new BroadcastProcessor());
        remotingServer.addConnectionLifeCycleListener(new ConnectionClosedListener(remotingServer));
        // this.remotingServer.addConnectionLifeCycleListener(new
        // ConnectionMetaDataTimerListener());

        int connectionThreshold = DEFAULT_CONN_THRESHOLD;
        int ipCountCheckInterval = DEFAULT_IPCOUNT_CHECK_INTERVAL;
        int ipCountThreshold = DEFAULT_IPCOUNT_THRESHOLD;
        if (!StringUtils.isBlank(props.getProperty("conn_threshold"))) {
            connectionThreshold = Integer.parseInt(props.getProperty("conn_threshold"));
        }
        if (!StringUtils.isBlank(props.getProperty("ip_check_interval"))) {
            ipCountCheckInterval = Integer.parseInt(props.getProperty("ip_check_interval"));
        }
        if (!StringUtils.isBlank(props.getProperty("ip_count_threshold"))) {
            ipCountThreshold = Integer.parseInt(props.getProperty("ip_count_threshold"));
        }
        this.remotingServer.addConnectionLifeCycleListener(new ConnectionNumberListener(connectionThreshold,
            ipCountThreshold, ipCountCheckInterval));
    }


    public void stop() {
        try {
            remotingServer.stop();
        }
        catch (NotifyRemotingException e) {
            log.error("关闭remoting server出错", e);

        }
        clusterService.stop();
        log.info("Stop pushit broker successfully");
    }


    public void startup() {
        try {
            remotingServer.start();
        }
        catch (NotifyRemotingException e) {
            log.error("启动remoting server出错", e);

        }
        myUrl = remotingServer.getConnectURI().toString();
        log.info("Start pushit broker successfully");
    }

}
