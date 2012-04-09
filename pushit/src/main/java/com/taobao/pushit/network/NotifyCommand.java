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
import com.taobao.gecko.core.command.RequestCommand;




/**
 * notify协议，用于服务器通知客户端: notify dataId group [message]\r\n
 * 
 * @author boyan
 * @Date 2011-5-19
 * 
 */
public class NotifyCommand implements RequestCommand, PushitEncodeCommand {
    static final long serialVersionUID = -1L;

    protected final String dataId;

    protected final String group;

    protected String message;

    private boolean useLocalCache;

    private final static ThreadLocal<IoBuffer> LOCAL_BUF = new ThreadLocal<IoBuffer>();


    public static void resetLocalBuf() {
        LOCAL_BUF.remove();
    }


    public void setUseLocalCache(boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }


    public String getMessage() {
        return message;
    }


    public String getDataId() {
        return dataId;
    }


    public String getGroup() {
        return group;
    }


    public NotifyCommand(String dataId, String group, String message) {
        super();
        this.dataId = dataId;
        this.group = group;
        this.message = message;
    }


    public Integer getOpaque() {
        return 0;
    }


    public IoBuffer encode() {
        if (useLocalCache) {
            IoBuffer buf = LOCAL_BUF.get();
            if (buf != null) {
                return buf.slice();
            }
            else {
                buf = encodeBuf();
                LOCAL_BUF.set(buf);
                return buf.slice();
            }
        }
        else {
            return encodeBuf();
        }
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (dataId == null ? 0 : dataId.hashCode());
        result = prime * result + (group == null ? 0 : group.hashCode());
        result = prime * result + (message == null ? 0 : message.hashCode());
        result = prime * result + (useLocalCache ? 1231 : 1237);
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
        NotifyCommand other = (NotifyCommand) obj;
        if (dataId == null) {
            if (other.dataId != null) {
                return false;
            }
        }
        else if (!dataId.equals(other.dataId)) {
            return false;
        }
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        }
        else if (!group.equals(other.group)) {
            return false;
        }
        if (message == null) {
            if (other.message != null) {
                return false;
            }
        }
        else if (!message.equals(other.message)) {
            return false;
        }
        if (useLocalCache != other.useLocalCache) {
            return false;
        }
        return true;
    }


    private IoBuffer encodeBuf() {
        String msg = null;
        if (message != null) {
            // 将空格替换为\10
            msg = message.replaceAll(" ", PushitWireFormatType.BLANK_REPLACE);
        }
        int msgLen = msg != null ? msg.length() : 0;
        IoBuffer buf =
                IoBuffer.allocate((msgLen == 0 ? 10 : 11) + getDataId().length() + getGroup().length()
                        + msgLen);
        ByteUtils.setArguments(buf, "notify", getDataId(), getGroup(), msg);
        buf.flip();
        return buf;
    }


    public CommandHeader getRequestHeader() {
        return this;
    }

}
