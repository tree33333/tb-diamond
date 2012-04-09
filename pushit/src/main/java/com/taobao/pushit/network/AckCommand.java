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
import com.taobao.gecko.core.command.AbstractResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;


/**
 * 应答协议,ack errormsg opaque\r\n
 * 
 * 如果成功，则errormsg为null
 * 
 * @author boyan
 * 
 */
public class AckCommand extends AbstractResponseCommand implements BooleanAckCommand, PushitEncodeCommand {

    private String errorMsg;


    public AckCommand(String errorMsg, Integer opaque) {
        super();
        this.errorMsg = errorMsg;
        this.opaque = opaque;
        if (this.errorMsg == null) {
            responseStatus = ResponseStatus.NO_ERROR;
        }
        else {
            responseStatus = ResponseStatus.ERROR;
        }
    }


    public String getErrorMsg() {
        if (errorMsg == null && responseStatus != null) {
            return responseStatus.getErrorMessage();
        }
        else {
            return errorMsg;
        }
    }


    public void setErrorMsg(String arg0) {
        errorMsg = arg0;

    }

    private static final byte[] ACK_CMD = { 'a', 'c', 'k' };


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (errorMsg == null ? 0 : errorMsg.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AckCommand other = (AckCommand) obj;
        if (errorMsg == null) {
            if (other.errorMsg != null) {
                return false;
            }
        }
        else if (!errorMsg.equals(other.errorMsg)) {
            return false;
        }
        return true;
    }


    public IoBuffer encode() {

        if (responseStatus != null && responseStatus != ResponseStatus.NO_ERROR && errorMsg == null) {
            errorMsg = responseStatus.getErrorMessage();
        }

        String errorInfo = null;
        // 替换空格为\10
        if (errorMsg != null) {
            errorInfo = errorMsg.replaceAll(" ", PushitWireFormatType.BLANK_REPLACE);
        }

        byte[] errorMsgData = ByteUtils.getBytes(errorInfo);
        int errorMsgLen = errorMsgData != null ? errorMsgData.length : 0;
        int capacity = ACK_CMD.length + errorMsgLen + 1 + ByteUtils.stringSize(opaque) + 2;
        if (errorMsgLen > 0) {
            capacity += 1;
        }
        IoBuffer buffer = IoBuffer.allocate(capacity);
        ByteUtils.setArguments(buffer, ACK_CMD, errorMsgData, opaque);
        buffer.flip();
        return buffer;
    }

}
