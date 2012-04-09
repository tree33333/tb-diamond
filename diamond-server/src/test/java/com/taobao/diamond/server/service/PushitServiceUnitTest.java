package com.taobao.diamond.server.service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;


public class PushitServiceUnitTest {

    private PushitService pushitService;

    private PushitClient client;


    @Before
    public void setUp() throws Exception {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] { "application.xml" });
        pushitService = (PushitService) ctx.getBean("pushitService");

    }
    
    @After
    public void tearDown() throws Exception {
        client.stop();
    }


    @Test
    public void testPushAndInterest() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String group = "test_pushit";
        final AtomicBoolean notified = new AtomicBoolean(false);
        final AtomicReference<String> id = new AtomicReference<String>();
        final AtomicReference<String> grp = new AtomicReference<String>();
        client = new PushitClient(pushitService.getPushitServers(), new NotifyListener() {

            public void onNotify(String dataId, String group, String message) {
                notified.set(true);
                id.set(dataId);
                grp.set(group);
                System.out.println("receive notification:" + message);
            }

        });
        client.interest(dataId, group);
        Thread.sleep(1000);
        pushitService.pushNotification(dataId, group, "test");

        while (!notified.get()) {
            Thread.sleep(1000);
        }

        Assert.assertEquals(dataId, id.get());
        Assert.assertEquals(group, grp.get());
    }

}
