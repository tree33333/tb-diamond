package com.taobao.diamond.client.impl;

import junit.framework.Assert;

import org.junit.Test;


public class URLContentIdentityPatternUnitTest {

    @Test
    public void testNotifyNormalContent() {
        URLContentIdentityPattern pattern = new URLContentIdentityPattern();
        // protocal://ip:port
        String content = "";
        String id = pattern.getContentIdentity(content);
        Assert.assertEquals(content, id);

        // protocal://ip:port?k=v
        content = "";
        id = pattern.getContentIdentity(content);
        // protocal://ip:port
        Assert.assertEquals("", id);
    }


    @Test
    public void testHsfNormalContent() {
        URLContentIdentityPattern pattern = new URLContentIdentityPattern();
        // ip:port?k=v
        String content = "";
        String id = pattern.getContentIdentity(content);
        // ip:port
        Assert.assertEquals("", id);
    }


    @Test
    public void testTaeNormalContent() {
        URLContentIdentityPattern pattern = new URLContentIdentityPattern();
        // ip:
        String content = "";
        String id = pattern.getContentIdentity(content);
        // ip:
        Assert.assertEquals("", id);

        // ip:port
        content = "";
        id = pattern.getContentIdentity(content);
        // ip:port
        Assert.assertEquals("", id);

        // ip:port?k=v
        content = "";
        id = pattern.getContentIdentity(content);
        // ip:port
        Assert.assertEquals("", id);
    }


    @Test
    public void testInvalid() {
        URLContentIdentityPattern pattern = new URLContentIdentityPattern();
        String content = "test";
        try {
            pattern.getContentIdentity(content);
            Assert.fail("Î´Å×³öÒì³£");
        }
        catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

    }
}
