package com.taobao.diamond.server.service;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class ACLServiceUnitTest {

    private ACLService aclService;
    
    @Before
    public void setUp() throws Exception {
        aclService = new ACLService();
    }
    
    
    @Test
    public void test() {
        Assert.assertTrue(this.aclService.check("192.168.0.1"));
        Assert.assertFalse(this.aclService.check("192.168.0.*"));
        Assert.assertFalse(this.aclService.check(null));
    }
}
