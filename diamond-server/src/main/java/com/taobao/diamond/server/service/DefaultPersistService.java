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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.server.utils.SystemConfig;


public class DefaultPersistService implements PersistService {
    static final Log log = LogFactory.getLog(DefaultPersistService.class);
    private PersistService persistService;

    private DataSource dataSource;


    public DataSource getDataSource() {
        return dataSource;
    }


    public void setDataSource(DataSource dataSource) {
        System.out.println("#setDataSource");
        this.dataSource = dataSource;
        initDataSource(dataSource);
    }


    public void initDataSource(DataSource dataSource) {
        System.out.println("#initDataSource");
        if (dataSource == null) {
            System.out.println("dataSource is null.");
        }
        String mode = System.getProperty("diamond.server.mode");
        System.out.println("-Ddiamond.server.mode:" + mode);
        String[] attrNames = new String[] { "offline", "flying", "lostDB", "xx" };
        for (String key : attrNames) {
            if (key.equals(mode)) {
                SystemConfig.setOffline();
            }
        }
        int timeout = 3;// seconds
        boolean dsValid = false;
        Connection conn = null;
        if (dataSource != null)
            try {
                BasicDataSource bds = (BasicDataSource) dataSource;
                bds.isPoolPreparedStatements();
                conn = dataSource.getConnection();

                Statement stmt = conn.createStatement();
                stmt.setQueryTimeout(timeout);
                dsValid = true;
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM config_info");
                    if (rs.next()) {
                        rs.getInt(1);
                    }
                    else {
                        dsValid = false;
                    }
                    rs = stmt.executeQuery("select count(*) from group_info");
                    if (rs.next()) {
                        rs.getInt(1);
                    }
                    else {
                        dsValid = false;
                    }
                }
                catch (Exception e) {
                    dsValid = false;
                }

            }
            catch (Throwable t) {
                log.error(t.getMessage(), t.getCause());
            }

        if (dsValid == false) {
            // 数据库模式
            if (SystemConfig.isOnlineMode()) {
                System.out.println("#########################################################");
                System.out.println("DataSource 初始化异常，连接超时或者主机拒连，按任意键终止.");
                System.out.println("error occured in DataSource initilizing,connection timeout or refuse conn.");
                System.out.println("#########################################################");
                SystemConfig.system_pause();
                System.exit(0);
            }
            // 离线模式
            if (SystemConfig.isOfflineMode()) {
                String msg = "##########################离线模式###############################";
                System.out.println(msg);
                log.info(msg);
                OfflinePersistService ps = new OfflinePersistService();
                persistService = ps;

            }
        }
        else {
            DBPersistService ps = new DBPersistService();
            ps.setDataSource(dataSource);
            persistService = ps;
        }
        System.out.println("#########################################################");
        System.out.println("Current Persist Service");
        System.out.println("当前persistService:" + persistService);
        System.out.println("DBPersistService:" + (persistService instanceof DBPersistService));
        System.out.println("OfflinePersistService:" + (persistService instanceof OfflinePersistService));
        System.out.println("#########################################################");
    }


    public void addConfigInfo(ConfigInfo configInfo) {
        persistService.addConfigInfo(configInfo);
    }


    public void addConfigInfo(String srcIp, String srcUser, Timestamp time, ConfigInfo configInfo) {
        persistService.addConfigInfo(srcIp, srcUser, time, configInfo);
    }


    public void addGroupInfo(GroupInfo groupInfo) {
        persistService.addGroupInfo(groupInfo);
    }


    public void addGroupInfo(String srcIp, String srcUser, Timestamp time, GroupInfo groupInfo) {
        persistService.addGroupInfo(srcIp, srcUser, time, groupInfo);
    }


    public void removeConfigInfo(String dataId, String group) {
        persistService.removeConfigInfo(dataId, group);
    }


    public void removeGroupInfoByID(long id) {
        persistService.removeGroupInfoByID(id);
    }


    public void removeConfigInfo(ConfigInfo configInfo) {
        persistService.removeConfigInfo(configInfo);
    }


    public void removeConfigInfoByID(long id) {
        persistService.removeConfigInfoByID(id);
    }


    public void updateGroup(long id, String newGroup) {
        persistService.updateGroup(id, newGroup);
    }


    public void updateGroup(long id, String srcIp, String srcUser, Timestamp time, String newGroup) {
        persistService.updateGroup(id, srcIp, srcUser, time, newGroup);
    }


    public void updateConfigInfo(ConfigInfo configInfo) {
        persistService.updateConfigInfo(configInfo);
    }


    public void updateConfigInfo(String srcIp, String srcUser, Timestamp time, ConfigInfo configInfo) {
        persistService.updateConfigInfo(srcIp, srcUser, time, configInfo);
    }


    public int updateConfigInfoByMd5(ConfigInfoEx configInfoEx) {
        return persistService.updateConfigInfoByMd5(configInfoEx);
    }


    public int updateConfigInfoByMd5(String srcIp, String srcUser, Timestamp time, ConfigInfoEx configInfoEx) {
        return persistService.updateConfigInfoByMd5(srcIp, srcUser, time, configInfoEx);
    }


    public ConfigInfo findConfigInfo(String dataId, String group) {
        return persistService.findConfigInfo(dataId, group);
    }


    public ConfigInfo findConfigInfoByID(long id) {
        return persistService.findConfigInfoByID(id);
    }


    public GroupInfo findGroupInfoByID(long id) {
        return persistService.findGroupInfoByID(id);
    }


    public GroupInfo findGroupInfoByAddressDataId(String address, String dataId) {
        return persistService.findGroupInfoByAddressDataId(address, dataId);
    }


    public List<GroupInfo> findGroupInfoByGroup(String group) {
        return persistService.findGroupInfoByGroup(group);
    }


    public List<GroupInfo> findAllGroupInfo() {
        return persistService.findAllGroupInfo();
    }


    public Page<ConfigInfo> findConfigInfoByGroup(int pageNo, int pageSize, String group) {
        return persistService.findConfigInfoByGroup(pageNo, pageSize, group);
    }


    public Page<ConfigInfo> findAllConfigInfo(int pageNo, int pageSize) {
        return persistService.findAllConfigInfo(pageNo, pageSize);
    }


    public Page<ConfigInfo> findConfigInfoByDataId(int pageNo, int pageSize, String dataId) {
        return persistService.findConfigInfoByDataId(pageNo, pageSize, dataId);
    }


    public Page<ConfigInfo> findConfigInfoLike(int pageNo, int pageSize, String dataId, String group) {
        return persistService.findConfigInfoLike(pageNo, pageSize, dataId, group);
    }


    public int countAllDataIds() {
        return persistService.countAllDataIds();
    }

}
