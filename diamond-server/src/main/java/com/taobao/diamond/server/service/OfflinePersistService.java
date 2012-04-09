/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service;

import java.sql.Timestamp;
import java.util.List;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;


public class OfflinePersistService implements PersistService {

    private static final String UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG = "离线模式下不支持的操作";


    public void addConfigInfo(ConfigInfo configInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void addConfigInfo(String srcIp, String srcUser, Timestamp time, ConfigInfo configInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void addGroupInfo(GroupInfo groupInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);

    }


    public void addGroupInfo(String srcIp, String srcUser, Timestamp time, GroupInfo groupInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void removeConfigInfo(String dataId, String group) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);

    }


    public void removeGroupInfoByID(long id) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void removeConfigInfo(ConfigInfo configInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);

    }


    public void removeConfigInfoByID(long id) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void updateGroup(long id, String newGroup) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void updateGroup(long id, String srcIp, String srcUser, Timestamp time, String newGroup) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void updateConfigInfo(ConfigInfo configInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public void updateConfigInfo(String srcIp, String srcUser, Timestamp time, ConfigInfo configInfo) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public int updateConfigInfoByMd5(ConfigInfoEx configInfoEx) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public int updateConfigInfoByMd5(String srcIp, String srcUser, Timestamp time, ConfigInfoEx configInfoEx) {
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public ConfigInfo findConfigInfo(String dataId, String group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public ConfigInfo findConfigInfoByID(long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public GroupInfo findGroupInfoByID(long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public GroupInfo findGroupInfoByAddressDataId(String address, String dataId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public List<GroupInfo> findGroupInfoByGroup(String group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public List<GroupInfo> findAllGroupInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public Page<ConfigInfo> findConfigInfoByGroup(int pageNo, int pageSize, String group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public Page<ConfigInfo> findAllConfigInfo(int pageNo, int pageSize) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public Page<ConfigInfo> findConfigInfoByDataId(int pageNo, int pageSize, String dataId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public Page<ConfigInfo> findConfigInfoLike(int pageNo, int pageSize, String dataId, String group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }


    public int countAllDataIds() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(UNSUPPORTED_OP_UNDER_OFFLINE_MODE_MSG);
    }

}
