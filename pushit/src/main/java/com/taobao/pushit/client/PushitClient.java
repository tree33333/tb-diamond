/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.client;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.StringUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.pushit.commons.AddrUtils;
import com.taobao.pushit.commons.ClientInterest;
import com.taobao.pushit.commons.Constants;
import com.taobao.pushit.commons.ClientLoggerInit;
import com.taobao.pushit.network.InterestCommand;
import com.taobao.pushit.network.NotifyCommand;
import com.taobao.pushit.network.PushitWireFormatType;


/**
 * Pushit客户端
 * <p>
 * 典型调用如下： <blockquote>
 * 
 * <pre>
 * PushitClient ptClient=new PushitClient("host:port",new NotifyListener(){
 *             public void onNotify(String dataId,String group,String message){
 *                     doSomething...
 *             }
 *             
 * });
 * ptClient.interest(dataId,group);
 * ptClient.push(notifyDataId,notifyGroup);
 * </pre>
 * 
 * </blockquote>
 * 
 * <p>
 * 
 * @author boyan
 * @Date 2011-5-25
 * 
 */
public class PushitClient {
    
    static {
        try {
            ClientLoggerInit.initLog();
        }
        catch(Throwable t) {
            // ignore
        }
    }

    private RemotingClient remotingClient;
    private final NotifyListener notifyListener;
    private String serverUrl;
    static final Log log = LogFactory.getLog(PushitClient.class);

    private final List<ClientInterest> clientInterests;

    /**
     * 初始化Pushit客户端,默认连接超时为30秒，仅用于push通知，无法监听通知
     * 
     * @param servers
     *            服务器列表，形如"host:port host:port..."的字符串
     * @throws IOException
     *             网络异常
     * @throws InterruptedException
     *             阻塞连接，响应中断抛出此异常
     */
    public PushitClient(String servers) throws IOException, InterruptedException {
        this(servers, (NotifyListener) null);
    }


    /**
     * 初始化Pushit客户端,默认连接超时为30秒
     * 
     * @param servers
     *            服务器列表，形如"host:port host:port..."的字符串
     * @param notifyListener
     *            通知监听器
     * @throws IOException
     *             网络异常
     * @throws InterruptedException
     *             阻塞连接，响应中断抛出此异常
     */
    public PushitClient(String servers, NotifyListener notifyListener) throws IOException, InterruptedException {
        this(servers, notifyListener, 30000L);
    }


    /**
     * 初始化Pushit客户端
     * 
     * @param servers
     *            服务器列表，形如"host:port host:port..."的字符串
     * @param notifyListener
     *            通知监听器
     * @param connectTimeoutInMills
     *            连接超时，单位毫秒
     * @throws IOException
     *             网络异常
     * @throws InterruptedException
     *             阻塞连接，响应中断抛出此异常
     */
    public PushitClient(String servers, NotifyListener notifyListener, long connectTimeoutInMills) throws IOException,
            InterruptedException {
        super();
        this.notifyListener = notifyListener;
        if (connectTimeoutInMills <= 0) {
            throw new IllegalArgumentException("connectTimeoutInMills must be great than zero");
        }
        clientInterests = new CopyOnWriteArrayList<ClientInterest>();
        initRemotingClient(connectTimeoutInMills);
        connect(servers);
    }


    private void initRemotingClient(long connectTimeoutInMills) throws IOException {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new PushitWireFormatType());
        // clientConfig.setIdleTime(-1);// 禁止心跳检测
        clientConfig.setConnectTimeout(connectTimeoutInMills);
        remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        remotingClient.registerProcessor(NotifyCommand.class, new RequestProcessor<NotifyCommand>() {

            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void handleRequest(NotifyCommand request, Connection conn) {
                if (notifyListener != null) {
                    notifyListener.onNotify(request.getDataId(), request.getGroup(), request.getMessage());
                }
            }

        });
        remotingClient.addConnectionLifeCycleListener(new ReconnectionListener());

        try {
            remotingClient.start();
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }


    private void connect(String servers) throws InterruptedException, IOException {
        if (StringUtils.isBlank(servers)) {
            throw new IllegalArgumentException("blank servers");
        }
        String[] hosts = servers.split(",");
        if (hosts.length <= 0) {
            throw new IllegalArgumentException("Empty hosts");
        }
        String host = getTargetHost(hosts);
        if (host.equals(serverUrl)) {
            return;
        }
        // 关闭旧的
        if (!StringUtils.isBlank(serverUrl)) {

            try {
                log.info("Closing " + serverUrl);
                remotingClient.close(serverUrl, false);
            }
            catch (NotifyRemotingException e) {
                throw new IOException(e);
            }
        }
        // 连接新的
        serverUrl = AddrUtils.getUrlFromHost(host);
        try {
            log.info("Connecting to " + serverUrl);
            remotingClient.connect(serverUrl);
            remotingClient.awaitReadyInterrupt(serverUrl);
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }


    /**
     * 返回当前连接的服务器的URL
     * 
     * @return
     */
    public String getServerUrl() {
        return serverUrl;
    }


    private String getTargetHost(String[] hosts) {
        Random rand = new SecureRandom();
        int targetIndex = rand.nextInt(hosts.length);
        return hosts[targetIndex];
    }


    /**
     * 注册感兴趣的默认分组的dataId，当有指定的通知送达的时候回调NotifyListener
     * 
     * @param dataId
     * @throws IOException
     */
    public void interest(String dataId) throws IOException {
        this.interest(dataId, Constants.DEFAULT_GROUP);
    }


    /**
     * 注册感兴趣的dataId和group，当有指定的通知送达的时候回调NotifyListener
     * 
     * @param dataId
     * @param group
     * @throws IOException
     */
    public void interest(String dataId, String group) throws IOException {
        List<ClientInterest> clientInterests = new ArrayList<ClientInterest>();
        clientInterests.add(new ClientInterest(dataId, group));
        this.interest(clientInterests);
    }


    /**
     * 注册感兴趣的dataId和group列表，当有指定的通知送达的时候回调NotifyListener
     * 
     * @param clientInterests
     * @throws IOException
     */
    public void interest(List<ClientInterest> clientInterests) throws IOException {
        checkParams(clientInterests);
        this.clientInterests.addAll(clientInterests);
        try {
            remotingClient.sendToGroup(serverUrl, new InterestCommand(clientInterests));
        }
        catch (NotifyRemotingException e) {
            throw new IOException("Interests failed", e);
        }

    }


    private void checkParams(List<ClientInterest> clientInterests) {
        if (notifyListener == null) {
            throw new IllegalStateException("Null notifyListener for interests");
        }
        for (ClientInterest clientInterest : clientInterests) {
            if (StringUtils.isBlank(clientInterest.dataId)) {
                throw new IllegalArgumentException("Blank dataId");
            }
            checkCharacter(clientInterest.dataId);
            if (clientInterest.group != null) {
                checkCharacter(clientInterest.group);
            }

        }
    }


    private void checkCharacter(String dataId) {
        if (dataId.contains(" ")) {
            throw new IllegalArgumentException("DataId contains blank");
        }
        if (dataId.contains(PushitWireFormatType.BLANK_REPLACE)) {
            throw new IllegalArgumentException("DataId contains invalid character");
        }
    }


    /**
     * 发布通知
     * 
     * @param dataId
     *            通知到的dataId
     * @param group
     *            通知到的分组
     * @param message
     *            附带消息内容，可无
     * @throws IOException
     */
    public void push(String dataId, String group, String message) throws IOException {
        if (StringUtils.isBlank(dataId)) {
            throw new IllegalArgumentException("Blank dataId");
        }
        checkCharacter(dataId);
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Blank group");
        }
        checkCharacter(group);
        try {
            remotingClient.sendToGroup(serverUrl, new NotifyCommand(dataId, group, message));
        }
        catch (NotifyRemotingException e) {
            throw new IOException("Push failed", e);
        }
    }


    /**
     * 发布通知到默认分组
     * 
     * @param dataId
     *            通知到的dataId
     * @param message
     *            附带消息内容，可无
     * @throws IOException
     */
    public void push(String dataId, String message) throws IOException {
        this.push(dataId, Constants.DEFAULT_GROUP, message);
    }


    public void stop() throws IOException {
        try {
            remotingClient.stop();
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }

    private class ReconnectionListener implements ConnectionLifeCycleListener {

        public void onConnectionCreated(Connection conn) {
        }


        public void onConnectionClosed(Connection conn) {
        }


        public void onConnectionReady(Connection conn) {
            if (conn.isConnected()) {
                try {
                    if (!clientInterests.isEmpty()) {
                        conn.send(new InterestCommand(clientInterests));
                        log.warn("重新连接并发送clientInterests：" + conn);
                    }
                }
                catch (Exception e) {
                    log.error("发送clientInterests失败:" + conn, e);
                }
            }
            else {
                log.error("无效重连：" + conn);
            }

        }

    }

}
