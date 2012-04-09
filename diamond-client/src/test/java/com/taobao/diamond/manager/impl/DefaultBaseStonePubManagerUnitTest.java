package com.taobao.diamond.manager.impl;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.impl.DiamondClientFactory;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.manager.BaseStonePubManager;
import com.taobao.diamond.manager.BaseStoneSubManager;
import com.taobao.diamond.manager.ManagerListener;


public class DefaultBaseStonePubManagerUnitTest {
    // diamond server address (ip)
    private static final String DIAMOND_SERVER_ADDR = "";
    // pushit server address (ip:port)
    private static final String PUSHIT_SERVER_ADDR = "";

    private static final String GROUP = "BS_PUB_TEST";

    private BaseStonePubManager pubManager;
    private BaseStoneSubManager subManager;
    private DiamondPublisher publisher;
    private DiamondSubscriber subscriber;

    private final AtomicBoolean notified = new AtomicBoolean();
    private final AtomicReference<String> info = new AtomicReference<String>();

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


    /**
     * 测试发布增量数据, 发一次, dataId不存在, 使用默认的ContentIdentityPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublish1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            // protocal://ip:port?k=v
            String content = "";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(content);

            while (!notified.get()) {
                Thread.sleep(1000);
            }

            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试发布增量数据, 前后两次发布数据的唯一标识不同, 使用默认的ContentIdentityPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublish2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            // protocal://ip:port?k=v
            String content = "";
            // protocal://ip:port?k=v
            String newContent = "";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });

            pubManager.publish(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(newContent);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            String expected = content + Constants.DIAMOND_LINE_SEPARATOR + newContent;
            Assert.assertEquals(expected, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试发布增量数据, 前后两次发布数据的唯一标识相同, 使用默认的ContentIdentityPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublish3() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            // protocal://ip:port?k=v
            String content = "";
            // protocal://ip:port?k=v
            String newContent = "";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });

            pubManager.publish(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(newContent);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试自定义的ContentIdentifyPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublishByPattern1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is a pattern test content";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }

            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试自定义的ContentIdentifyPattern, 前后两次发布数据的标识不同
     * 
     * @throws Exception
     */
    @Test
    public void testPublishByPattern2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is a pattern test content";
            String newContent = "that is a pattern test content";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(dataId, group, newContent, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            String expected = content + Constants.DIAMOND_LINE_SEPARATOR + newContent;
            Assert.assertEquals(expected, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试自定义的ContentIdentifyPattern, 前后两次发布数据的标识相同
     * 
     * @throws Exception
     */
    @Test
    public void testPublishByPattern3() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is a pattern test content";
            String newContent = "this is a new pattern test content";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(dataId, group, newContent, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试发布全量数据, dataId不存在
     * 
     * @throws Exception
     */
    @Test
    public void testPublishAll1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is publish all test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publishAll(content);

            while (!notified.get()) {
                Thread.sleep(1000);
            }

            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }

    }


    /**
     * 测试发布全量数据, dataId存在
     * 
     * @throws Exception
     */
    @Test
    public void testPublishAll2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is publish all test";
            String newContent = "this is another publish all test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publishAll(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publishAll(newContent);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }

    }


    /**
     * 测试删除数据, 发布一条, 删除一条, 删除后dataId也被删除
     * 
     * @throws Exception
     */
    @Test
    public void testUnpublish1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is unpublish test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publishAll(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.unpublish(content);

            // 因为获取数据为null时不会调用监听器, 所以这里只能等待15秒, 使TTL Cache失效
            Thread.sleep(15000);

            String receivedConfigInfo = subManager.getAvailableConfigureInfomation(60000);
            Assert.assertEquals(null, receivedConfigInfo);
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 测试删除数据, 发布两条, 删除一条
     * 
     * @throws Exception
     */
    @Test
    public void testUnpublish2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is unpublish test";
            String newContent = "that is unpublish test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(dataId, group, newContent, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content + Constants.DIAMOND_LINE_SEPARATOR + newContent, info.get());
            notified.set(false);
            info.set(null);

            pubManager.unpublish(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * 订阅的数据不存在 ——> 发布 ——> 订阅成功
     * 
     * @throws Exception
     */
    @Test
    public void testSubAndPub() throws Exception {
        try {
            publisher.close();
            subscriber.close();

            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is func test";

            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("接收到配置信息:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            // 确认订阅的数据不存在
            Assert.assertFalse(subManager.exists(dataId, group));

            // 等待数据
            int count = 0;
            while (!notified.get() && count++ < 10) {
                Thread.sleep(500);
            }
            Assert.assertFalse(content.equals(info.get()));

            // 发布
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());

            // 等待数据
            count = 0;
            while (!notified.get() && count++ < 10) {
                Thread.sleep(1000);
            }

            // 成功订阅到数据
            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
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

    private class CustomContentIdentityPattern implements ContentIdentityPattern {

        public String getContentIdentity(String content) {
            // 以内容的前四个字符作为唯一标识
            return content.substring(0, 4);
        }

    }

}
