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

import static com.taobao.diamond.common.Constants.LINE_SEPARATOR;
import static com.taobao.diamond.common.Constants.WORD_SEPARATOR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.client.DiamondConfigure;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.SubscriberListener;
import com.taobao.diamond.client.jmx.DiamondClientUtil;
import com.taobao.diamond.client.processor.LocalConfigInfoProcessor;
import com.taobao.diamond.client.processor.ServerAddressProcessor;
import com.taobao.diamond.client.processor.SnapshotConfigInfoProcessor;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.configinfo.CacheData;
import com.taobao.diamond.configinfo.ConfigureInfomation;
import com.taobao.diamond.md5.MD5;
import com.taobao.diamond.mockserver.MockServer;
import com.taobao.diamond.utils.AppNameUtils;
import com.taobao.diamond.utils.DiamondStatLog;
import com.taobao.diamond.utils.LoggerInit;
import com.taobao.diamond.utils.SimpleCache;
import com.taobao.diamond.utils.StatConstants;
import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;


/**
 * ȱʡ��DiamondSubscriber
 * 
 * @author aoqiong
 * 
 */
class DefaultDiamondSubscriber implements DiamondSubscriber {
    // �����ļ�����Ŀ¼
    private static final String DATA_DIR = "data";
    // ��һ����ȷ���õľ���Ŀ¼
    private static final String SNAPSHOT_DIR = "snapshot";

    private static final Log log = LogFactory.getLog(DefaultDiamondSubscriber.class);

    private static final int SC_OK = 200;

    private static final int SC_NOT_MODIFIED = 304;

    private static final int SC_NOT_FOUND = 404;

    private static final int SC_SERVICE_UNAVAILABLE = 503;

    static {
        try {
            LoggerInit.initLogFromBizLog();
        }
        catch (Throwable _) {
        }
    }
    private final Log dataLog = LogFactory.getLog(LoggerInit.LOG_NAME_CONFIG_DATA);

    private final ConcurrentHashMap<String/* DataID */, ConcurrentHashMap<String/* Group */, CacheData>> cache =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, CacheData>>();

    private volatile SubscriberListener subscriberListener = null;
    private volatile DiamondConfigure diamondConfigure;

    private ScheduledExecutorService scheduledExecutor = null;

    private final LocalConfigInfoProcessor localConfigInfoProcessor = new LocalConfigInfoProcessor();

    private SnapshotConfigInfoProcessor snapshotConfigInfoProcessor;

    private final SimpleCache<String> contentCache = new SimpleCache<String>();

    private ServerAddressProcessor serverAddressProcessor = null;

    private final AtomicInteger domainNamePos = new AtomicInteger(0);

    private volatile boolean isRun = false;

    private HttpClient httpClient = null;

    private PushitClient pushitClient = null;

    // private SimpleFlowData flowData = null;

    private String appName = null;

    private volatile boolean bFirstCheck = true;

    // ��Ⱥ��ʶ
    private String clusterType;

    public static class Builder {
        private final Map<String/* dataId */, Set<String>/* group */> dataIdGroupPairs =
                new HashMap<String, Set<String>>();
        private final SubscriberListener subscriberListener;
        private DiamondConfigure diamondConfigure;
        private String appName;
        private String type;


        public Builder(SubscriberListener subscriberListener) {
            this(subscriberListener, Constants.DEFAULT_DIAMOND_CLUSTER);
        }


        public Builder(SubscriberListener subscriberListener, String type) {
            this.subscriberListener = subscriberListener;
            this.type = type;
            this.diamondConfigure = new DiamondConfigure(type);
        }


        public Builder addDataId(String dataId, String group) {
            if (null == group) {
                group = Constants.DEFAULT_GROUP;
            }
            synchronized (this.dataIdGroupPairs) {
                Set<String> groups = this.dataIdGroupPairs.get(dataId);
                if (null == groups) {
                    Set<String> groupSet = new HashSet<String>();
                    groupSet.add(group);
                    this.dataIdGroupPairs.put(dataId, groupSet);
                }
            }
            return this;
        }


        public Builder addDataId(String dataId) {
            return addDataId(dataId, null);
        }


        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }


        public Builder setDiamondConfigure(DiamondConfigure diamondConfigure) {
            this.diamondConfigure = diamondConfigure;
            return this;
        }


        public DiamondSubscriber build() {
            return new DefaultDiamondSubscriber(this);
        }
    }


    private DefaultDiamondSubscriber(Builder builder) {
        this.subscriberListener = builder.subscriberListener;
        this.diamondConfigure = builder.diamondConfigure;
        this.appName = builder.appName;
        if (this.appName == null) {
            this.appName = getAppName();
        }
        this.clusterType = builder.type;
        if (null != builder.dataIdGroupPairs) {
            for (Map.Entry<String, Set<String>> dataIdGroupsPair : builder.dataIdGroupPairs.entrySet()) {
                for (String group : dataIdGroupsPair.getValue()) {
                    this.addDataId(dataIdGroupsPair.getKey(), group);
                }
            }
        }
    }


    public DefaultDiamondSubscriber(SubscriberListener subscriberListener) {
        this(subscriberListener, Constants.DEFAULT_DIAMOND_CLUSTER);
    }


    public DefaultDiamondSubscriber(SubscriberListener subscriberListener, String clusterType) {
        this.subscriberListener = subscriberListener;
        this.appName = getAppName();
        this.clusterType = clusterType;
        this.diamondConfigure = new DiamondConfigure(clusterType);
    }


    private void recordDataIdChange(String dataId, String group) {
        DiamondClientUtil.addPopCount(clusterType, dataId, group);
    }


    /**
     * ����DiamondSubscriber��<br>
     * 1.����������ȡ���е�DataId������Ϣ<br>
     * 2.������ʱ�̶߳�ʱ��ȡ���е�DataId������Ϣ<br>
     */
    public synchronized void start() {
        if (isRun) {
            return;
        }

        if (null == scheduledExecutor || scheduledExecutor.isTerminated()) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        localConfigInfoProcessor.start(this.diamondConfigure.getFilePath() + "/" + DATA_DIR);
        serverAddressProcessor = new ServerAddressProcessor(this.diamondConfigure, this.scheduledExecutor);
        serverAddressProcessor.setClusterType(clusterType);
        serverAddressProcessor.start();

        this.snapshotConfigInfoProcessor =
                new SnapshotConfigInfoProcessor(this.diamondConfigure.getFilePath() + "/" + SNAPSHOT_DIR);
        // ����domainNamePosֵ
        randomDomainNamePos();
        // TODO ����DiamondSubscriber
        initHttpClient();
        // ��ʼ��pushit client
        try {
            initPushitClient();
        }
        catch (Exception e) {
            log.error("��ʼ��pushit client����", e);
        }

        // ��ʼ�����
        isRun = true;

        if (log.isInfoEnabled()) {
            log.info("��ǰʹ�õ������У�" + this.diamondConfigure.getDomainNameList());
        }

        if (MockServer.isTestMode()) {
            bFirstCheck = false;
        }
        else {
            // ������ѯ���ʱ��
            this.diamondConfigure.setPollingIntervalTime(Constants.POLLING_INTERVAL_TIME);
        }
        // ��ѯ
        rotateCheckConfigInfo();
        
        addShutdownHook();
    }


    private void randomDomainNamePos() {
        // �������ʼ��������ַ
        Random rand = new Random();
        List<String> domainList = this.diamondConfigure.getDomainNameList();
        if (!domainList.isEmpty()) {
            this.domainNamePos.set(rand.nextInt(domainList.size()));
        }
    }

    
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                // �رյ���������
                close();
            }
            
        });
    }
    

    protected void initHttpClient() {
        if (MockServer.isTestMode()) {
            return;
        }
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(diamondConfigure.getDomainNameList().get(this.domainNamePos.get()),
            diamondConfigure.getPort());

        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.closeIdleConnections(diamondConfigure.getPollingIntervalTime() * 4000);

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setStaleCheckingEnabled(diamondConfigure.isConnectionStaleCheckingEnabled());
        params.setMaxConnectionsPerHost(hostConfiguration, diamondConfigure.getMaxHostConnections());
        params.setMaxTotalConnections(diamondConfigure.getMaxTotalConnections());
        params.setConnectionTimeout(diamondConfigure.getConnectionTimeout());
        // ���ö���ʱΪ1����,
        // boyan@taobao.com
        params.setSoTimeout(60 * 1000);

        connectionManager.setParams(params);
        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfiguration);
    }


    private void initPushitClient() throws Exception {
        StringBuilder pushitServers = new StringBuilder();
        List<String> pushitServerList = this.getDiamondConfigure().getPushitDomainNameList();
        for (String pushitServer : pushitServerList) {
            pushitServers.append(pushitServer).append(",");
        }
        pushitServers.deleteCharAt(pushitServers.length() - 1);

        pushitClient = new PushitClient(pushitServers.toString(), new NotifyListener() {

            public void onNotify(String dataId, String group, String message) {
                if ("subscribe".equals(message)) {
                    log.info("�յ�pushit server��֪ͨ: dataId=" + dataId + ", group=" + group);
                    getConfigInfoAndPopToClient(dataId, group);
                }
            }

        });
    }


    /**
     * �������ԣ��������
     * 
     * @param pos
     */
    void setDomainNamesPos(int pos) {
        this.domainNamePos.set(pos);
    }


    /**
     * ѭ��̽��������Ϣ�Ƿ�仯������仯�����ٴ���DiamondServer�����ȡ��Ӧ��������Ϣ
     */
    private void rotateCheckConfigInfo() {
        scheduledExecutor.schedule(new Runnable() {
            public void run() {
                if (!isRun) {
                    log.warn("DiamondSubscriber��������״̬�У��˳���ѯѭ��");
                    return;
                }
                try {
                    checkLocalConfigInfo();
                    checkDiamondServerConfigInfo();
                    checkSnapshot();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    log.error("ѭ��̽�ⷢ���쳣", e);
                }
                finally {
                    rotateCheckConfigInfo();
                }
            }

        }, bFirstCheck ? 60 : diamondConfigure.getPollingIntervalTime(), TimeUnit.SECONDS);
        bFirstCheck = false;
    }


    void getConfigInfoAndPopToClient(String dataId, String group) {
        ConcurrentHashMap<String, CacheData> cacheDatas = cache.get(dataId);
        if (null == cacheDatas) {
            log.info("[pushit client] cacheDatas == null");
            return;
        }
        CacheData cacheData = cacheDatas.get(group);
        if (null == cacheData) {
            log.info("[pushit client] cacheData == null");
            return;
        }
        log.info("[pushit client] diamond-client��ʼ������diamond-server��������");
        receiveConfigInfo(cacheData);
    }


    /**
     * ��DiamondServer����dataId��Ӧ��������Ϣ����������׸��ͻ��ļ�����
     * 
     * @param dataId
     */
    private void receiveConfigInfo(final CacheData cacheData) {
        scheduledExecutor.execute(new Runnable() {
            public void run() {
                if (!isRun) {
                    log.warn("DiamondSubscriber��������״̬�У��˳���ѯѭ��");
                    return;
                }

                try {
                    String configInfo =
                            getConfigureInfomation(cacheData.getDataId(), cacheData.getGroup(),
                                diamondConfigure.getReceiveWaitTime(), true);
                    if (null == configInfo) {
                        return;
                    }

                    if (null == subscriberListener) {
                        log.warn("null == subscriberListener");
                        return;
                    }

                    popConfigInfo(cacheData, configInfo, RotateType.SERVER);
                }
                catch (Exception e) {
                    log.error("��Diamond��������Ҫ������Ϣ�Ĺ������쳣", e);
                }
            }
        });
    }


    private void checkSnapshot() {
        for (Entry<String, ConcurrentHashMap<String, CacheData>> cacheDatasEntry : cache.entrySet()) {
            ConcurrentHashMap<String, CacheData> cacheDatas = cacheDatasEntry.getValue();
            if (null == cacheDatas) {
                continue;
            }
            for (Entry<String, CacheData> cacheDataEntry : cacheDatas.entrySet()) {
                final CacheData cacheData = cacheDataEntry.getValue();
                // û�л�ȡ�������ã�Ҳû�д�diamond server��ȡ���óɹ�,�������һ�ε�snapshot
                if (!cacheData.isUseLocalConfigInfo() && cacheData.getFetchCount() == 0) {
                    String configInfo = getSnapshotConfiginfomation(cacheData.getDataId(), cacheData.getGroup());
                    if (configInfo != null) {
                        popConfigInfo(cacheData, configInfo, RotateType.SNAPSHOT);
                    }
                }
            }
        }
    }


    private void checkDiamondServerConfigInfo() {
        Set<String> updateDataIdGroupPairs = checkUpdateDataIds(diamondConfigure.getReceiveWaitTime());
        if (null == updateDataIdGroupPairs || updateDataIdGroupPairs.size() == 0) {
            log.debug("û�б��޸ĵ�DataID");
            return;
        }
        // ����ÿ�������仯��DataID��������һ�ζ�Ӧ��������Ϣ
        for (String freshDataIdGroupPair : updateDataIdGroupPairs) {
            int middleIndex = freshDataIdGroupPair.indexOf(WORD_SEPARATOR);
            if (middleIndex == -1)
                continue;
            String freshDataId = freshDataIdGroupPair.substring(0, middleIndex);
            String freshGroup = freshDataIdGroupPair.substring(middleIndex + 1);

            ConcurrentHashMap<String, CacheData> cacheDatas = cache.get(freshDataId);
            if (null == cacheDatas) {
                continue;
            }
            CacheData cacheData = cacheDatas.get(freshGroup);
            if (null == cacheData) {
                continue;
            }
            receiveConfigInfo(cacheData);
        }
    }


    private void checkLocalConfigInfo() {
        for (Entry<String/* dataId */, ConcurrentHashMap<String/* group */, CacheData>> cacheDatasEntry : cache
            .entrySet()) {
            ConcurrentHashMap<String, CacheData> cacheDatas = cacheDatasEntry.getValue();
            if (null == cacheDatas) {
                continue;
            }
            for (Entry<String, CacheData> cacheDataEntry : cacheDatas.entrySet()) {
                final CacheData cacheData = cacheDataEntry.getValue();
                try {
                    String configInfo = getLocalConfigureInfomation(cacheData);
                    if (null != configInfo) {
                        if (log.isInfoEnabled()) {
                            log.info("����������Ϣ����ȡ, dataId:" + cacheData.getDataId() + ", group:" + cacheData.getGroup());
                        }
                        popConfigInfo(cacheData, configInfo, RotateType.LOCAL);
                        continue;
                    }
                    if (cacheData.isUseLocalConfigInfo()) {
                        continue;
                    }
                }
                catch (Exception e) {
                    log.error("�򱾵���Ҫ������Ϣ�Ĺ������쳣", e);
                }
            }
        }
    }


    /**
     * ��������Ϣ�׸��ͻ��ļ�����
     * 
     */
    void popConfigInfo(final CacheData cacheData, final String configInfo, final RotateType rotateType) {
        final ConfigureInfomation configureInfomation = new ConfigureInfomation();
        configureInfomation.setConfigureInfomation(configInfo);
        final String dataId = cacheData.getDataId();
        final String group = cacheData.getGroup();
        configureInfomation.setDataId(dataId);
        configureInfomation.setGroup(group);
        cacheData.incrementFetchCountAndGet();
        if (null != this.subscriberListener.getExecutor()) {
            this.subscriberListener.getExecutor().execute(new Runnable() {
                public void run() {
                    try {
                        if(subscriberListener instanceof DefaultSubscriberListener) {
                            ((DefaultSubscriberListener)subscriberListener).setRotateType(rotateType);
                        }
                        subscriberListener.receiveConfigInfo(configureInfomation);
                        saveSnapshot(dataId, group, configInfo);
                    }
                    catch (Throwable t) {
                        log.error("������Ϣ�����������쳣��groupΪ��" + group + ", dataIdΪ��" + dataId, t);
                    }
                }
            });
        }
        else {
            try {
                if(subscriberListener instanceof DefaultSubscriberListener) {
                    ((DefaultSubscriberListener)subscriberListener).setRotateType(rotateType);
                }
                subscriberListener.receiveConfigInfo(configureInfomation);
                saveSnapshot(dataId, group, configInfo);
            }
            catch (Throwable t) {
                log.error("������Ϣ�����������쳣��groupΪ��" + group + ", dataIdΪ��" + dataId, t);
            }
        }
    }


    public synchronized void close() {
        if (!isRun) {
            return;
        }
        log.warn("��ʼ�ر�DiamondSubscriber");

        try {
            pushitClient.stop();
        }
        catch (Exception e) {
            log.error("�ر�pushit client����", e);
        }
        localConfigInfoProcessor.stop();
        serverAddressProcessor.stop();
        serverAddressProcessor = null;
        isRun = false;
        scheduledExecutor.shutdownNow();
        scheduledExecutor = null;
        cache.clear();
        DiamondClientUtil.close();
        log.warn("�ر�DiamondSubscriber���");
    }


    /**
     * 
     * @param waitTime
     *            ���β�ѯ�Ѿ��ķѵ�ʱ��(�Ѿ���ѯ�Ķ��HTTP�ķѵ�ʱ��)
     * @param timeout
     *            ���β�ѯ�ܵĿɺķ�ʱ��(�ɹ����HTTP��ѯʹ��)
     * @return ����HTTP��ѯ�ܹ�ʹ�õ�ʱ��
     */
    long getOnceTimeOut(long waitTime, long timeout) {
        long onceTimeOut = this.diamondConfigure.getOnceTimeout();
        long remainTime = timeout - waitTime;
        if (onceTimeOut > remainTime) {
            onceTimeOut = remainTime;
        }
        return onceTimeOut;
    }


    public String getLocalConfigureInfomation(CacheData cacheData) throws IOException {
        if (!isRun) {
            throw new RuntimeException("DiamondSubscriber��������״̬�У��޷���ȡ����ConfigureInfomation");
        }
        return localConfigInfoProcessor.getLocalConfigureInfomation(cacheData, false);
    }


    public String getConfigureInfomation(String dataId, long timeout) {
        return getConfigureInfomation(dataId, null, timeout);
    }


    public String getConfigureInfomation(String dataId, String group, long timeout) {
        // ͬ���ӿ�����
        // flowControl();
        if (null == group) {
            group = Constants.DEFAULT_GROUP;
        }
        CacheData cacheData = getCacheData(dataId, group);
        // ����ʹ�ñ�������
        try {
            String localConfig = localConfigInfoProcessor.getLocalConfigureInfomation(cacheData, true);
            if (localConfig != null) {
                cacheData.incrementFetchCountAndGet();
                saveSnapshot(dataId, group, localConfig);
                return localConfig;
            }
        }
        catch (IOException e) {
            log.error("��ȡ���������ļ�����", e);
        }
        // ��ȡ��������ʧ�ܣ�������ȡ
        String result = getConfigureInfomation(dataId, group, timeout, false);
        if (result != null) {
            saveSnapshot(dataId, group, result);
            cacheData.incrementFetchCountAndGet();
        }
        return result;
    }


    public boolean exists(String dataId, String group) {
        try {
            String result = this.getConfigureInfomation(dataId, group, 10000, false);
            if (result == null) {
                return false;
            }
            return true;
        }
        catch (Exception e) {
            log.error("�ж������Ƿ����ʱ����, dataId=" + dataId + ", group=" + group, e);
            return false;
        }
    }


    // private void flowControl() {
    // if (this.diamondConfigure.isUseFlowControl()) {
    // if (this.flowData == null) {
    // this.flowData = new SimpleFlowData(20, 3000);
    // }
    // flowData.addAndGet(1);
    // if (flowData.getAverageCount() >
    // this.diamondConfigure.getFlowControlThreshold()) {
    // throw new RuntimeException("����ͬ���ӿڳ�������");
    // }
    // }
    // }

    private void saveSnapshot(String dataId, String group, String config) {
        if (config != null) {
            try {
                this.snapshotConfigInfoProcessor.saveSnaptshot(dataId, group, config);
            }
            catch (IOException e) {
                log.error("����snapshot����,dataId=" + dataId + ",group=" + group, e);
            }
        }
    }


    public String getAvailableConfigureInfomation(String dataId, String group, long timeout) {
        // �����ȴӱ��غ������ȡ������Ϣ
        try {
            String result = getConfigureInfomation(dataId, group, timeout);
            if (result != null && result.length() > 0) {
                return result;
            }
        }
        catch (Throwable t) {
            // ignore
        }
        // ����ģʽ��ʹ�ñ���dump
        if (MockServer.isTestMode()) {
            return null;
        }
        return getSnapshotConfiginfomation(dataId, group);
    }


    public String getAvailableConfigureInfomationFromSnapshot(String dataId, String group, long timeout) {
        String result = getSnapshotConfiginfomation(dataId, group);
        if (!StringUtils.isBlank(result)) {
            return result;
        }
        return getConfigureInfomation(dataId, group, timeout);
    }


    private String getSnapshotConfiginfomation(String dataId, String group) {
        if (group == null) {
            group = Constants.DEFAULT_GROUP;
        }
        try {
            CacheData cacheData = getCacheData(dataId, group);
            String config = this.snapshotConfigInfoProcessor.getConfigInfomation(dataId, group);
            if (config != null && cacheData != null) {
                cacheData.incrementFetchCountAndGet();
            }
            return config;
        }
        catch (Exception e) {
            log.error("��ȡsnapshot������ dataId=" + dataId + ",group=" + group, e);
            return null;
        }
    }


    /**
     * 
     * @param dataId
     * @param group
     * @param timeout
     * @param skipContentCache
     *            �Ƿ�ʹ�ñ��ص�����cache������getʱ��ʹ�ã���check�������첽get��ʹ�ñ���cache��
     * @return
     */
    String getConfigureInfomation(String dataId, String group, long timeout, boolean skipContentCache) {
        start();
        if (!isRun) {
            throw new RuntimeException("DiamondSubscriber��������״̬�У��޷���ȡConfigureInfomation");
        }
        if (null == group) {
            group = Constants.DEFAULT_GROUP;
        }
        // =======================ʹ�ò���ģʽ=======================
        if (MockServer.isTestMode()) {
            return MockServer.getConfigInfo(dataId, group);
        }
        // ==========================================================
        /**
         * ʹ�ô���TTL��cache��
         */
        if (!skipContentCache) {
            String key = makeCacheKey(dataId, group);
            String content = contentCache.get(key);
            if (content != null) {
                return content;
            }
        }

        long start = System.currentTimeMillis();

        long waitTime = 0;

        String uri = getUriString(dataId, group);
        if (log.isInfoEnabled()) {
            log.info(uri);
        }

        CacheData cacheData = getCacheData(dataId, group);

        // �ܵ����Դ���
        int retryTimes = this.getDiamondConfigure().getRetrieveDataRetryTimes();
        log.info("�趨�Ļ�ȡ�������ݵ����Դ���Ϊ��" + retryTimes);
        // �Ѿ����Թ��Ĵ���
        int tryCount = 0;

        while (0 == timeout || timeout > waitTime) {
            // ���Դ�����1
            tryCount++;
            if (tryCount > retryTimes + 1) {
                log.warn("�Ѿ��������趨�����Դ���");
                break;
            }
            log.info("��ȡ�������ݣ���" + tryCount + "�γ���, waitTime:" + waitTime);

            // ���ó�ʱʱ��
            long onceTimeOut = getOnceTimeOut(waitTime, timeout);
            waitTime += onceTimeOut;

            HttpMethod httpMethod = new GetMethod(uri);

            configureHttpMethod(skipContentCache, cacheData, onceTimeOut, httpMethod);

            boolean stated = false;
            try {

                int httpStatus = httpClient.executeMethod(httpMethod);
                String response = httpMethod.getResponseBodyAsString();

                switch (httpStatus) {

                case SC_OK: {
                    String result = getSuccess(dataId, group, cacheData, httpMethod);
                    stated = true;
                    DiamondStatLog.addStatValue2(StatConstants.APP_NAME, StatConstants.STAT_AVERAGE_HTTP_GET_OK,
                        dataId, group, System.currentTimeMillis() - start);
                    // ͳ��dataid�ı仯
                    recordDataIdChange(dataId, group);
                    return result;
                }

                case SC_NOT_MODIFIED: {
                    String result = getNotModified(dataId, cacheData, httpMethod);
                    stated = true;
                    DiamondStatLog.addStatValue2(StatConstants.APP_NAME,
                        StatConstants.STAT_AVERAGE_HTTP_GET_NOT_MODIFIED, dataId, group, System.currentTimeMillis()
                                - start);
                    return result;
                }

                case SC_NOT_FOUND: {
                    log.warn("û���ҵ�DataIDΪ:" + dataId + "��Ӧ��������Ϣ");
                    DiamondStatLog.addStat(StatConstants.APP_NAME, appName, dataId + "-" + group, response);
                    // add by zxq���snapshot�еĶ�ӦdataId������
                    this.snapshotConfigInfoProcessor.removeSnapshot(dataId, group);
                    return null;
                }

                case SC_SERVICE_UNAVAILABLE: {
                    DiamondStatLog.addStat(StatConstants.APP_NAME, appName, dataId + "-" + group, response);
                    // ��ǰ�����������ã��л�
                    rotateToNextDomain();
                }
                    break;

                default: {
                    log.warn("HTTP State: " + httpStatus + ":" + httpClient.getState());
                    DiamondStatLog.addStat(StatConstants.APP_NAME, appName, dataId + "-" + group, response);
                    rotateToNextDomain();
                }
                }
            }
            catch (HttpException e) {
                log.error("��ȡ������ϢHttp�쳣" + e);
                rotateToNextDomain();
            }
            catch (IOException e) {

                log.error("��ȡ������ϢIO�쳣" + e);
                rotateToNextDomain();
            }
            catch (Exception e) {
                log.error("δ֪�쳣", e);
                rotateToNextDomain();
            }
            finally {
                httpMethod.releaseConnection();
                if (!stated) {
                    DiamondStatLog.addStatValue2(StatConstants.APP_NAME, StatConstants.STAT_AVERAGE_HTTP_GET_OTHER,
                        dataId, group, System.currentTimeMillis() - start);
                }
            }
        }
        throw new RuntimeException("��ȡConfigureInfomation��ʱ, DataID" + dataId + ", GroupΪ��" + group + ",��ʱʱ��Ϊ��"
                + timeout);
    }


    private CacheData getCacheData(String dataId, String group) {
        CacheData cacheData = null;
        ConcurrentHashMap<String, CacheData> cacheDatas = this.cache.get(dataId);
        if (null != cacheDatas) {
            cacheData = cacheDatas.get(group);
        }
        if (null == cacheData) {
            cacheData = new CacheData(dataId, group);
            ConcurrentHashMap<String, CacheData> newCacheDatas = new ConcurrentHashMap<String, CacheData>();
            ConcurrentHashMap<String, CacheData> oldCacheDatas = this.cache.putIfAbsent(dataId, newCacheDatas);
            if (null == oldCacheDatas) {
                oldCacheDatas = newCacheDatas;
            }
            if (null != oldCacheDatas.putIfAbsent(group, cacheData)) {
                cacheData = oldCacheDatas.get(group);
            }
        }
        return cacheData;
    }


    /**
     * �����Ľ��ΪRP_NO_CHANGE������������Ϊ��<br>
     * 1.��黺���е�MD5���뷵�ص�MD5���Ƿ�һ�£������һ�£���ɾ�������С������ٴβ�ѯ��<br>
     * 2.���MD5��һ�£���ֱ�ӷ���NULL<br>
     */
    private String getNotModified(String dataId, CacheData cacheData, HttpMethod httpMethod) {
        Header md5Header = httpMethod.getResponseHeader(Constants.CONTENT_MD5);
        if (null == md5Header) {
            throw new RuntimeException("RP_NO_CHANGE���صĽ����û��MD5��");
        }
        String md5 = md5Header.getValue();
        if (!cacheData.getMd5().equals(md5)) {
            String lastMd5 = cacheData.getMd5();
            cacheData.setMd5(Constants.NULL);
            cacheData.setLastModifiedHeader(Constants.NULL);
            throw new RuntimeException("MD5��У��Աȳ���,DataIDΪ:[" + dataId + "]�ϴ�MD5Ϊ:[" + lastMd5 + "]����MD5Ϊ:[" + md5
                    + "]");
        }

        cacheData.setMd5(md5);
        changeSpacingInterval(httpMethod);
        if (log.isInfoEnabled()) {
            log.info("DataId: " + dataId + ", ��Ӧ��configInfoû�б仯");
        }
        return null;
    }


    /**
     * �����Ľ��ΪRP_OK������������Ϊ��<br>
     * 1.��ȡ������Ϣ�����������ϢΪ�ջ����׳��쳣�����׳�����ʱ�쳣<br>
     * 2.���������Ϣ�Ƿ���ϻ�������е�MD5�룬�����ϣ����ٴλ�ȡ������Ϣ������¼��־<br>
     * 3.���ϣ���洢LastModified��Ϣ��MD5�룬������ѯ�ļ��ʱ�䣬����ȡ��������Ϣ���͸��ͻ��ļ�����<br>
     */
    private String getSuccess(String dataId, String group, CacheData cacheData, HttpMethod httpMethod) {
        String configInfo = Constants.NULL;
        configInfo = getContent(httpMethod);
        if (null == configInfo) {
            throw new RuntimeException("RP_OK��ȡ�˴����������Ϣ");
        }

        Header md5Header = httpMethod.getResponseHeader(Constants.CONTENT_MD5);
        if (null == md5Header) {
            throw new RuntimeException("RP_OK���صĽ����û��MD5��, " + configInfo);
        }
        String md5 = md5Header.getValue();
        if (!checkContent(configInfo, md5)) {
            throw new RuntimeException("������Ϣ��MD5��У�����,DataIDΪ:[" + dataId + "]������ϢΪ:[" + configInfo + "]MD5Ϊ:[" + md5
                    + "]");
        }

        Header lastModifiedHeader = httpMethod.getResponseHeader(Constants.LAST_MODIFIED);
        if (null == lastModifiedHeader) {
            throw new RuntimeException("RP_OK���صĽ����û��lastModifiedHeader");
        }
        String lastModified = lastModifiedHeader.getValue();

        cacheData.setMd5(md5);
        cacheData.setLastModifiedHeader(lastModified);

        changeSpacingInterval(httpMethod);

        // ���õ�����cache
        String key = makeCacheKey(dataId, group);
        contentCache.put(key, configInfo);

        // ��¼���յ�������
        StringBuilder buf = new StringBuilder();
        buf.append("dataId=").append(dataId);
        buf.append(" ,group=").append(group);
        buf.append(" ,content=").append(configInfo);
        dataLog.info(buf.toString());

        return configInfo;
    }


    private void configureHttpMethod(boolean skipContentCache, CacheData cacheData, long onceTimeOut,
            HttpMethod httpMethod) {
        if (skipContentCache && null != cacheData) {
            if (null != cacheData.getLastModifiedHeader() && Constants.NULL != cacheData.getLastModifiedHeader()) {
                httpMethod.addRequestHeader(Constants.IF_MODIFIED_SINCE, cacheData.getLastModifiedHeader());
            }
            if (null != cacheData.getMd5() && Constants.NULL != cacheData.getMd5()) {
                httpMethod.addRequestHeader(Constants.CONTENT_MD5, cacheData.getMd5());
            }
        }
        // ����appName�Ϳͻ��˰汾
        if (null != this.appName) {
            httpMethod.addRequestHeader(Constants.APPNAME, this.appName);
        }
        httpMethod.addRequestHeader(Constants.CLIENT_VERSION_HEADER, Constants.CLIENT_VERSION);

        httpMethod.addRequestHeader(Constants.ACCEPT_ENCODING, "gzip,deflate");

        // ����HttpMethod�Ĳ���
        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout((int) onceTimeOut);
        // ///////////////////////
        httpMethod.setParams(params);
        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(this.domainNamePos.get()),
            diamondConfigure.getPort());
    }


    private String makeCacheKey(String dataId, String group) {
        String key = dataId + "-" + group;
        return key;
    }


    /**
     * ��DiamondServer��ȡֵ�仯�˵�DataID�б�
     * 
     * @param timeout
     * @return
     */
    Set<String> checkUpdateDataIds(long timeout) {
        if (!isRun) {
            throw new RuntimeException("DiamondSubscriber��������״̬�У��޷���ȡ�޸Ĺ���DataID�б�");
        }
        // =======================ʹ�ò���ģʽ=======================
        if (MockServer.isTestMode()) {
            return testData();
        }
        // ==========================================================
        long waitTime = 0;

        // Set<String> localModifySet = getLocalUpdateDataIds();
        String probeUpdateString = getProbeUpdateString();
        if (StringUtils.isBlank(probeUpdateString)) {
            return null;
        }

        long start = System.currentTimeMillis();
        while (0 == timeout || timeout > waitTime) {
            // ���ó�ʱʱ��
            long onceTimeOut = getOnceTimeOut(waitTime, timeout);
            waitTime += onceTimeOut;

            PostMethod postMethod = new PostMethod(Constants.HTTP_URI_FILE);

            postMethod.addParameter(Constants.PROBE_MODIFY_REQUEST, probeUpdateString);

            if (null != this.appName) {
                postMethod.addRequestHeader(Constants.APPNAME, this.appName);
            }
            postMethod.addRequestHeader(Constants.CLIENT_VERSION_HEADER, Constants.CLIENT_VERSION);
            // ����HttpMethod�Ĳ���
            HttpMethodParams params = new HttpMethodParams();
            params.setSoTimeout((int) onceTimeOut);
            // ///////////////////////
            postMethod.setParams(params);

            try {
                httpClient.getHostConfiguration()
                    .setHost(diamondConfigure.getDomainNameList().get(this.domainNamePos.get()),
                        this.diamondConfigure.getPort());

                int httpStatus = httpClient.executeMethod(postMethod);

                switch (httpStatus) {
                case SC_OK: {

                    Set<String> result = getUpdateDataIds(postMethod);
                    // ͳ��ƽ��checkʱ��
                    DiamondStatLog.addStatValue2(StatConstants.APP_NAME, StatConstants.STAT_AVERAGE_HTTP_POST_CHECK,
                        System.currentTimeMillis() - start);
                    return result;
                }

                case SC_SERVICE_UNAVAILABLE: {
                    rotateToNextDomain();
                }
                    break;

                default: {
                    log.warn("��ȡ�޸Ĺ���DataID�б��������Ӧ��HTTP State: " + httpStatus);
                    rotateToNextDomain();
                }
                }
            }
            catch (HttpException e) {
                log.error("��ȡ������ϢHttp�쳣" + e);
                rotateToNextDomain();
            }
            catch (IOException e) {
                log.error("��ȡ������ϢIO�쳣" + e);
                rotateToNextDomain();
            }
            catch (Exception e) {
                log.error("δ֪�쳣", e);
                rotateToNextDomain();
            }
            finally {
                postMethod.releaseConnection();
            }
        }
        throw new RuntimeException("��ȡ�޸Ĺ���DataID�б���ʱ "
                + diamondConfigure.getDomainNameList().get(this.domainNamePos.get()) + ", ��ʱʱ��Ϊ��" + timeout);
    }


    void sendAckToServer(String dataId, String group, String message, String listenerName, RotateType rotateType, long timeout) {
        
        long waitTime = 0;
        
        while(timeout == 0 || timeout > waitTime) {
            // ���ñ��γ�ʱʱ��
            long onceTimeOut = getOnceTimeOut(waitTime, timeout);
            waitTime += onceTimeOut;
            
            GetMethod ackMethod = new GetMethod(getAckUrl(dataId, group, message, listenerName, rotateType));
            configureAckHttpMethod(ackMethod, onceTimeOut);
            
            try {
                int status = httpClient.executeMethod(ackMethod);
                
                switch(status) {
                
                case SC_OK: {
                    log.info("�ͻ�����ȷ�����˷����˴������");
                    return;
                }

                case SC_NOT_FOUND: {
                    log.warn("client.ack�ļ�������");
                    rotateToNextDomain();
                    break;
                }

                case SC_SERVICE_UNAVAILABLE: {
                    log.warn("��ǰdiamond server������");
                    rotateToNextDomain();
                    break;
                }

                default: {
                    log.warn("����HTTP״̬��: " + httpClient.getState());
                    rotateToNextDomain();
                }
                
                }
            }
            catch (HttpException e) {
                log.error("�ͻ��������˷��ʹ������ʱ����HTTP�쳣��", e);
                rotateToNextDomain();
            }
            catch (IOException e) {
                log.error("�ͻ��������˷��ʹ������ʱ����IO�쳣��", e);
                rotateToNextDomain();
            }
            catch(Exception e) {
                log.error("�ͻ��������˷��ʹ������ʱ����δ֪�쳣��", e);
                rotateToNextDomain();
            }
            finally {
                ackMethod.releaseConnection();
            }
        }
        
        log.error("�ͻ��������˷��ʹ��������ʱ, ��ʱʱ��Ϊ:" + timeout + "ms");
        
    }


    private String getAckUrl(String dataId, String group, String ackMessage, String listenerName, RotateType rotateType) {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.HTTP_URI_ACK);
        uriBuilder.append("?");
        uriBuilder.append("dataId=").append(dataId).append("&");
        uriBuilder.append("group=").append(group).append("&");
        uriBuilder.append("ack=").append(ackMessage).append("&");
        uriBuilder.append("listener=").append(listenerName).append("&");
        uriBuilder.append("type=").append(rotateType);

        return uriBuilder.toString();
    }
    
    
    private void configureAckHttpMethod(HttpMethod httpMethod, long onceTimeOut) {
        // ����appName�Ϳͻ��˰汾
        if (null != this.appName) {
            httpMethod.addRequestHeader(Constants.APPNAME, this.appName);
        }
        httpMethod.addRequestHeader(Constants.CLIENT_VERSION_HEADER, Constants.CLIENT_VERSION);

        // ����HttpMethod�Ĳ���
        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout((int) onceTimeOut);
        
        httpMethod.setParams(params);
        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(this.domainNamePos.get()),
            diamondConfigure.getPort());
    }


    private Set<String> testData() {
        Set<String> dataIdList = new HashSet<String>();
        for (String dataId : this.cache.keySet()) {
            ConcurrentHashMap<String, CacheData> cacheDatas = this.cache.get(dataId);
            for (String group : cacheDatas.keySet()) {
                if (null != MockServer.getUpdateConfigInfo(dataId, group)) {
                    dataIdList.add(dataId + WORD_SEPARATOR + group);
                }
            }
        }
        return dataIdList;
    }


    /**
     * ��ȡ���ظ����˵�DataID
     * 
     * @return
     */
    // private Set<String> getLocalUpdateDataIds() {
    // Set<String> probeModifySet = new HashSet<String>();
    // for (Entry<String, ConcurrentHashMap<String, CacheData>> cacheDatasEntry
    // : this.cache.entrySet()) {
    // ConcurrentHashMap<String, CacheData> cacheDatas =
    // cacheDatasEntry.getValue();
    // if (null == cacheDatas) {
    // continue;
    // }
    // for (Entry<String, CacheData> cacheDataEntry : cacheDatas.entrySet()) {
    // if
    // (this.localConfigInfoProcessor.containsNewLocalConfigureInfomation(cacheDataEntry.getValue()))
    // {
    // probeModifySet.add(cacheDataEntry.getKey());
    // }
    // }
    // }
    // return probeModifySet;
    // }

    /**
     * ��ȡ̽����µ�DataID�������ַ���
     * 
     * @param localModifySet
     * @return
     */
    private String getProbeUpdateString() {
        // ��ȡcheck��DataID:Group:MD5��
        StringBuilder probeModifyBuilder = new StringBuilder();
        for (Entry<String, ConcurrentHashMap<String, CacheData>> cacheDatasEntry : this.cache.entrySet()) {
            String dataId = cacheDatasEntry.getKey();
            ConcurrentHashMap<String, CacheData> cacheDatas = cacheDatasEntry.getValue();
            if (null == cacheDatas) {
                continue;
            }
            for (Entry<String, CacheData> cacheDataEntry : cacheDatas.entrySet()) {
                final CacheData data = cacheDataEntry.getValue();
                // ��ʹ�ñ������ã���ȥdiamond server���
                if (!data.isUseLocalConfigInfo()) {
                    probeModifyBuilder.append(dataId).append(WORD_SEPARATOR);

                    if (null != cacheDataEntry.getValue().getGroup()
                            && Constants.NULL != cacheDataEntry.getValue().getGroup()) {
                        probeModifyBuilder.append(cacheDataEntry.getValue().getGroup()).append(WORD_SEPARATOR);
                    }
                    else {
                        probeModifyBuilder.append(WORD_SEPARATOR);
                    }

                    if (null != cacheDataEntry.getValue().getMd5()
                            && Constants.NULL != cacheDataEntry.getValue().getMd5()) {
                        probeModifyBuilder.append(cacheDataEntry.getValue().getMd5()).append(LINE_SEPARATOR);
                    }
                    else {
                        probeModifyBuilder.append(LINE_SEPARATOR);
                    }
                }
            }
        }
        String probeModifyString = probeModifyBuilder.toString();
        return probeModifyString;
    }


    /**
     * �����Ժ���ܴ�����һ���̳߳���ͬʱ������DataID��Ӧ��ConfigInfo�������Ϊ�����໥Ӱ�죬
     * ÿ��DataID��DomainPos�ǲ�һ����
     * 
     * @param cacheData
     */
    /*
     * void rotateToNextDomain(CacheData cacheData) { synchronized (cacheData) {
     * int domainNameCount = diamondConfigure.getDomainNameList().size(); int
     * index = cacheData.getDomainNamePos().incrementAndGet(); // �������� if(index
     * < 0) { index = -index; } cacheData.getDomainNamePos().set(index %
     * domainNameCount); if (diamondConfigure.getDomainNameList().size() > 0)
     * log.warn("DataID: [" + cacheData.getDataId() + "]�ֻ�DiamondServer��������" +
     * diamondConfigure
     * .getDomainNameList().get(cacheData.getDomainNamePos().get())); } }
     */

    /**
     * ����϶�ͬʱֻ��һ����ѯ��
     */
    synchronized void rotateToNextDomain() {
        int domainNameCount = diamondConfigure.getDomainNameList().size();
        int index = domainNamePos.incrementAndGet();
        if (index < 0) {
            index = -index;
        }
        if (domainNameCount == 0) {
            log.error("diamond��������ַ�б�����Ϊ��, ����ϵ�������Ų�");
            return;
        }
        domainNamePos.set(index % domainNameCount);
        if (diamondConfigure.getDomainNameList().size() > 0)
            log.warn("�ֻ�DiamondServer��������"
                    + diamondConfigure.getDomainNameList().get(domainNamePos.get()));
    }


    /**
     * ��ȡ��ѯUri��String
     * 
     * @param dataId
     * @param group
     * @return
     */
    String getUriString(String dataId, String group) {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.HTTP_URI_FILE);
        uriBuilder.append("?");
        uriBuilder.append(Constants.DATAID).append("=").append(dataId);
        if (null != group) {
            uriBuilder.append("&");
            uriBuilder.append(Constants.GROUP).append("=").append(group);
        }
        return uriBuilder.toString();
    }


    /**
     * �����µ���Ϣ��ѯ���ʱ��
     * 
     * @param httpMethod
     */
    void changeSpacingInterval(HttpMethod httpMethod) {
        Header[] spacingIntervalHeaders = httpMethod.getResponseHeaders(Constants.SPACING_INTERVAL);
        if (spacingIntervalHeaders.length >= 1) {
            try {
                diamondConfigure.setPollingIntervalTime(Integer.parseInt(spacingIntervalHeaders[0].getValue()));
            }
            catch (RuntimeException e) {
                log.error("�����´μ��ʱ��ʧ��", e);
            }
        }
    }


    /**
     * ��ȡResponse��������Ϣ
     * 
     * @param httpMethod
     * @return
     */
    String getContent(HttpMethod httpMethod) {
        StringBuilder contentBuilder = new StringBuilder();
        if (isZipContent(httpMethod)) {
            // ����ѹ������������Ϣ���߼�
            InputStream is = null;
            GZIPInputStream gzin = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                is = httpMethod.getResponseBodyAsStream();
                gzin = new GZIPInputStream(is);
                isr = new InputStreamReader(gzin, ((HttpMethodBase) httpMethod).getResponseCharSet()); // ���ö�ȡ���ı����ʽ���Զ������
                br = new BufferedReader(isr);
                char[] buffer = new char[4096];
                int readlen = -1;
                while ((readlen = br.read(buffer, 0, 4096)) != -1) {
                    contentBuilder.append(buffer, 0, readlen);
                }
            }
            catch (Exception e) {
                log.error("��ѹ��ʧ��", e);
            }
            finally {
                try {
                    br.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    isr.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    gzin.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    is.close();
                }
                catch (Exception e1) {
                    // ignore
                }
            }
        }
        else {
            // ����û�б�ѹ������������Ϣ���߼�
            String content = null;
            try {
                content = httpMethod.getResponseBodyAsString();
            }
            catch (Exception e) {
                log.error("��ȡ������Ϣʧ��", e);
            }
            if (null == content) {
                return null;
            }
            contentBuilder.append(content);
        }
        return contentBuilder.toString();
    }


    Set<String> getUpdateDataIdsInBody(HttpMethod httpMethod) {
        Set<String> modifiedDataIdSet = new HashSet<String>();
        try {
            String modifiedDataIdsString = httpMethod.getResponseBodyAsString();
            return convertStringToSet(modifiedDataIdsString);
        }
        catch (Exception e) {

        }
        return modifiedDataIdSet;

    }


    Set<String> getUpdateDataIds(HttpMethod httpMethod) {
        return getUpdateDataIdsInBody(httpMethod);
    }


    private Set<String> convertStringToSet(String modifiedDataIdsString) {

        if (null == modifiedDataIdsString || "".equals(modifiedDataIdsString)) {
            return null;
        }

        Set<String> modifiedDataIdSet = new HashSet<String>();

        try {
            modifiedDataIdsString = URLDecoder.decode(modifiedDataIdsString, "UTF-8");
        }
        catch (Exception e) {
        	log.error("����modifiedDataIdsString����", e);
        }
        
        if (log.isInfoEnabled() && modifiedDataIdsString != null) {
        	if(modifiedDataIdsString.startsWith("OK")) {
                log.debug("̽��ķ��ؽ��:" + modifiedDataIdsString);
            }
            else {
                log.info("̽�⵽���ݱ仯:" + modifiedDataIdsString);
            }
        }

        final String[] modifiedDataIdStrings = modifiedDataIdsString.split(LINE_SEPARATOR);
        for (String modifiedDataIdString : modifiedDataIdStrings) {
            if (!"".equals(modifiedDataIdString)) {
                modifiedDataIdSet.add(modifiedDataIdString);
            }
        }
        return modifiedDataIdSet;
    }


    /**
     * ���������Ϣ������MD5���Ƿ�һ��
     * 
     * @param configInfo
     * @param md5
     * @return
     */
    boolean checkContent(String configInfo, String md5) {
        String realMd5 = MD5.getInstance().getMD5String(configInfo);
        return realMd5 == null ? md5 == null : realMd5.equals(md5);
    }


    /**
     * �鿴�Ƿ�Ϊѹ��������
     * 
     * @param httpMethod
     * @return
     */
    boolean isZipContent(HttpMethod httpMethod) {
        if (null != httpMethod.getResponseHeader(Constants.CONTENT_ENCODING)) {
            String acceptEncoding = httpMethod.getResponseHeader(Constants.CONTENT_ENCODING).getValue();
            if (acceptEncoding.toLowerCase().indexOf("gzip") > -1) {
                return true;
            }
        }
        return false;
    }


    public void setSubscriberListener(SubscriberListener subscriberListener) {
        this.subscriberListener = subscriberListener;
    }


    public SubscriberListener getSubscriberListener() {
        return this.subscriberListener;
    }


    public PushitClient getPushitClient() {
        return pushitClient;
    }


    public void addDataId(String dataId, String group) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy��MM��dd��   HH:mm:ss");
        log.info("diamond client start:" + formatter.format(new Date(System.currentTimeMillis())));
        if (null == group) {
            group = Constants.DEFAULT_GROUP;
        }

        ConcurrentHashMap<String, CacheData> cacheDatas = this.cache.get(dataId);
        if (null == cacheDatas) {
            ConcurrentHashMap<String, CacheData> newCacheDatas = new ConcurrentHashMap<String, CacheData>();
            ConcurrentHashMap<String, CacheData> oldCacheDatas = this.cache.putIfAbsent(dataId, newCacheDatas);
            if (null != oldCacheDatas) {
                cacheDatas = oldCacheDatas;
            }
            else {
                cacheDatas = newCacheDatas;
            }
        }
        CacheData cacheData = cacheDatas.get(group);
        if (null == cacheData) {
            cacheDatas.putIfAbsent(group, new CacheData(dataId, group));
            if (log.isInfoEnabled()) {
                log.info("������DataID[" + dataId + "]����GroupΪ" + group);
            }
            this.start();
            DiamondClientUtil.addDataId(this.clusterType, dataId + "-" + group);
        }
    }


    public void addDataId(String dataId) {
        addDataId(dataId, null);
    }


    public boolean containDataId(String dataId) {
        return containDataId(dataId, null);
    }


    public boolean containDataId(String dataId, String group) {
        if (null == group) {
            group = Constants.DEFAULT_GROUP;
        }
        ConcurrentHashMap<String, CacheData> cacheDatas = this.cache.get(dataId);
        if (null == cacheDatas) {
            return false;
        }
        return cacheDatas.containsKey(group);
    }


    public void clearAllDataIds() {
        this.cache.clear();
    }


    public Set<String> getDataIds() {
        return new HashSet<String>(this.cache.keySet());
    }


    public ConcurrentHashMap<String, ConcurrentHashMap<String, CacheData>> getCache() {
        return cache;
    }


    public void removeDataId(String dataId) {
        removeDataId(dataId, null);
    }


    public synchronized void removeDataId(String dataId, String group) {
        if (null == group) {
            group = Constants.DEFAULT_GROUP;
        }
        ConcurrentHashMap<String, CacheData> cacheDatas = this.cache.get(dataId);
        if (null == cacheDatas) {
            return;
        }
        cacheDatas.remove(group);
        
        log.warn("ɾ����DataID[" + dataId + "]�е�Group: " + group);
        
        if(cacheDatas.size() == 0) {
            this.cache.remove(dataId);
            log.warn("ɾ����DataID[" + dataId + "]");
        }
    }


    public DiamondConfigure getDiamondConfigure() {
        return this.diamondConfigure;
    }


    public void setDiamondConfigure(DiamondConfigure diamondConfigure) {
        if (!isRun) {
            this.diamondConfigure = diamondConfigure;
        }
        else {
            // ����֮��ĳЩ�����޷�����
            copyDiamondConfigure(diamondConfigure);
        }
        if (!diamondConfigure.getDomainNameList().isEmpty()) {
            DiamondClientUtil.setServerAddrs(this.clusterType, diamondConfigure.getDomainNameList());
        }
    }


    private void copyDiamondConfigure(DiamondConfigure diamondConfigure) {
        // TODO ��Щֵ����������ʱ��̬����?
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
    
    
    public void setAppName(String appName) {
        this.appName = appName;
    }

}