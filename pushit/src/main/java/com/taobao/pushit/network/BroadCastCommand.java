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

import com.taobao.gecko.core.buffer.IoBuffer;



/**
 * 用于pushit集群内的服务器广播协议</br>： broadcast dataId group [message]\r\b;
 * 
 * @author boyan
 * @Date 2011-5-25
 * 
 */
public class BroadCastCommand extends NotifyCommand {

    /**
     * 
     */
    private static final long serialVersionUID = -7117868992493212243L;


    public BroadCastCommand(String dataId, String group, String message) {
        super(dataId, group, message);
    }


    @Override
    public IoBuffer encode() {
        String msg = null;
        if (message != null) {
            // 将空格替换为\10
            msg = message.replaceAll(" ", PushitWireFormatType.BLANK_REPLACE);
        }
        int msgLen = msg != null ? msg.length() : 0;
        IoBuffer buf = IoBuffer.allocate(14 + getDataId().length() + getGroup().length() + msgLen);
        ByteUtils.setArguments(buf, "broadcast", getDataId(), getGroup(), msg);
        buf.flip();
        return buf;
    }

}
