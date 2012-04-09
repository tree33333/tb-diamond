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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;


public class HangingModePersistService implements PersistService {

    public void addConfigInfo(ConfigInfo configInfo) {
        // TODO Auto-generated method stub

    }


    public void addConfigInfo(String srcIp, String srcUser, Timestamp time, ConfigInfo configInfo) {
        // TODO Auto-generated method stub

    }


    public void addGroupInfo(GroupInfo groupInfo) {
        // TODO Auto-generated method stub

    }


    public void addGroupInfo(String srcIp, String srcUser, Timestamp time, GroupInfo groupInfo) {
        // TODO Auto-generated method stub

    }


    public void removeConfigInfo(String dataId, String group) {
        // TODO Auto-generated method stub

    }


    public void removeGroupInfoByID(long id) {
        // TODO Auto-generated method stub

    }


    public void removeConfigInfo(ConfigInfo configInfo) {
        // TODO Auto-generated method stub

    }


    public void removeConfigInfoByID(long id) {
        // TODO Auto-generated method stub

    }


    public void updateGroup(long id, String newGroup) {
        // TODO Auto-generated method stub

    }


    public void updateGroup(long id, String srcIp, String srcUser, Timestamp time, String newGroup) {
        // TODO Auto-generated method stub

    }


    public void updateConfigInfo(ConfigInfo configInfo) {
        // TODO Auto-generated method stub

    }


    public void updateConfigInfo(String srcIp, String srcUser, Timestamp time, ConfigInfo configInfo) {
        // TODO Auto-generated method stub

    }


    public int updateConfigInfoByMd5(ConfigInfoEx configInfoEx) {
        return 0;
    }


    public int updateConfigInfoByMd5(String srcIp, String srcUser, Timestamp time, ConfigInfoEx configInfoEx) {
        // TODO Auto-generated method stub
        return 0;
    }


    public ConfigInfo findConfigInfo(String dataId, String group) {
        // TODO Auto-generated method stub
        return null;
    }


    public ConfigInfo findConfigInfoByID(long id) {
        // TODO Auto-generated method stub
        return null;
    }


    public GroupInfo findGroupInfoByID(long id) {
        // TODO Auto-generated method stub
        return null;
    }


    public GroupInfo findGroupInfoByAddressDataId(String address, String dataId) {
        // TODO Auto-generated method stub
        return null;
    }


    public List<GroupInfo> findGroupInfoByGroup(String group) {
        // TODO Auto-generated method stub
        return null;
    }


    public List<GroupInfo> findAllGroupInfo() {
        // TODO Auto-generated method stub
        return null;
    }


    public Page<ConfigInfo> findConfigInfoByGroup(int pageNo, int pageSize, String group) {
        // TODO Auto-generated method stub
        return null;
    }


    public Page<ConfigInfo> findAllConfigInfo(int pageNo, int pageSize) {
        // TODO Auto-generated method stub
        return null;
    }


    public Page<ConfigInfo> findConfigInfoByDataId(int pageNo, int pageSize, String dataId) {
        // TODO Auto-generated method stub
        return null;
    }


    public Page<ConfigInfo> findConfigInfoLike(int pageNo, int pageSize, String dataId, String group) {
        // TODO Auto-generated method stub
        return null;
    }


    public int countAllDataIds() {
        // TODO Auto-generated method stub
        return 0;
    }


    public void hangingSimulationMode() {
        // int queryTimeout = 1;
        // getJdbcTemplate().setQueryTimeout(queryTimeout);
        // getJdbcTemplate().execute(new
        // ProcCallableStatementCreator("sp_test_sleep", 1000),new
        // ProcCallableStatementCallback());

    }

}


class ProcCallableStatementCreator implements CallableStatementCreator {
    private String storedProc;
    private int sleepTime;


    /**
     * Constructs a callable statement.
     * 
     * @param storedProc
     *            The stored procedure's name.
     * @param params
     *            Input parameters.
     * @param outResultCount
     *            count of output result set.
     */
    public ProcCallableStatementCreator(String storedProc, int sleepTime) {
        this.sleepTime = sleepTime;
        this.storedProc = storedProc;
    }


    /**
     * Returns a callable statement
     * 
     * @param conn
     *            Connection to use to create statement
     * @return cs A callable statement
     */
    public CallableStatement createCallableStatement(Connection conn) {
        StringBuffer storedProcName = new StringBuffer("call ");
        storedProcName.append(storedProc + "(");
        // set output parameters
        storedProcName.append("?");
        storedProcName.append(")");

        CallableStatement cs = null;
        try {
            // set the first parameter is OracleTyep.CURSOR for oracel stored
            // procedure
            cs = conn.prepareCall(storedProcName.toString());
            cs.setInt(1, sleepTime);
        }
        catch (SQLException e) {
            throw new RuntimeException("createCallableStatement method Error : SQLException " + e.getMessage());
        }
        return cs;
    }

}


class ProcCallableStatementCallback implements CallableStatementCallback {

    /**
     * Constructs a ProcCallableStatementCallback.
     */
    public ProcCallableStatementCallback() {
    }


    /**
     * Returns a List(Map) collection.
     * 
     * @param cs
     *            object that can create a CallableStatement given a Connection
     * @return resultsList a result object returned by the action, or null
     */
    public Object doInCallableStatement(CallableStatement cs) {
        try {
            // cs.setInt(1,1000);
            cs.execute();
        }
        catch (SQLException e) {
            throw new RuntimeException("doInCallableStatement method error : SQLException " + e.getMessage());
        }
        return null;
    }
}
