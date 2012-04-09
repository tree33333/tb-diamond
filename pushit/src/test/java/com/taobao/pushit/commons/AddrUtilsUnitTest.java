package com.taobao.pushit.commons;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class AddrUtilsUnitTest {

    @Test
    public void testGetUrlFromHost() {
        assertEquals("pushit://localhost:8609", AddrUtils.getUrlFromHost("localhost"));
        assertEquals("pushit://192.168.1.1:8609", AddrUtils.getUrlFromHost("192.168.1.1"));
        assertEquals("pushit://127.0.0.1:8123", AddrUtils.getUrlFromHost("localhost:8123"));
        assertEquals("pushit://192.168.1.1:9999", AddrUtils.getUrlFromHost("192.168.1.1:9999"));
    }
}
