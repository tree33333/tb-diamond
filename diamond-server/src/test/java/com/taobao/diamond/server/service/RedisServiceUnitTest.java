package com.taobao.diamond.server.service;

import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RedisServiceUnitTest {

    private RedisService redisService;
    
    @Before
    public void setUp() throws Exception {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("application.xml");
        redisService = (RedisService)ctx.getBean("redisService");
    }
    
    @After
    public void tearDown() throws Exception {
        redisService.close();
    }
    
    @Test
    public void testAddOneValue() throws Exception {
        String key = "key-" + UUID.randomUUID().toString();
        String value = "value";
        redisService.add(key, value);
        Assert.assertTrue(redisService.get(key).contains(value));
        redisService.remove(key, value);
        Assert.assertTrue(redisService.get(key).size() == 0);
    }
    
    @Test
    public void testAddDiffValues() throws Exception {
        String key = "key-" + UUID.randomUUID().toString();
        for(int i=0; i<10; i++) {
            redisService.add(key, "value" + i);
        }
        Assert.assertTrue(redisService.get(key).size() == 10);
        for(int i=0; i<5; i++) {
            redisService.remove(key, "value" + i);
        }
        Assert.assertTrue(redisService.get(key).size() == 5);
        for(int i=5; i<10; i++) {
            redisService.remove(key, "value" + i);
        }
        Assert.assertTrue(redisService.get(key).size() == 0);
    }
    
    @Test
    public void testAddSameValues() throws Exception {
        String key = "key-" + UUID.randomUUID().toString();
        for(int i=0; i<5; i++) {
            redisService.add(key, "value");
        }
        Assert.assertTrue(redisService.get(key).size() == 1);
        redisService.remove(key, "value");
        Assert.assertTrue(redisService.get(key).size() == 0);
    }
    
}
