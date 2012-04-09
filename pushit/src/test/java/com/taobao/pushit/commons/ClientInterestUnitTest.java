package com.taobao.pushit.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


public class ClientInterestUnitTest {

    @Test
    public void testlength() {
        ClientInterest clientInterest = new ClientInterest(null, null);
        assertEquals(0, clientInterest.length());
        clientInterest = new ClientInterest("dataId", null);
        assertEquals(6, clientInterest.length());
        clientInterest = new ClientInterest("dataId", "group");
        assertEquals(11, clientInterest.length());
        clientInterest = new ClientInterest("", "group");
        assertEquals(5, clientInterest.length());
    }


    @Test
    public void testToString() {
        ClientInterest clientInterest = new ClientInterest(null, null);
        assertNull(clientInterest.toString());
        clientInterest = new ClientInterest("dataId", null);
        assertEquals("dataId", clientInterest.toString());
        clientInterest = new ClientInterest("dataId", "group");
        assertEquals("dataId,group", clientInterest.toString());
        clientInterest = new ClientInterest("", "group");
        assertEquals(",group", clientInterest.toString());
    }
}
