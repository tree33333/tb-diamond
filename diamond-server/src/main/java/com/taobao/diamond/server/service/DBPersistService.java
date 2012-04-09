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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.StringUtils;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.server.utils.PaginationHelper;


/**
 * 从PersistService中剥离跟DataSource的直接耦合,
 * 
 * 避免dataSource注入失败导致PersistService初始化失败，
 * 
 * 避免Spring框架抛出异常,影响其它的bean注入
 * 
 * @author zhidao 2011/04/02
 * 
 */

public class DBPersistService extends JdbcDaoSupport implements PersistService {

    @Override
    protected void initTemplateConfig() {
        // 设置最大记录数，防止内存膨胀
        getJdbcTemplate().setMaxRows(10000);
    }

    static final Log log = LogFactory.getLog(PersistService.class);

    static int QUERY_TIMEOUT = 1;// seconds


    public void initDataSource(DataSource dataSource) {
        this.setDataSource(dataSource);
    }

    private static final class ConfigInfoRowMapper implements ParameterizedRowMapper<ConfigInfo> {
        public ConfigInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            ConfigInfo info = new ConfigInfo();
            info.setId(rs.getLong("ID"));
            info.setDataId(rs.getString("data_id"));
            info.setGroup(rs.getString("group_id"));
            info.setContent(rs.getString("content"));
            info.setMd5(rs.getString("md5"));
            return info;
        }
    }

    private static final class GroupRowMapper implements ParameterizedRowMapper<GroupInfo> {
        public GroupInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            GroupInfo info = new GroupInfo();
            info.setId(rs.getLong("ID"));
            info.setAddress(rs.getString("address"));
            info.setGroup(rs.getString("group_id"));
            info.setDataId(rs.getString("data_id"));
            return info;
        }
    }

    private static final ConfigInfoRowMapper CONFIG_INFO_ROW_MAPPER = new ConfigInfoRowMapper();

    private static final GroupRowMapper GROUP_INFO_ROW_MAPPER = new GroupRowMapper();


    /**
     * 添加ConfigInfo到数据库
     * 
     * @param configInfo
     */
    public void addConfigInfo(final ConfigInfo configInfo) {

        getJdbcTemplate().update("insert into config_info (data_id,group_id,content,md5) values(?,?,?,?)",
            new PreparedStatementSetter() {
                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, configInfo.getDataId());
                    ps.setString(index++, configInfo.getGroup());
                    ps.setString(index++, configInfo.getContent());
                    ps.setString(index++, configInfo.getMd5());
                }

            });
    }


    public void addConfigInfo(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfo configInfo) {

        getJdbcTemplate()
            .update(
                "insert into config_info (data_id,group_id,content,md5,src_ip,src_user,gmt_create,gmt_modified) values(?,?,?,?,?,?,?,?)",
                new PreparedStatementSetter() {

                    public void setValues(PreparedStatement ps) throws SQLException {
                        int index = 1;
                        ps.setString(index++, configInfo.getDataId());
                        ps.setString(index++, configInfo.getGroup());
                        ps.setString(index++, configInfo.getContent());
                        ps.setString(index++, configInfo.getMd5());
                        ps.setString(index++, srcIp);
                        ps.setString(index++, srcUser);
                        ps.setTimestamp(index++, time);
                        ps.setTimestamp(index++, time);
                    }
                });
    }


    /**
     * 添加GroupInfo到数据库
     * 
     * @param groupInfo
     */
    public void addGroupInfo(final GroupInfo groupInfo) {

        getJdbcTemplate().update("insert into group_info (address,group_id,data_id) values(?,?,?)",
            new PreparedStatementSetter() {
                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, groupInfo.getAddress());
                    ps.setString(index++, groupInfo.getGroup());
                    ps.setString(index++, groupInfo.getDataId());
                }
            });
    }


    public void addGroupInfo(final String srcIp, final String srcUser, final Timestamp time, final GroupInfo groupInfo) {

        getJdbcTemplate()
            .update(
                "insert into group_info (address,group_id,data_id,src_ip,src_user,gmt_create,gmt_modified) values(?,?,?,?,?,?,?)",
                new PreparedStatementSetter() {

                    public void setValues(PreparedStatement ps) throws SQLException {
                        int index = 1;
                        ps.setString(index++, groupInfo.getAddress());
                        ps.setString(index++, groupInfo.getGroup());
                        ps.setString(index++, groupInfo.getDataId());
                        ps.setString(index++, srcIp);
                        ps.setString(index++, srcUser);
                        ps.setTimestamp(index++, time);
                        ps.setTimestamp(index++, time);
                    }

                });
    }


    /**
     * 根据dataId和group删除配置信息
     * 
     * @param dataId
     * @param group
     */
    public void removeConfigInfo(final String dataId, final String group) {

        getJdbcTemplate().update("delete from config_info where data_id=? and group_id=?",
            new PreparedStatementSetter() {
                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, dataId);
                    ps.setString(index++, group);
                }

            });
    }


    /**
     * 从数据库删除Group信息
     * 
     * @param dataId
     * @param group
     */
    public void removeGroupInfoByID(final long id) {

        getJdbcTemplate().update("delete from group_info where id=?", new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setLong(1, id);
            }
        });

    }


    public void removeConfigInfo(ConfigInfo configInfo) {
        removeConfigInfo(configInfo.getDataId(), configInfo.getGroup());
    }


    public void removeConfigInfoByID(final long id) {

        getJdbcTemplate().update("delete from config_info where ID=? ", new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setLong(1, id);
            }

        });
    }


    /**
     * 更新分组信息
     * 
     * @param address
     *            ip地址
     * @param oldGroup
     *            老的分组名
     * @param newGroup
     *            新的分组名
     */
    public void updateGroup(final long id, final String newGroup) {

        getJdbcTemplate().update("update group_info set group_id=? where id=?", new PreparedStatementSetter() {

            public void setValues(PreparedStatement ps) throws SQLException {
                int index = 1;
                ps.setString(index++, newGroup);
                ps.setLong(index++, id);
            }
        });
    }


    public void updateGroup(final long id, final String srcIp, final String srcUser, final Timestamp time,
            final String newGroup) {

        getJdbcTemplate().update("update group_info set group_id=?,src_ip=?,src_user=?,gmt_modified=? where id=?",
            new PreparedStatementSetter() {

                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, newGroup);
                    ps.setString(index++, srcIp);
                    ps.setString(index++, srcUser);
                    ps.setTimestamp(index++, time);
                    ps.setLong(index++, id);
                }
            });
    }


    public void updateConfigInfo(final ConfigInfo configInfo) {

        getJdbcTemplate().update("update config_info set content=?,md5=? where data_id=? and group_id=?",
            new PreparedStatementSetter() {

                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, configInfo.getContent());
                    ps.setString(index++, configInfo.getMd5());
                    ps.setString(index++, configInfo.getDataId());
                    ps.setString(index++, configInfo.getGroup());
                }
            });
    }


    public void updateConfigInfo(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfo configInfo) {

        getJdbcTemplate().update(
            "update config_info set content=?,md5=?,src_ip=?,src_user=?,gmt_modified=? where data_id=? and group_id=?",
            new PreparedStatementSetter() {

                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, configInfo.getContent());
                    ps.setString(index++, configInfo.getMd5());
                    ps.setString(index++, srcIp);
                    ps.setString(index++, srcUser);
                    ps.setTimestamp(index++, time);
                    ps.setString(index++, configInfo.getDataId());
                    ps.setString(index++, configInfo.getGroup());
                }
            });
    }


    public int updateConfigInfoByMd5(final ConfigInfoEx configInfoEx) {
        return getJdbcTemplate().update(
            "update config_info set content=?,md5=? where data_id=? and group_id=? and md5=?",
            new PreparedStatementSetter() {

                public void setValues(PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setString(index++, configInfoEx.getContent());
                    ps.setString(index++, configInfoEx.getMd5());
                    ps.setString(index++, configInfoEx.getDataId());
                    ps.setString(index++, configInfoEx.getGroup());
                    ps.setString(index++, configInfoEx.getOldMd5());
                }
            });
    }


    public int updateConfigInfoByMd5(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfoEx configInfoEx) {

        return getJdbcTemplate()
            .update(
                "update config_info set content=?,md5=?,src_ip=?,src_user=?,gmt_modified=? where data_id=? and group_id=? and md5=?",
                new PreparedStatementSetter() {

                    public void setValues(PreparedStatement ps) throws SQLException {
                        int index = 1;
                        ps.setString(index++, configInfoEx.getContent());
                        ps.setString(index++, configInfoEx.getMd5());
                        ps.setString(index++, srcIp);
                        ps.setString(index++, srcUser);
                        ps.setTimestamp(index++, time);
                        ps.setString(index++, configInfoEx.getDataId());
                        ps.setString(index++, configInfoEx.getGroup());
                        ps.setString(index++, configInfoEx.getOldMd5());
                    }
                });
    }


    /**
     * 根据dataId和group查询ConfigInfo
     * 
     * @param dataId
     * @param group
     * @return
     */
    public ConfigInfo findConfigInfo(final String dataId, final String group) {

        try {
            return (ConfigInfo) getJdbcTemplate().queryForObject(
                "select ID,data_id,group_id,content,md5 from config_info where group_id=? and data_id=?",
                new Object[] { group, dataId }, CONFIG_INFO_ROW_MAPPER);
        }
        catch (DataAccessException e) {
            if (!(e instanceof EmptyResultDataAccessException)) {
                log.error("查询ConfigInfo失败, 数据库异常", e);
                // 将异常重新抛出
                throw e;
            }
            // 是EmptyResultDataAccessException, 表明数据不存在, 返回null
            return null;
        }
    }


    public ConfigInfo findConfigInfoByID(long id) {

        try {
            return (ConfigInfo) getJdbcTemplate().queryForObject(
                "select ID,data_id,group_id,content,md5 from config_info where ID=?", new Object[] { id },
                CONFIG_INFO_ROW_MAPPER);
        }
        catch (DataAccessException e) {
            if (!(e instanceof EmptyResultDataAccessException)) {
                log.error("查询ConfigInfo失败, 数据库异常", e);
                throw e;
            }
            return null;
        }
    }


    public GroupInfo findGroupInfoByID(long id) {

        try {
            return (GroupInfo) getJdbcTemplate().queryForObject(
                "select ID,address,group_id,data_id from group_info where ID=?", new Object[] { id },
                GROUP_INFO_ROW_MAPPER);
        }
        catch (DataAccessException e) {
            if (!(e instanceof EmptyResultDataAccessException)) {
                log.error("查询GroupInfo失败, 数据库异常", e);
                throw e;
            }
            return null;
        }
    }


    /**
     * 根据IP查找分组信息
     * 
     * @param address
     * @return
     */
    public GroupInfo findGroupInfoByAddressDataId(String address, String dataId) {

        try {
            return (GroupInfo) getJdbcTemplate().queryForObject(
                "select ID,address,group_id,data_id from group_info where address=? and data_id=?",
                new Object[] { address, dataId }, GROUP_INFO_ROW_MAPPER);
        }
        catch (DataAccessException e) {
            if (!(e instanceof EmptyResultDataAccessException)) {
                log.error("查询GroupInfo失败, 数据库异常", e);
                throw e;
            }
            return null;
        }
    }


    /**
     * 根据group查找该分组所有ip
     * 
     * @param group
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<GroupInfo> findGroupInfoByGroup(String group) {

        return getJdbcTemplate().query("select ID,address,group_id,data_id from group_info where group_id=?",
            new Object[] { group }, GROUP_INFO_ROW_MAPPER);

    }


    /**
     * 加载所有的分组信息
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<GroupInfo> findAllGroupInfo() {

        getJdbcTemplate().setQueryTimeout(QUERY_TIMEOUT);
        return getJdbcTemplate().query("select ID,address,group_id,data_id from group_info", GROUP_INFO_ROW_MAPPER);
    }


    /**
     * 根据group查询配置信息
     * 
     * @param pageNo
     *            页数
     * @param pageSize
     *            每页大小
     * @param group
     *            组名
     * @return
     */
    public Page<ConfigInfo> findConfigInfoByGroup(final int pageNo, final int pageSize, final String group) {

        PaginationHelper<ConfigInfo> helper = new PaginationHelper<ConfigInfo>();
        return helper.fetchPage(getJdbcTemplate(), "select count(ID) from config_info where group_id=?",
            "select ID,data_id,group_id,content,md5 from config_info where group_id=? ", new Object[] { group },
            pageNo, pageSize, CONFIG_INFO_ROW_MAPPER);
    }


    /**
     * 分页查询所有的配置信息
     * 
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ConfigInfo> findAllConfigInfo(final int pageNo, final int pageSize) {

        getJdbcTemplate().setQueryTimeout(QUERY_TIMEOUT);
        PaginationHelper<ConfigInfo> helper = new PaginationHelper<ConfigInfo>();
        return helper.fetchPage(getJdbcTemplate(), "select count(ID) from config_info order by ID",
            "select ID,data_id,group_id,content,md5 from config_info order by ID ", new Object[] {}, pageNo, pageSize,
            CONFIG_INFO_ROW_MAPPER);
    }


    /**
     * 根据dataId查询配置信息
     * 
     * @param pageNo
     *            页数
     * @param pageSize
     *            每页大小
     * @param dataId
     *            dataId
     * @return
     */
    public Page<ConfigInfo> findConfigInfoByDataId(final int pageNo, final int pageSize, final String dataId) {

        PaginationHelper<ConfigInfo> helper = new PaginationHelper<ConfigInfo>();
        getJdbcTemplate().setQueryTimeout(1);
        return helper.fetchPage(getJdbcTemplate(), "select count(ID) from config_info where data_id=?",
            "select ID,data_id,group_id,content,md5 from config_info where data_id=? ", new Object[] { dataId },
            pageNo, pageSize, CONFIG_INFO_ROW_MAPPER);
    }


    public int countAllDataIds() {
        return getJdbcTemplate().queryForInt("select count(*) from config_info");
    }


    /**
     * 根据dataId和group模糊查询配置信息
     * 
     * @param pageNo
     *            页数
     * @param pageSize
     *            每页大小
     * @param dataId
     *            dataId
     * @return
     */
    public Page<ConfigInfo> findConfigInfoLike(final int pageNo, final int pageSize, final String dataId,
            final String group) {

        PaginationHelper<ConfigInfo> helper = new PaginationHelper<ConfigInfo>();
        String sqlCountRows = "select count(ID) from config_info where ";
        String sqlFetchRows = "select ID,data_id,group_id,content,md5 from config_info where ";
        boolean wasFirst = true;
        if (StringUtils.hasLength(dataId)) {
            sqlCountRows += "data_id like ? ";
            sqlFetchRows += "data_id like ? ";
            wasFirst = false;
        }
        if (StringUtils.hasLength(group)) {
            if (wasFirst) {
                sqlCountRows += "group_id like ? ";
                sqlFetchRows += "group_id like ? ";
            }
            else {
                sqlCountRows += "and group_id like ? ";
                sqlFetchRows += "and group_id like ? ";
            }
        }
        Object[] args = null;
        if (StringUtils.hasLength(dataId) && StringUtils.hasLength(group)) {
            args = new Object[2];
            args[0] = generateLikeArgument(dataId);
            args[1] = generateLikeArgument(group);
        }
        else if (StringUtils.hasLength(dataId)) {
            args = new Object[] { generateLikeArgument(dataId) };
        }
        else if (StringUtils.hasLength(group)) {
            args = new Object[] { generateLikeArgument(group) };
        }

        return helper.fetchPage(getJdbcTemplate(), sqlCountRows, sqlFetchRows, args, pageNo, pageSize,
            CONFIG_INFO_ROW_MAPPER);
    }


    private String generateLikeArgument(String s) {
        if (s.indexOf("*") >= 0)
            return s.replaceAll("\\*", "%");
        else {
            return "%" + s + "%";
        }
    }

}
