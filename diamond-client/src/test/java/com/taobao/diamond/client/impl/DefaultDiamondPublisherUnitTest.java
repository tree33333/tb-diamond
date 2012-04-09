package com.taobao.diamond.client.impl;

import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.common.Constants;


public class DefaultDiamondPublisherUnitTest {
    // diamond server address (ip)
    private static final String DIAMOND_SERVER_ADDR = "";
    // pushit server address (ip:port)
    private static final String PUSHIT_SERVER_ADDR = "";

    private DefaultDiamondPublisher publisher;
    private DefaultDiamondSubscriber subscriber;

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
    public void testPublishNew() throws Exception {
        publisher.close();
        subscriber.close();
        String dataId = UUID.randomUUID().toString();
        String group = "leiwen";
        String content = "test publish";
        publisher.addDataId(dataId, group, content);
        publisher.start();
        subscriber.addDataId(dataId, group);
        subscriber.start();
        publisher.publishNew(dataId, group, content);
        Thread.sleep(1000);

        String configInfo = subscriber.getAvailableConfigureInfomation(dataId, group, 60000);
        Assert.assertEquals(content, configInfo);
    }


    @Test
    public void testPublishUpdate() throws Exception {
        publisher.close();
        subscriber.close();
        String dataId = UUID.randomUUID().toString();
        String group = "leiwen";
        String content = "test update";
        publisher.addDataId(dataId, group, content);
        publisher.start();
        subscriber.addDataId(dataId, group);
        subscriber.start();
        publisher.publishNew(dataId, group, content);
        Thread.sleep(1000);

        String newContent = "test update new" + Constants.WORD_SEPARATOR + "test update new";
        publisher.publishUpdate(dataId, group, newContent);
        Thread.sleep(1000);

        String newReceivedConfigInfo = subscriber.getAvailableConfigureInfomation(dataId, group, 60000);
        String expected = content + Constants.DIAMOND_LINE_SEPARATOR + "test update new";
        Assert.assertEquals(expected, newReceivedConfigInfo);
    }


    @Test
    public void testPublishRemove() throws Exception {
        publisher.close();
        subscriber.close();
        String dataId = UUID.randomUUID().toString();
        String group = "leiwen";
        String content = "test publish remove";
        publisher.addDataId(dataId, group, content);
        publisher.start();
        subscriber.addDataId(dataId, group);
        subscriber.start();
        publisher.publishNew(dataId, group, content);
        Thread.sleep(1000);

        publisher.publishRemove(dataId, group, content);
        Thread.sleep(1000);

        String receivedConfig = subscriber.getAvailableConfigureInfomation(dataId, group, 60000);
        Assert.assertEquals(null, receivedConfig);
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
