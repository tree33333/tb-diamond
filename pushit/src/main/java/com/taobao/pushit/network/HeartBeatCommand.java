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
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;


/**
 * ÐÄÌøÃüÁî
 * 
 * @author boyan
 * 
 */
public class HeartBeatCommand implements HeartBeatRequestCommand, PushitEncodeCommand {

    private final Integer opaque;


    public HeartBeatCommand(Integer opaque) {
        super();
        this.opaque = opaque;
    }


    public CommandHeader getRequestHeader() {
        return this;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (opaque == null ? 0 : opaque.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        HeartBeatCommand other = (HeartBeatCommand) obj;
        if (opaque == null) {
            if (other.opaque != null) {
                return false;
            }
        }
        else if (!opaque.equals(other.opaque)) {
            return false;
        }
        return true;
    }


    public Integer getOpaque() {
        return opaque;
    }


    public IoBuffer encode() {
        return IoBuffer.wrap(("heartbeat " + opaque + "\r\n").getBytes());
    }

}
