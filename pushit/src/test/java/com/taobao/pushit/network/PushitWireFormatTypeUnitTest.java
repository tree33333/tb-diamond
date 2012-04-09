package com.taobao.pushit.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.core.CodecFactory.Decoder;
import com.taobao.gecko.core.core.CodecFactory.Encoder;
import com.taobao.pushit.commons.ClientInterest;
import com.taobao.pushit.exception.PushitCodecException;


public class PushitWireFormatTypeUnitTest {

    private PushitWireFormatType formatType;
    private Encoder encoder;
    private Decoder decoder;


    @Before
    public void setUp() {
        formatType = new PushitWireFormatType();
        encoder = formatType.newCodecFactory().getEncoder();
        decoder = formatType.newCodecFactory().getDecoder();
    }


    @Test
    public void testEncodeDecodeNotifyCommand() {
        final NotifyCommand message = new NotifyCommand("dataId", "test-grp", "just for love");
        IoBuffer buf = encoder.encode(message, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("notify dataId test-grp just\10for\10love\r\n", new String(buf.array()));
        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(message, decodeCmd);
        assertEquals(message, decodeCmd);
    }


    @Test
    public void testEncodeDecodeNotifyCommand_WithoutMessage() {
        final NotifyCommand message = new NotifyCommand("dataId", "test-grp", null);
        IoBuffer buf = encoder.encode(message, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("notify dataId test-grp\r\n", new String(buf.array()));
        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(message, decodeCmd);
        assertEquals(message, decodeCmd);
    }


    @Test
    public void testEncodeDecodeBroadcastCommand() {
        final BroadCastCommand message = new BroadCastCommand("dataId", "test-grp", "just for love");
        IoBuffer buf = encoder.encode(message, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("broadcast dataId test-grp just\10for\10love\r\n", new String(buf.array()));
        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(message, decodeCmd);
        assertEquals(message, decodeCmd);
    }


    @Test
    public void testEncodeDecodeInterestCommand_one() {
        List<ClientInterest> clientInterests = new ArrayList<ClientInterest>();
        clientInterests.add(new ClientInterest("data1", "group1"));
        InterestCommand interestCommand = new InterestCommand(clientInterests);
        IoBuffer buf = encoder.encode(interestCommand, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("interest data1,group1\r\n", new String(buf.array()));
        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(interestCommand, decodeCmd);
        assertEquals(interestCommand, decodeCmd);
    }


    @Test
    public void testEncodeDecodeHeartBeatCommand() {
        HeartBeatCommand heartBeatCommand = new HeartBeatCommand(-1);
        IoBuffer buf = encoder.encode(heartBeatCommand, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("heartbeat -1\r\n", new String(buf.array()));

        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(heartBeatCommand, decodeCmd);
        assertEquals(heartBeatCommand, decodeCmd);
    }


    @Test
    public void testEncodeDecodeAckCommand_NoError() {
        AckCommand ack = new AckCommand(null, -1);
        IoBuffer buf = encoder.encode(ack, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("ack -1\r\n", new String(buf.array()));

        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(ack, decodeCmd);
        assertEquals(ack, decodeCmd);
    }

    @Test
    public void testEncodeDecodeAckCommand_HasError() {
        AckCommand ack = new AckCommand("fuck error", -1);
        IoBuffer buf = encoder.encode(ack, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("ack fuck\10error -1\r\n", new String(buf.array()));

        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(ack, decodeCmd);
        assertEquals(ack, decodeCmd);
    }
    
    @Test
    public void testEncodeDecodeAckCommand_HasResponseStatus() {
        AckCommand ack = new AckCommand(null, -1);
        ack.setResponseStatus(ResponseStatus.THREADPOOL_BUSY);
        IoBuffer buf = encoder.encode(ack, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("ack Thread\10pool\10is\10busy -1\r\n", new String(buf.array()));

        final ResponseCommand decodeCmd =(ResponseCommand) decoder.decode(buf, null);
        decodeCmd.setResponseStatus(ResponseStatus.THREADPOOL_BUSY);
        assertNotSame(ack, decodeCmd);
        assertEquals(ack, decodeCmd);
    }



    @Test
    public void testEncodeDecodeInterestCommand_more() {
        List<ClientInterest> clientInterests = new ArrayList<ClientInterest>();
        clientInterests.add(new ClientInterest("data1", "group1"));
        clientInterests.add(new ClientInterest("data2", "group2"));
        clientInterests.add(new ClientInterest("data3", "group2"));
        InterestCommand interestCommand = new InterestCommand(clientInterests);
        IoBuffer buf = encoder.encode(interestCommand, null);
        assertEquals(0, buf.position());
        assertTrue(buf.hasRemaining());
        assertEquals("interest data1,group1 data2,group2 data3,group2\r\n", new String(buf.array()));
        final Object decodeCmd = decoder.decode(buf, null);
        assertNotSame(interestCommand, decodeCmd);
        assertEquals(interestCommand, decodeCmd);
    }


    @Test
    public void testDecodeNotLine() {
        IoBuffer buf = IoBuffer.wrap("notify dataId test\r".getBytes());
        assertEquals(0, buf.position());
        decoder.decode(buf, null);
        assertEquals(0, buf.position());
    }


    @Test(expected = PushitCodecException.class)
    public void testDecodeInvalidNotifyCommand() {
        IoBuffer buf = IoBuffer.wrap("notify 1 2 3 4 5\r\n".getBytes());
        decoder.decode(buf, null);
    }


    @Test(expected = PushitCodecException.class)
    public void testDecodeUnsupportedCommand() {
        IoBuffer buf = IoBuffer.wrap("version\r\n".getBytes());
        decoder.decode(buf, null);
    }
}
