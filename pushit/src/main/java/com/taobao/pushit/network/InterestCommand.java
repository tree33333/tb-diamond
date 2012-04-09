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

import java.util.List;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.pushit.commons.ClientInterest;



/**
 * 握手协议，客户端告知服务端自己感兴趣的dataId和Group，协议如下：</br> interest dataId,group dataId,group
 * ... \r\n
 * 
 * @author boyan
 * @Date 2011-5-25
 * 
 */
public class InterestCommand implements RequestCommand,  PushitEncodeCommand {
    /**
     * 
     */
    private static final long serialVersionUID = 8275752760321708042L;

    private final List<ClientInterest> clientInterests;


    public List<ClientInterest> getClientInterests() {
        return clientInterests;
    }


    public InterestCommand(List<ClientInterest> clientInterests) {
        super();
        this.clientInterests = clientInterests;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (clientInterests == null ? 0 : clientInterests.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        InterestCommand other = (InterestCommand) obj;
        if (clientInterests == null) {
            if (other.clientInterests != null) {
                return false;
            }
        }
        else if (!clientInterests.equals(other.clientInterests)) {
            return false;
        }
        return true;
    }


    public Integer getOpaque() {
        return 0;
    }


    public CommandHeader getRequestHeader() {
        return this;
    }


    public IoBuffer encode() {
        int interestLength = 0;
        for (ClientInterest interest : clientInterests) {
            interestLength += 2 + interest.length();
        }
        IoBuffer buf = IoBuffer.allocate(10 + interestLength);
        buf.put(ByteUtils.getBytes("interest"));
        buf.put(ByteUtils.SPACE);
        boolean wasFirst = true;
        for (ClientInterest o : clientInterests) {
            if (o == null) {
                continue;
            }
            if (wasFirst) {
                wasFirst = false;
            }
            else {
                buf.put(ByteUtils.SPACE);
            }
            buf.put(ByteUtils.getBytes(o.toString()));
        }
        buf.put(ByteUtils.CRLF);
        buf.flip();
        return buf;
    }
}
