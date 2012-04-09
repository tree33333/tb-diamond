/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.network;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.util.ByteBufferMatcher;
import com.taobao.gecko.core.util.OpaqueGenerator;
import com.taobao.gecko.core.util.ShiftAndByteBufferMatcher;
import com.taobao.gecko.service.config.WireFormatType;
import com.taobao.pushit.commons.ClientInterest;
import com.taobao.pushit.commons.Constants;
import com.taobao.pushit.exception.PushitCodecException;


public class PushitWireFormatType extends WireFormatType {

    @Override
    public String getScheme() {
        return "pushit";
    }


    @Override
    public String name() {
        return "pushit";
    }

    static final ByteBufferMatcher LINE_MATCHER = new ShiftAndByteBufferMatcher(IoBuffer.wrap(ByteUtils.CRLF));
    static final Pattern SPLITER = Pattern.compile(" ");
    static final Pattern INTEREST_SPLITER = Pattern.compile(",");

    /**
     * 用于message中空格的替换
     */
    public static final String BLANK_REPLACE = "\10";

    static final Log log = LogFactory.getLog(PushitWireFormatType.class);


    @Override
    public CodecFactory newCodecFactory() {
        return new CodecFactory() {

            public Decoder getDecoder() {
                return new Decoder() {

                    public Object decode(IoBuffer buff, Session session) {
                        if (buff == null || !buff.hasRemaining()) {
                            return null;
                        }
                        buff.mark();
                        int index = LINE_MATCHER.matchFirst(buff);
                        if (index >= 0) {
                            byte[] bytes = new byte[index - buff.position()];
                            buff.get(bytes);
                            // 跳过\r\n
                            buff.position(buff.position() + 2);
                            String line = new String(bytes);
                            if (log.isDebugEnabled()) {
                                log.debug("Receive command:" + line);
                            }
                            String[] tmps = SPLITER.split(line);
                            if (tmps == null || tmps.length == 0) {
                                throw new PushitCodecException("Unknow command:" + line);
                            }
                            char first = tmps[0].charAt(0);
                            switch (first) {
                            case 'n':
                                return decodeNotify(line, tmps);
                            case 'b':
                                return decodeBroadcast(line, tmps);
                            case 'i':
                                return decodeInterest(tmps);
                            case 'h':
                                return decodeHeartBeat(tmps);
                            case 'a':
                                return decodeAck(line, tmps);
                            default:
                                throw new PushitCodecException("Unsupported command:" + line);
                            }

                        }
                        else {
                            return null;
                        }
                    }


                    private Object decodeAck(String line, String[] tmps) {
                        assertCommand(tmps[0], "ack");
                        // 成功应答，没有error msg
                        if (tmps.length == 2) {
                            Integer opaque = Integer.parseInt(tmps[1]);
                            return new AckCommand(null, opaque);
                        }
                        else if (tmps.length == 3) {
                            // 有error msg
                            String errorMsg = tmps[1];
                            // 替换\10为空格
                            errorMsg = errorMsg.replaceAll(BLANK_REPLACE, " ");
                            return new AckCommand(errorMsg, Integer.parseInt(tmps[2]));
                        }
                        else {
                            throw new PushitCodecException("Unsupported command:" + line);
                        }
                    }


                    private Object decodeHeartBeat(String[] tmps) {
                        assertCommand(tmps[0], "heartbeat");
                        Integer opaque = Integer.parseInt(tmps[1]);
                        return new HeartBeatCommand(opaque);
                    }


                    private Object decodeInterest(String[] tmps) {
                        assertCommand(tmps[0], "interest");
                        List<ClientInterest> clientInterests = new ArrayList<ClientInterest>();
                        for (int i = 1; i < tmps.length; i++) {
                            String interestStr = tmps[i];
                            String[] dataIdAndGroup = INTEREST_SPLITER.split(interestStr);
                            switch (dataIdAndGroup.length) {
                            case 1:
                                // 分组为默认分组
                                clientInterests.add(new ClientInterest(dataIdAndGroup[0], Constants.DEFAULT_GROUP));
                                break;

                            case 2:
                                clientInterests.add(new ClientInterest(dataIdAndGroup[0], dataIdAndGroup[1]));
                                break;
                            default:
                                throw new PushitCodecException("Invalid client interest:" + interestStr);
                            }
                        }
                        return new InterestCommand(clientInterests);
                    }


                    private Object decodeBroadcast(String line, String[] tmps) {
                        assertCommand(tmps[0], "broadcast");
                        String msg = null;
                        if (tmps.length == 4) {
                            msg = tmps[3];
                            msg = msg.replaceAll(BLANK_REPLACE, " ");
                        }
                        if (tmps.length > 4 || tmps.length < 3) {
                            throw new PushitCodecException("Invalid broadcast command:" + line);
                        }
                        return new BroadCastCommand(tmps[1], tmps[2], msg);
                    }


                    private Object decodeNotify(String line, String[] tmps) {
                        assertCommand(tmps[0], "notify");
                        String msg = null;
                        if (tmps.length == 4) {
                            msg = tmps[3];
                            msg = msg.replaceAll(BLANK_REPLACE, " ");
                        }
                        if (tmps.length > 4 || tmps.length < 3) {
                            throw new PushitCodecException("Invalid notify command:" + line);
                        }
                        return new NotifyCommand(tmps[1], tmps[2], msg);
                    }
                };
            }


            private void assertCommand(String tmp, String cmd) {
                if (!tmp.equals(cmd)) {
                    throw new PushitCodecException("Unsupported command:" + tmp);
                }
            }


            public Encoder getEncoder() {
                return new Encoder() {
                    public IoBuffer encode(Object message, Session session) {
                        return ((PushitEncodeCommand) message).encode();
                    }
                };
            }

        };
    }


    @Override
    public CommandFactory newCommandFactory() {
        return new CommandFactory() {

            public HeartBeatRequestCommand createHeartBeatCommand() {
                return new HeartBeatCommand(OpaqueGenerator.getNextOpaque());
            }


            public BooleanAckCommand createBooleanAckCommand(CommandHeader request, ResponseStatus responseStatus,
                    String errorMsg) {
                AckCommand ackCommand = new AckCommand(errorMsg, request.getOpaque());
                ackCommand.setResponseStatus(responseStatus);
                return ackCommand;
            }
        };
    }

}
