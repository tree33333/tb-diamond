package com.taobao.diamond.manager.impl;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.impl.DiamondClientFactory;
import com.taobao.diamond.manager.BaseStonePubManager;
import com.taobao.diamond.manager.BaseStoneSubManager;
import com.taobao.diamond.manager.ManagerListener;


public class DefaultBaseStoneSubManagerUnitTest {
    // diamond server address (ip)
    private static final String DIAMOND_SERVER_ADDR = "";
    // diamond server address (ip:port)
    private static final String PUSHIT_SERVER_ADDR = "";

    private static final String GROUP = "BS_SUB_TEST";

    private BaseStonePubManager pubManager;
    private BaseStoneSubManager subManager;
    private DiamondPublisher publisher;
    private DiamondSubscriber subscriber;

    private String clusterType = "basestone";


    @Before
    public void setUp() throws Exception {
        initPubAndSub();
    }


    @After
    public void tearDown() throws Exception {
        publisher.close();
        subscriber.close();
    }


    @Test
    public void testRealTimeNotify() throws Exception {
        publisher.close();
        subscriber.close();
        final String dataId = UUID.randomUUID().toString();
        final String group = GROUP;
        // protocal://ip:port?k=v
        final String content = "";
        final java.util.concurrent.atomic.AtomicBoolean invoked = new AtomicBoolean(false);
        pubManager = new DefaultBaseStonePubManager(dataId, group);
        subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

            public Executor getExecutor() {
                return null;
            }


            public void receiveConfigInfo(String configInfo) {
                System.out.println("received configInfo:" + configInfo);
                assertEquals(content, configInfo);
                invoked.set(true);
            }

        });

        pubManager.publish(content);

        while (!invoked.get()) {
            Thread.sleep(1000);
        }
    }


    @Test
    public void testNotUseRealTimeNotify() throws Exception {
        publisher.close();
        subscriber.close();
        final String dataId = UUID.randomUUID().toString();
        final String group = GROUP;
        // protocal://ip:port?k=v
        final String content = "";
        final java.util.concurrent.atomic.AtomicBoolean invoked = new AtomicBoolean(false);
        pubManager = new DefaultBaseStonePubManager(dataId, group);
        subManager = new DefaultBaseStoneSubManager(dataId, group, clusterType, new ManagerListener() {

            public Executor getExecutor() {
                return null;
            }


            public void receiveConfigInfo(String configInfo) {
                System.out.println("received configInfo:" + configInfo);
                Assert.fail();
                invoked.set(true);
            }

        }, false);

        pubManager.publish(content);
        pubManager.awaitPublishFinish();

        int count = 0;
        int maxCount = 10;
        while (!invoked.get() && count < maxCount) {
            count++;
            Thread.sleep(500);
        }

        // 实时通知没有起作用，尝试主动获取
        String result = subManager.getAvailableConfigureInfomation(60000);
        assertEquals(content, result);
    }


    @Test
    public void testGetConfigFromSnapshot() throws Exception {
        publisher.close();
        subscriber.close();

        String dataId = UUID.randomUUID().toString();
        String group = GROUP;
        String content = "test from snapshot";

        subManager = new DefaultBaseStoneSubManager(dataId, group, clusterType, new ManagerListener() {

            public Executor getExecutor() {
                return null;
            }


            public void receiveConfigInfo(String configInfo) {
                System.out.println("received configInfo:" + configInfo);
            }

        }, false);

        File snapshotFile = getSnapshotFile(dataId, group);
        if (!snapshotFile.exists()) {
            snapshotFile.createNewFile();
        }

        PrintWriter pw = new PrintWriter(new FileWriter(snapshotFile));
        pw.print(content);
        pw.flush();
        pw.close();

        try {
            String receivedConfigInfo = subManager.getAvailableConfigureInfomationFromSnapshot(10000);
            Assert.assertEquals(content, receivedConfigInfo);
        }
        finally {
            if (snapshotFile.exists()) {
                snapshotFile.delete();
            }
        }
    }


    /**
     * 测试exists接口
     * 
     * @throws Exception
     */
    @Test
    public void testExists() throws Exception {
        publisher.close();
        subscriber.close();

        String dataId = UUID.randomUUID().toString();
        String group = GROUP;
        subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

            public Executor getExecutor() {
                return null;
            }


            public void receiveConfigInfo(String configInfo) {
                System.out.println("接收到配置信息:" + configInfo);
            }

        });

        Assert.assertFalse(subManager.exists(dataId, group));
    }


    private void initPubAndSub() throws Exception {
        publisher = DiamondClientFactory.getSingletonDiamondPublisher(clusterType);
        subscriber = DiamondClientFactory.getSingletonDiamondSubscriber(clusterType);
        publisher.getDiamondConfigure().addDomainName(DIAMOND_SERVER_ADDR);
        publisher.getDiamondConfigure().addPushitDomainName(PUSHIT_SERVER_ADDR);
        subscriber.getDiamondConfigure().addDomainName(DIAMOND_SERVER_ADDR);
        subscriber.getDiamondConfigure().addPushitDomainName(PUSHIT_SERVER_ADDR);
        publisher.getDiamondConfigure().setLocalFirst(true);
        subscriber.getDiamondConfigure().setLocalFirst(true);
        publisher.setDiamondSubscriber(subscriber);
    }


    private File getSnapshotFile(String dataId, String group) {
        StringBuilder sb = new StringBuilder(this.subscriber.getDiamondConfigure().getFilePath());
        sb.append(File.separator).append("snapshot");
        sb.append(File.separator).append(group);
        sb.append(File.separator).append(dataId);

        return new File(sb.toString());
    }
}
