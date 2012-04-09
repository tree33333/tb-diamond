package com.taobao.pushit.intergration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;
import com.taobao.pushit.commons.Constants;
import com.taobao.pushit.server.PushitBroker;


public class FunctionTest {

    static int port = 8609;


    public Properties getPropsForTest() {
        Properties props = new Properties();
        props.put("port", String.valueOf(port++));
        return props;
    }


    @Test(timeout = 10000)
    public void testOneBrokerOneClientOneInterests() throws Exception {
        Properties props = getPropsForTest();
        PushitBroker pushitBroker = new PushitBroker(props);
        pushitBroker.startup();
        try {
            final AtomicBoolean notified = new AtomicBoolean();
            final AtomicReference<String> id = new AtomicReference<String>();
            final AtomicReference<String> grp = new AtomicReference<String>();
            final AtomicReference<String> msg = new AtomicReference<String>();
            PushitClient ptClient = new PushitClient("localhost:" + props.getProperty("port"), new NotifyListener() {

                public void onNotify(String dataId, String group, String message) {
                    notified.set(true);
                    grp.set(group);
                    id.set(dataId);
                    msg.set(message);
                    System.out.println("Receive:" + dataId + " " + group + " " + message);
                }
            });
            ptClient.interest("test");
            Thread.sleep(1000);
            ptClient.push("test", "testOneBrokerOneClientOneInterests");
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            assertEquals("test", id.get());
            assertEquals(Constants.DEFAULT_GROUP, grp.get());
            assertEquals("testOneBrokerOneClientOneInterests", msg.get());
            ptClient.stop();
        }
        catch (Throwable t) {
            t.printStackTrace();
            fail();
            //t.printStackTrace();
        }
        finally {
            pushitBroker.stop();
        }
    }


    @Test(timeout = 10000)
    public void testOneBrokerTwoClientOneInterests() throws Exception {
        Properties props = getPropsForTest();
        PushitBroker pushitBroker = new PushitBroker(props);
        pushitBroker.startup();
        try {
            final AtomicBoolean notified = new AtomicBoolean();
            final AtomicReference<String> id = new AtomicReference<String>();
            final AtomicReference<String> grp = new AtomicReference<String>();
            final AtomicReference<String> msg = new AtomicReference<String>();
            PushitClient notifyClient =
                    new PushitClient("localhost:" + props.getProperty("port"), new NotifyListener() {

                        public void onNotify(String dataId, String group, String message) {
                            notified.set(true);
                            grp.set(group);
                            id.set(dataId);
                            msg.set(message);
                            System.out.println("Receive:" + dataId + " " + group + " " + message);
                        }
                    });
            notifyClient.interest("test");
            Thread.sleep(1000);
            PushitClient pushClient = new PushitClient("localhost:" + props.getProperty("port"));
            pushClient.push("test", "testOneBrokerTwoClientOneInterests");
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            assertEquals("test", id.get());
            assertEquals(Constants.DEFAULT_GROUP, grp.get());
            assertEquals("testOneBrokerTwoClientOneInterests", msg.get());
            notifyClient.stop();
            pushClient.stop();
        }
        catch (Throwable t) {
            fail();
            t.printStackTrace();
        }
        finally {
            pushitBroker.stop();
        }
    }


    @Test(timeout = 50000)
    public void testThreeBrokersMoreClients_MoreNotify() throws Exception {
        Properties props1 = getPropsForTest();
        Properties props2 = getPropsForTest();
        Properties props3 = getPropsForTest();
        // broker配置集群机器列表
        String hosts = "127.0.0.1:" + props1.getProperty("port") + ",127.0.0.1:"
                + props2.getProperty("port") + ",127.0.0.1:" + props3.getProperty("port");
        props1.put("cluster_hosts", hosts);
        props2.put("cluster_hosts", hosts);
        props3.put("cluster_hosts", hosts);
        PushitBroker pushitBroker1 = new PushitBroker(props1);
        pushitBroker1.startup();
        PushitBroker pushitBroker2 = new PushitBroker(props2);
        pushitBroker2.startup();
        PushitBroker pushitBroker3 = new PushitBroker(props3);
        pushitBroker3.startup();
        try {
            String dataId = "test";
            String group = "test-grp";
            final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<String>();
            List<PushitClient> clients = new ArrayList<PushitClient>();
            for (int i = 0; i < 10; i++) {
                PushitClient ptClient =
                        new PushitClient(hosts, new NotifyListener() {

                            public void onNotify(String dataId, String group, String message) {
                                messages.add(message);
                            }
                        });
                System.out.println("客户端连接到："+ptClient.getServerUrl());
                ptClient.interest(dataId, group);
                clients.add(ptClient);
            }
            // 等待interest被处理
            Thread.sleep(2000);

            PushitClient pushClient = new PushitClient("localhost:" + props2.getProperty("port"));
            pushClient.push(dataId, group, "testThreeBrokersMoreClients_MoreNotify");

            while (messages.size() < clients.size()) {
                Thread.sleep(1000);
            }
            for (String msg : messages) {
                assertEquals("testThreeBrokersMoreClients_MoreNotify", msg);
            }

            for (PushitClient client : clients) {
                client.stop();
            }
            pushClient.stop();

        }
        finally {
            pushitBroker1.stop();
            pushitBroker2.stop();
            pushitBroker3.stop();
        }
    }


   @Test
    public void testTwoBrokerTwoClientOneInterests() throws Exception {
        Properties props1 = getPropsForTest();
        Properties props2 = getPropsForTest();
        // broker2配置集群机器列表
        props2.put("cluster_hosts", "localhost:" + props1.getProperty("port"));
        PushitBroker pushitBroker1 = new PushitBroker(props1);
        pushitBroker1.startup();
        PushitBroker pushitBroker2 = new PushitBroker(props2);
        pushitBroker2.startup();
        try {
            final AtomicBoolean notified = new AtomicBoolean();
            final AtomicReference<String> id = new AtomicReference<String>();
            final AtomicReference<String> grp = new AtomicReference<String>();
            final AtomicReference<String> msg = new AtomicReference<String>();
            // 订阅者，订阅到broker1
            PushitClient notifyClient =
                    new PushitClient("localhost:" + props1.getProperty("port"), new NotifyListener() {

                        public void onNotify(String dataId, String group, String message) {
                            notified.set(true);
                            grp.set(group);
                            id.set(dataId);
                            msg.set(message);
                            System.out.println("Receive:" + dataId + " " + group + " " + message);
                        }
                    });
            notifyClient.interest("test");
            Thread.sleep(1000);
            // 发布者，发布到broker2
            PushitClient pushClient = new PushitClient("localhost:" + props2.getProperty("port"));
            pushClient.push("test", "testTwoBrokerTwoClientOneInterests");
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            assertEquals("test", id.get());
            assertEquals(Constants.DEFAULT_GROUP, grp.get());
            assertEquals("testTwoBrokerTwoClientOneInterests", msg.get());
            notifyClient.stop();
            pushClient.stop();
        }
        catch (Throwable t) {
            fail();
            t.printStackTrace();
        }
        finally {
            pushitBroker1.stop();
            pushitBroker2.stop();
        }
    }
}
