package com.taobao.diamond.client.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.taobao.diamond.client.DiamondConfigure;
import com.taobao.diamond.client.SubscriberListener;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.configinfo.ConfigureInfomation;
import com.taobao.diamond.md5.MD5;


public class DefaultDiamondSubscriberUnitTest {
    // diamond server address (ip)
    private static final String DIAMOND_SERVER_ADDR = "";
    // pushit server address (ip:port)
    private static final String PUSHIT_SERVER_ADDR = "";

    private DefaultDiamondPublisher publisher;
    private DefaultDiamondSubscriber subscriber;
    private SubscriberListener listener;

    private String clusterType = "diamond";


    @Before
    public void setUp() throws Exception {
        initPublisherAndSubscriber();
    }


    @After
    public void tearDown() throws Exception {
        publisher.close();
        subscriber.close();
    }


    @Test
    public void test_被动获取() throws Exception {
        this.publisher.close();
        this.subscriber.close();
        final String dataId = UUID.randomUUID().toString();
        final String group = "leiwen";
        final String content = "test bei dong huo qu";
        final java.util.concurrent.atomic.AtomicBoolean invoked = new AtomicBoolean(false);
        this.subscriber.setSubscriberListener(new SubscriberListener() {

            public void receiveConfigInfo(ConfigureInfomation configureInfomation) {
                System.out.println("接收到配置信息" + configureInfomation);
                assertEquals(dataId, configureInfomation.getDataId());
                assertEquals(group, configureInfomation.getGroup());
                assertEquals(content, configureInfomation.getConfigureInfomation());
                invoked.set(true);
            }


            public Executor getExecutor() {
                return null;
            }
        });
        this.subscriber.addDataId(dataId, group);
        this.subscriber.start();
        this.publisher.addDataId(dataId, group, content);
        this.publisher.start();

        this.publisher.publishNew(dataId, group, content);

        while (!invoked.get()) {
            Thread.sleep(1000);
        }
    }


    @Test
    @Ignore
    public void test_GetAvaiableConfigInfomation_优先使用本地配置() throws Exception {
        this.publisher.close();
        this.subscriber.close();
        final String dataId = UUID.randomUUID().toString();
        final String group = "leiwen";
        final String content = "test bei di pei zhi";
        final java.util.concurrent.atomic.AtomicBoolean invoked = new AtomicBoolean(false);
        this.subscriber.setSubscriberListener(new SubscriberListener() {

            public void receiveConfigInfo(ConfigureInfomation configureInfomation) {
                System.out.println("接收到配置信息" + configureInfomation);
                assertEquals(dataId, configureInfomation.getDataId());
                assertEquals(group, configureInfomation.getGroup());
                assertEquals(content, configureInfomation.getConfigureInfomation());
                invoked.set(true);
            }


            public Executor getExecutor() {
                return null;
            }
        });
        // 加入本地配置
        File dir = new File(System.getProperty("user.home") + "/diamond/" + clusterType + "/data/config-data");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        dir = new File(System.getProperty("user.home") + "/diamond/" + clusterType + "/data/config-data/" + group);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file =
                new File(System.getProperty("user.home") + "/diamond/" + clusterType + "/data/config-data/" + group
                        + "/" + dataId);
        if (!file.exists())
            file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write("local config".getBytes());
        out.flush();
        out.close();
        Thread.sleep(10000);

        this.subscriber.addDataId(dataId, group);
        this.subscriber.start();
        this.publisher.start();

        assertEquals("local config", this.subscriber.getAvailableConfigureInfomation(dataId, group, 1000));
        file.delete();

        this.publisher.publish(dataId, group, content);

        while (!invoked.get()) {
            Thread.sleep(1000);
        }
    }


    @Test
    public void subscriberListenerTest() {
        this.subscriber.setSubscriberListener(listener);
        Assert.assertEquals(listener, subscriber.getSubscriberListener());
        SubscriberListener tempListener = new TestSubscriberListener();
        this.subscriber.setSubscriberListener(tempListener);
        Assert.assertEquals(tempListener, subscriber.getSubscriberListener());
    }


    @Test
    public void diamondConfigureTest() {
        DiamondConfigure configure = new DiamondConfigure("diamond");
        subscriber.setDiamondConfigure(configure);
        // 运行后设置无效
        Assert.assertFalse(configure == subscriber.getDiamondConfigure());
    }


    @Test
    public void md5Test() {
        Assert.assertTrue(((DefaultDiamondSubscriber) subscriber).checkContent("configInfo", MD5.getInstance()
            .getMD5String("configInfo")));
        Assert.assertTrue(((DefaultDiamondSubscriber) subscriber).checkContent("傲穹",
            MD5.getInstance().getMD5String("傲穹")));
    }


    @Test
    public void testGetConfigureInfomation() throws Exception {
        this.publisher.close();
        this.subscriber.close();
        String dataId = UUID.randomUUID().toString();
        String group = "leiwen";
        String content = "test get configure information";
        this.subscriber.addDataId(dataId, group);
        this.subscriber.start();
        this.publisher.start();

        publisher.publishNew(dataId, group, content);
        String result = ((DefaultDiamondSubscriber) this.subscriber).getConfigureInfomation(dataId, group, 5000, true);
        Assert.assertTrue(result.contains(content));
        // 再次查询，返回null,因为没有改变
        result = ((DefaultDiamondSubscriber) this.subscriber).getConfigureInfomation(dataId, group, 5000, true);
        assertNull(result);

    }


    /*
     * catch (RuntimeException e) { assertEquals(
     * "获取ConfigureInfomation超时, DataIDnotify, Group为：test,超时时间为：1000",
     * e.getMessage()); }
     */

    @Test
    public void testGetConfigureInfomation_NotFound() {
        this.publisher.close();
        this.subscriber.close();
        String dataId = UUID.randomUUID().toString();
        String group = "leiwen";

        this.subscriber.addDataId(dataId, group);
        this.subscriber.start();
        this.publisher.start();

        assertNull(((DefaultDiamondSubscriber) this.subscriber).getConfigureInfomation(dataId, group, 1000, true));
    }


    @Test
    @Ignore
    public void testRotateToNextDomain() throws Exception {
        this.publisher.close();
        this.subscriber.close();
        String dataId = UUID.randomUUID().toString();
        String group = "leiwen";
        String content = "test rotate domain";
        this.subscriber.addDataId(dataId, group);
        this.subscriber.start();
        this.publisher.start();
        this.publisher.publishNew(dataId, group, content);
        List<String> list = new ArrayList<String>();
        list.add("unknow_host");
        list.add("localhost");
        this.subscriber.getDiamondConfigure().setDomainNameList(list);
        this.subscriber.getDiamondConfigure().setPort(8080);
        ((DefaultDiamondSubscriber) this.subscriber).setDomainNamesPos(0);

        // 第一次抛异常并切换域名
        try {
            ((DefaultDiamondSubscriber) this.subscriber).getConfigureInfomation(dataId, group, 1000, true);
            fail();
        }
        catch (RuntimeException e) {

            assertEquals("获取ConfigureInfomation超时, DataIDnotify, Group为：test,超时时间为：1000", e.getMessage());
        }
        assertEquals(content,
            ((DefaultDiamondSubscriber) this.subscriber).getConfigureInfomation(dataId, group, 1000, true));
    }


    @Test
    public void onceTimeOutTest() {
        this.publisher.close();
        this.subscriber.close();
        subscriber.getDiamondConfigure().setOnceTimeout(10);
        Assert.assertEquals(10, ((DefaultDiamondSubscriber) this.subscriber).getOnceTimeOut(20, 100));
        Assert.assertEquals(9, ((DefaultDiamondSubscriber) this.subscriber).getOnceTimeOut(91, 100));
        Assert.assertEquals(1, ((DefaultDiamondSubscriber) this.subscriber).getOnceTimeOut(99, 100));
    }


    @Test
    public void uriTest() {
        String uri1 = ((DefaultDiamondSubscriber) this.subscriber).getUriString("dataId", "group");
        Assert.assertEquals(Constants.HTTP_URI_FILE + "?" + Constants.DATAID + "=dataId&" + Constants.GROUP + "=group",
            uri1);

        String uri2 = ((DefaultDiamondSubscriber) this.subscriber).getUriString("dataId", null);
        Assert.assertEquals(Constants.HTTP_URI_FILE + "?" + Constants.DATAID + "=dataId", uri2);
    }


    @Test
    public void dataIdTest() {
        subscriber.addDataId("dataId1", "group1");
        subscriber.addDataId("dataId2", "group2");
        subscriber.addDataId("dataId3", "group3");
        subscriber.addDataId("dataId1", "group3");

        Assert.assertTrue(subscriber.containDataId("dataId1", "group1"));

        Assert.assertFalse(subscriber.containDataId("dataId1"));
        Assert.assertFalse(subscriber.containDataId("dataId2"));
        Assert.assertFalse(subscriber.containDataId("dataId3"));

        Assert.assertTrue(subscriber.containDataId("dataId2", "group2"));
        Assert.assertTrue(subscriber.containDataId("dataId3", "group3"));
        Assert.assertTrue(subscriber.containDataId("dataId1", "group3"));

        Set<String> dataIds1 = subscriber.getDataIds();
        Assert.assertEquals(3, dataIds1.size());
        Assert.assertTrue(dataIds1.contains("dataId1"));
        Assert.assertTrue(dataIds1.contains("dataId2"));
        Assert.assertTrue(dataIds1.contains("dataId3"));

        subscriber.clearAllDataIds();
        Set<String> dataIds2 = subscriber.getDataIds();
        Assert.assertEquals(0, dataIds2.size());
    }


    private void initPublisherAndSubscriber() throws Exception {
        publisher = (DefaultDiamondPublisher) DiamondClientFactory.getSingletonDiamondPublisher(clusterType);
        publisher.getDiamondConfigure().addDomainName(DIAMOND_SERVER_ADDR);
        publisher.getDiamondConfigure().addPushitDomainName(PUSHIT_SERVER_ADDR);
        publisher.getDiamondConfigure().setLocalFirst(true);

        subscriber = (DefaultDiamondSubscriber) DiamondClientFactory.getSingletonDiamondSubscriber(clusterType);
        subscriber.getDiamondConfigure().addDomainName(DIAMOND_SERVER_ADDR);
        subscriber.getDiamondConfigure().addPushitDomainName(PUSHIT_SERVER_ADDR);
        subscriber.getDiamondConfigure().setLocalFirst(true);

        publisher.setDiamondSubscriber(subscriber);

        publisher.start();
        subscriber.start();
    }

}
