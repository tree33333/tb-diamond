package com.taobao.diamond.server.utils;

import junit.framework.Assert;

import org.junit.Test;


public class HSFDataIdUtilsUnitTest {

    @Test
    public void c2dMappingTest() throws Exception {
        String addrDataId = "com.taobao.diamond.test";
        String modifiedAddrDataId = HSFDataIdUtils.c2dDataIdMapping(addrDataId, true);
        Assert.assertEquals(addrDataId + ".ADDRESS", modifiedAddrDataId);
        
        String routDataId = "com.taobao.diamond.test.ROUTING.RULE";
        String modifiedRoutDataId = HSFDataIdUtils.c2dDataIdMapping(routDataId, false);
        Assert.assertEquals("com.taobao.diamond.test.ROUTINGRULE", modifiedRoutDataId);
    }


    @Test
    public void d2cMappingTest() throws Exception {
        String addrDataId = "com.taobao.diamond.test.ADDRESS";
        String modifiedAddrDataId = HSFDataIdUtils.d2cDataIdMapping(addrDataId);
        Assert.assertEquals("com.taobao.diamond.test", modifiedAddrDataId);
        
        String routDataId = "com.taobao.diamond.test.ROUTINGRULE";
        String modifiedRoutDataId = HSFDataIdUtils.d2cDataIdMapping(routDataId);
        Assert.assertEquals("com.taobao.diamond.test.ROUTING.RULE", modifiedRoutDataId);
    }

}
