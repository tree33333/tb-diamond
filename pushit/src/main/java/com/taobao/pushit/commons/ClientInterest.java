/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.commons;

/**
 * 客户端感兴趣的group和dataId
 * 
 * @author boyan
 * @Date 2011-5-25
 * 
 */
public final class ClientInterest {
    public final String group;
    public final String dataId;


    public ClientInterest(String dataId, String group) {
        super();
        this.group = group;
        this.dataId = dataId;
    }


    public int length() {
        return this.length(this.group) + this.length(this.dataId);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.dataId == null ? 0 : this.dataId.hashCode());
        result = prime * result + (this.group == null ? 0 : this.group.hashCode());
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
        ClientInterest other = (ClientInterest) obj;
        if (this.dataId == null) {
            if (other.dataId != null) {
                return false;
            }
        }
        else if (!this.dataId.equals(other.dataId)) {
            return false;
        }
        if (this.group == null) {
            if (other.group != null) {
                return false;
            }
        }
        else if (!this.group.equals(other.group)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        if (this.group == null) {
            return this.dataId;
        }
        else {
            return this.dataId + "," + this.group;
        }
    }


    private int length(String s) {
        return s != null ? s.length() : 0;
    }
}
