/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.configinfo;

public class PublishCacheData {
    private String dataId;
    private String group;
    private String configInfo;


    public PublishCacheData(String dataId, String group, String configInfo) {
        this.dataId = dataId;
        this.group = group;
        this.configInfo = configInfo;
    }


    public String getDataId() {
        return dataId;
    }


    public void setDataId(String dataId) {
        this.dataId = dataId;
    }


    public String getGroup() {
        return group;
    }


    public void setGroup(String group) {
        this.group = group;
    }


    public String getConfigInfo() {
        return configInfo;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configInfo == null) ? 0 : configInfo.hashCode());
        result = prime * result + ((dataId == null) ? 0 : dataId.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PublishCacheData other = (PublishCacheData) obj;
        if (configInfo == null) {
            if (other.configInfo != null)
                return false;
        }
        else if (!configInfo.equals(other.configInfo))
            return false;
        if (dataId == null) {
            if (other.dataId != null)
                return false;
        }
        else if (!dataId.equals(other.dataId))
            return false;
        if (group == null) {
            if (other.group != null)
                return false;
        }
        else if (!group.equals(other.group))
            return false;
        return true;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataId: ").append(dataId);
        sb.append(", Group: ").append(group);
        sb.append(", ConfigInfo: ").append(configInfo);
        return sb.toString();
    }
}
