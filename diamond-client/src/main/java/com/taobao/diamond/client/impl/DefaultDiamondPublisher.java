/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.client.impl;

import static com.taobao.diamond.common.Constants.WORD_SEPARATOR;
import static com.taobao.pushit.commons.Constants.DEFAULT_GROUP;
import static com.taobao.pushit.commons.Constants.REAPER_ID;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondConfigure;
import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.jmx.DiamondClientUtil;
import com.taobao.diamond.client.processor.ServerAddressProcessor;
import com.taobao.diamond.client.task.PublishTask;
import com.taobao.diamond.client.task.UnpublishTask;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.configinfo.PublishCacheData;
import com.taobao.diamond.mockserver.MockServer;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.utils.AppNameUtils;
import com.taobao.diamond.utils.ContentUtils;
import com.taobao.diamond.utils.LoggerInit;
import com.taobao.diamond.utils.SimpleFlowData;
import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;


/**
 * Diamond发布者默认实现 该发布者不希望用户直接使用，而是作为软负载中心发布者的基础， 多个basestone
 * manager共用一个单例的DefaultDiamondPublisher，每个集群有一个单例的DefaultDiamondPublisher
 * 
 * @author leiwen
 * 
 */
class DefaultDiamondPublisher implements DiamondPublisher {

    private static final Log log = LogFactory.getLog(DefaultDiamondPublisher.class);

    private String userName = Constants.DEFAULT_USERNAME;
    private String password = Constants.DEFAULT_PASSWORD;

    private int requestTimeout;

    private HttpClient httpClient;

    private PushitClient pushitClient;

    private ServerAddressProcessor serverAddressProcessor;

    private ScheduledExecutorService scheduledExecutor;

    private ScheduledExecutorService scheduler;

    private TaskManager publishTaskManager;

    private DefaultDiamondSubscriber diamondSubscriber;

    private volatile DiamondConfigure diamondConfigure;

    private final SimpleFlowData flowData = new SimpleFlowData(10, 2000);

    private final AtomicInteger domainNamePos = new AtomicInteger(0);

    private final ConcurrentHashMap<String/* dataId+group */, PublishCacheData> cache =
            new ConcurrentHashMap<String, PublishCacheData>();

    private final CopyOnWriteArrayList<String> pushitClientCache = new CopyOnWriteArrayList<String>();

    private final long REPORT_INTERVAL = 20L;

    private volatile boolean isRun = false;

    private String appName;
    // 集群标识
    private String clusterType;

    // 缺省唯一性标识提取pattern
    private ContentIdentityPattern defaultPattern = new URLContentIdentityPattern();

    private PublishTaskProcessor publishProcessor;

    private UnpublishTaskProcessor unpublishProcessor;

    private PublishAllTaskProcessor pubAllProcessor;

    static {
        LoggerInit.initLogFromBizLog();
    }

    public static class Builder {
        private DiamondConfigure diamondConfigure;
        private String appName;
        private String type;


        public Builder(String type) {
            this.type = type;
            this.diamondConfigure = new DiamondConfigure(this.type);
        }


        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }


        public Builder setDiamondConfigure(DiamondConfigure diamondConfigure) {
            this.diamondConfigure = diamondConfigure;
            return this;
        }


        public DiamondPublisher build() {
            return new DefaultDiamondPublisher(this);
        }
    }


    private DefaultDiamondPublisher(Builder builder) {
        this.diamondConfigure = builder.diamondConfigure;
        this.appName = builder.appName;
        if (this.appName == null) {
            this.appName = getAppName();
        }
        this.clusterType = builder.type;

        publishProcessor = new PublishTaskProcessor();
        publishProcessor.setDiamondPublisher(this);

        unpublishProcessor = new UnpublishTaskProcessor();
        unpublishProcessor.setDiamondPublisher(this);

        pubAllProcessor = new PublishAllTaskProcessor();
        pubAllProcessor.setDiamondPublisher(this);
    }


    HttpClient getHttpClient() {
        return httpClient;
    }


    void setUserName(String userName) {
        this.userName = userName;
    }


    void setPassword(String password) {
        this.password = password;
    }


    void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }


    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }


    public DiamondConfigure getDiamondConfigure() {
        return this.diamondConfigure;
    }


    public void setDiamondConfigure(DiamondConfigure diamondConfigure) {
        if (!isRun) {
            this.diamondConfigure = diamondConfigure;
        }
        else {
            copyDiamondConfigure(diamondConfigure);
        }
    }


    public TaskManager getPublishTaskManager() {
        return publishTaskManager;
    }


    public PushitClient getPushitClient() {
        return pushitClient;
    }


    public void setDiamondSubscriber(DiamondSubscriber diamondSubscriber) {
        this.diamondSubscriber = (DefaultDiamondSubscriber) diamondSubscriber;
        publishProcessor.setDiamondSubscriber(this.diamondSubscriber);
        unpublishProcessor.setDiamondSubscriber(this.diamondSubscriber);
        pubAllProcessor.setDiamondSubscriber(this.diamondSubscriber);
    }


    public void addDataId(String dataId, String group, String configInfo) {
        String key = dataId + "-" + group;
        PublishCacheData pubCacheData = new PublishCacheData(dataId, group, configInfo);
        this.cache.put(key, pubCacheData);
        DiamondClientUtil.addPubDataId(clusterType, key);
        log.info("增量发布者cache中添加了dataId:" + dataId + ",group:" + group);
        this.start();
    }


    public void removeDataId(String dataId, String group) {
        String key = dataId + "-" + group;
        this.cache.remove(key);
        log.info("发布者cache中移除了dataId:" + dataId + ",group:" + group);
    }


    public synchronized void start() {
        if (isRun) {
            return;
        }

        if (null == scheduledExecutor || scheduledExecutor.isTerminated()) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        if (null == scheduler || scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        serverAddressProcessor = new ServerAddressProcessor(this.diamondConfigure, this.scheduledExecutor);
        serverAddressProcessor.setClusterType(clusterType);
        serverAddressProcessor.start();

        // 设置domainNamePos值
        randomDomainNamePos();

        initHttpClient();

        try {
            initPushitClient();
        }
        catch (Exception e) {
            log.error("发布者实时通知客户端初始化出错", e);
        }

        publishTaskManager = new TaskManager("publish task manager thread");

        // 初始化完毕
        isRun = true;

        // 轮询检查是否发布成功
        rotateCheckAndPublish();
    }


    public void close() {
        if (!isRun) {
            return;
        }
        serverAddressProcessor.stop();
        httpClient = null;
        try {
            pushitClient.stop();
        }
        catch (Exception e) {
            log.error("关闭pushit client出错", e);
        }
        isRun = false;
    }


    public void publish(String dataId, String group, String configInfo) {
        this.publish(dataId, group, configInfo, defaultPattern);
    }


    public void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern) {
        // 流控应该在这里, 瞬间创建大量任务, 内存负荷会很高
        this.flowControl();

        // 校验增量发布内容的格式
        ContentUtils.verifyIncrementPubContent(configInfo);

        // 校验dataId和group是否为空
        this.validate(dataId, group, configInfo);

        // 获取内容的唯一标识
        String identify = pattern.getContentIdentity(configInfo);
        // 拼接出要发往server的内容
        String content = identify + WORD_SEPARATOR + configInfo;

        this.addDataId(dataId, group, content);

        String taskType = dataId + "-" + group + "-pub";
        PublishTask publishTask = new PublishTask(dataId, group, content);

        this.publishTaskManager.addProcessor(taskType, publishProcessor);
        this.publishTaskManager.addTask(taskType, publishTask, true);

        try {
            if (this.pushitClientCache.addIfAbsent(dataId + Constants.WORD_SEPARATOR + group)) {
                this.pushitClient.interest(dataId, group);
            }
        }
        catch (Exception e) {
            log.error("注册实时通知出错: dataId=" + dataId + ", group=" + group, e);
        }

    }


    public boolean syncPublish(String dataId, String group, String configInfo, long timeout) {
        return this.syncPublish(dataId, group, configInfo, timeout, defaultPattern);
    }


    public boolean syncPublish(String dataId, String group, String configInfo, long timeout,
            ContentIdentityPattern pattern) {
        this.flowControl();

        // 校验增量发布内容的格式
        ContentUtils.verifyIncrementPubContent(configInfo);

        // 校验dataId和group是否为空
        this.validate(dataId, group, configInfo);

        // 获取内容的唯一标识
        String identify = pattern.getContentIdentity(configInfo);
        // 拼接出要发往server的内容
        String content = identify + WORD_SEPARATOR + configInfo;

        // 去掉cache中对应的项, 发布后不再进行是否成功的检查
        this.removeDataId(dataId, group);

        // 同步发布
        return this.publishProcessor.syncProcess(dataId, group, content, timeout);
    }


    public void unpublish(String dataId, String group, String configInfo) {
        this.flowControl();

        this.validate(dataId, group, configInfo);

        this.removeDataId(dataId, group);

        String taskType = dataId + "-" + group + "-unpub";
        UnpublishTask task = new UnpublishTask(dataId, group, configInfo);

        this.publishTaskManager.addProcessor(taskType, unpublishProcessor);
        this.publishTaskManager.addTask(taskType, task, true);
    }


    public boolean syncUnpublish(String dataId, String group, String configInfo, long timeout) {
        this.flowControl();

        this.validate(dataId, group, configInfo);

        this.removeDataId(dataId, group);

        return this.unpublishProcessor.syncProcess(dataId, group, configInfo, timeout);
    }


    public void publishAll(String dataId, String group, String configInfo) {
        this.flowControl();

        this.validate(dataId, group, configInfo);

        String taskType = dataId + "-" + group + "-all";
        PublishTask publishTask = new PublishTask(dataId, group, configInfo);
        this.publishTaskManager.addProcessor(taskType, this.pubAllProcessor);
        this.publishTaskManager.addTask(taskType, publishTask, true);

        // 全量数据不能重推, 可能出现相互覆盖的情况
        /*
         * try { if (this.pushitClientCache.addIfAbsent(dataId +
         * Constants.WORD_SEPARATOR + group)) {
         * this.pushitClient.interest(dataId, group); } } catch (Exception e) {
         * log.error("注册实时通知出错: dataId=" + dataId + ", group=" + group, e); }
         */
    }
    
    
    public boolean syncPublishAll(String dataId, String group, String configInfo, long timeout) {
        this.flowControl();
        
        this.validate(dataId, group, configInfo);
        
        this.removeDataId(dataId, group);
        
        return this.pubAllProcessor.syncProcess(dataId, group, configInfo, timeout);
    }


    public void scheduledReport() {
        final String ip = this.getHostAddress();

        this.scheduler.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                try {
                    StringBuilder msg = new StringBuilder();
                    msg.append("R").append(ip).append("-");

                    for (PublishCacheData pubCache : cache.values()) {
                        String dataId = pubCache.getDataId();
                        String group = pubCache.getGroup();
                        msg.append(dataId).append(WORD_SEPARATOR).append(group);
                        msg.append(",");
                    }

                    pushitClient.push(REAPER_ID, DEFAULT_GROUP, msg.toString());
                }
                catch (Exception e) {
                    log.error("发送report message失败", e);
                }

            }

        }, 0L, REPORT_INTERVAL, TimeUnit.SECONDS);
    }


    void publishNew(String dataId, String group, String configInfo) throws Exception {

        // 登录
        login();

        // 发布全新的数据
        processPublish(dataId, group, configInfo);
    }


    void publishUpdate(String dataId, String group, String configInfo) throws Exception {

        // 登录
        login();

        // 发布更新后的数据
        processUpdate(dataId, group, configInfo);
    }


    void publishRemove(String dataId, String group, String configInfo) throws Exception {

        // 登录
        login();

        // 删除数据
        processRemove(dataId, group, configInfo);
    }


    void updateAll(String dataId, String group, String configInfo) throws Exception {

        // 登录
        login();

        // 全量更新
        processUpdateAll(dataId, group, configInfo);
    }


    public void awaitPublishFinish() throws InterruptedException {
        publishTaskManager.await();

    }


    private void rotateCheckAndPublish() {
        scheduledExecutor.scheduleAtFixedRate(new Runnable() {

            public void run() {
                for (PublishCacheData cacheData : cache.values()) {
                    checkAndPublish(cacheData);
                }
            }

        }, 10, 15, TimeUnit.MINUTES);
    }


    private void checkAndPublish(PublishCacheData cacheData) {
        String dataId = cacheData.getDataId();
        String group = cacheData.getGroup();
        String configInfo = cacheData.getConfigInfo();

        String taskType = dataId + "-" + group + "-pub";
        PublishTask publishTask = new PublishTask(dataId, group, configInfo);

        this.publishTaskManager.addProcessor(taskType, publishProcessor);
        this.publishTaskManager.addTask(taskType, publishTask, true);
    }


    private String getPublishUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=postConfig");
        return uriBuilder.toString();
    }


    private String getUpdateUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=updateConfig");
        return uriBuilder.toString();
    }


    private String getRemoveUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=deleteConfig");
        return uriBuilder.toString();
    }


    private String getUpdateAllUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=updateAll");
        return uriBuilder.toString();
    }


    private String getLoginUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.HTTP_URI_LOGIN);
        uriBuilder.append("?method=login");
        return uriBuilder.toString();
    }


    private void randomDomainNamePos() {
        Random rand = new Random();
        List<String> domainList = this.diamondConfigure.getDomainNameList();
        if (!domainList.isEmpty()) {
            this.domainNamePos.set(rand.nextInt(domainList.size()));
        }
    }


    private void initHttpClient() {
        if (MockServer.isTestMode()) {
            return;
        }
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.closeIdleConnections(60 * 1000);

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setStaleCheckingEnabled(diamondConfigure.isConnectionStaleCheckingEnabled());
        params.setMaxConnectionsPerHost(hostConfiguration, diamondConfigure.getMaxHostConnections());
        params.setMaxTotalConnections(diamondConfigure.getMaxTotalConnections());
        params.setConnectionTimeout(diamondConfigure.getConnectionTimeout());
        params.setSoTimeout(requestTimeout);

        connectionManager.setParams(params);

        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfiguration);
    }


    private void initPushitClient() throws Exception {
        String servers = getPushitServers();
        pushitClient = new PushitClient(servers, new NotifyListener() {

            public void onNotify(String dataId, String group, String message) {
                if ("publish".equals(message)) {
                    // 收到通知，主动发布数据
                    log.info("收到通知，开始主动发布数据: dataId=" + dataId + ", group=" + group);
                    innerPublish(dataId, group);
                }
                else if ("remove".equals(message)) {
                    log.info("收到remove通知，开始主动发布数据: dataId=" + dataId + ", group=" + group);
                    innerRemove(dataId, group);
                }
            }

        });
    }


    private void innerPublish(String dataId, String group) {
        PublishCacheData cacheData = cache.get(dataId + "-" + group);
        if (cacheData != null) {
            checkAndPublish(cacheData);
        }
    }


    private void innerRemove(String dataId, String group) {
        PublishCacheData cacheData = cache.get(dataId + "-" + group);
        if (cache != null) {
            this.unpublish(dataId, group, cacheData.getConfigInfo());
        }
    }


    private String getPushitServers() {
        StringBuilder result = new StringBuilder();
        List<String> serverList = this.diamondConfigure.getPushitDomainNameList();
        for (String server : serverList) {
            result.append(server).append(",");
        }
        result.deleteCharAt(result.length() - 1);

        return result.toString();
    }


    private void login() throws Exception {
        PostMethod loginMethod = new PostMethod(getLoginUri());

        NameValuePair username_value = new NameValuePair("username", userName);
        NameValuePair password_value = new NameValuePair("password", password);

        loginMethod.addRequestHeader(Constants.APPNAME, this.appName);
        loginMethod.setRequestBody(new NameValuePair[] { username_value, password_value });

        this.configureHttpMethod(loginMethod);

        log.info("server list:" + this.diamondConfigure.getDomainNameList());
        log.info("index=" + this.domainNamePos.get());
        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(loginMethod);
            String responseMsg = loginMethod.getResponseBodyAsString();
            if (statusCode == HttpStatus.SC_OK) {
                log.info("登录成功");
            }
            else {
                log.error("登录失败，原因为：" + responseMsg + ", 错误码为：" + statusCode);
                throw new Exception("登录失败, 原因请查看客户端日志");
            }

        }
        finally {
            loginMethod.releaseConnection();
        }
    }


    private void processPublish(String dataId, String group, String content) throws Exception {
        PostMethod publishMethod = new PostMethod(getPublishUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        publishMethod.addRequestHeader(Constants.APPNAME, this.appName);
        publishMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(publishMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(publishMethod);
            String responseMsg = publishMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("数据发布成功");
            }
            else {
                log.error("数据发布失败, 原因为：" + responseMsg + ", 错误码为:" + statusCode);
                throw new Exception("数据发布失败, 原因请查看客户端日志");
            }
        }
        finally {
            publishMethod.releaseConnection();
        }
    }


    private void processUpdate(String dataId, String group, String content) throws Exception {
        PostMethod updateMethod = new PostMethod(getUpdateUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        updateMethod.addRequestHeader(Constants.APPNAME, this.appName);
        updateMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(updateMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(updateMethod);
            String responseMsg = updateMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("数据更新成功");
            }
            else {
                log.error("数据更新失败, 原因为：" + responseMsg + ", 错误码为:" + statusCode);
                throw new Exception("数据更新失败，原因请查看客户端日志");
            }
        }
        finally {
            updateMethod.releaseConnection();
        }
    }


    private void processRemove(String dataId, String group, String content) throws Exception {
        PostMethod removeMethod = new PostMethod(getRemoveUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        removeMethod.addRequestHeader(Constants.APPNAME, this.appName);
        removeMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(removeMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(removeMethod);
            String responseMsg = removeMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("数据删除成功");
            }
            else {
                log.error("数据删除失败, 原因为：" + responseMsg + ", 错误码为:" + statusCode);
                throw new Exception("数据删除失败，原因请查看客户端日志");
            }
        }
        finally {
            removeMethod.releaseConnection();
        }
    }


    private void processUpdateAll(String dataId, String group, String content) throws Exception {
        PostMethod updateMethod = new PostMethod(getUpdateAllUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        updateMethod.addRequestHeader(Constants.APPNAME, this.appName);
        updateMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(updateMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(updateMethod);
            String responseMsg = updateMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("数据全量更新成功");
            }
            else {
                log.error("数据全量更新失败, 原因为：" + responseMsg + ", 错误码为:" + statusCode);
                throw new Exception("数据全量更新失败，原因请查看客户端日志");
            }
        }
        finally {
            updateMethod.releaseConnection();
        }
    }


    private void configureHttpMethod(HttpMethod method) {
        // 设置socket超时
        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout(Constants.ONCE_TIMEOUT);
        method.setParams(params);
    }


    private void validate(String dataId, String group, String content) {
        if (StringUtils.isBlank(dataId) || StringUtils.isBlank(group) || StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("dataId, group, content都不能为空");
        }
    }


    synchronized void rotateToNextDomain() {
        int domainNameCount = diamondConfigure.getDomainNameList().size();
        int index = domainNamePos.incrementAndGet();
        if (index < 0) {
            index = -index;
        }
        domainNamePos.set(index % domainNameCount);
        if (diamondConfigure.getDomainNameList().size() > 0)
            log.warn("发布/更新数据时，轮换server域名到：" + diamondConfigure.getDomainNameList().get(domainNamePos.get()));
    }


    private void flowControl() {
        if (diamondConfigure.isUseFlowControl()) {
            flowData.addAndGet(1);

            while (flowData.getAverageCount() > diamondConfigure.getFlowControlThreshold()) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    private static String getAppName() {
        String appName = null;
        try {
            appName = AppNameUtils.getAppName();
        }
        catch (Throwable t) {
            log.warn("Can not getAppName. AppName = null");
            appName = null;
        }
        if (appName == null) {
            appName = "null";
        }
        else {
            appName = appName.trim();
        }
        return appName;
    }


    private void copyDiamondConfigure(DiamondConfigure diamondConfigure) {
        // TODO 哪些值可以运行时动态更新?
    }


    private String getHostAddress() {
        String address = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> ads = ni.getInetAddresses();
                while (ads.hasMoreElements()) {
                    InetAddress ip = ads.nextElement();
                    if (!ip.isLoopbackAddress() && ip.isSiteLocalAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("获取主机地址出错", e);
        }
        return address;
    }

}
