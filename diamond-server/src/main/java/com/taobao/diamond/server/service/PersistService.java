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


/**
 * 数据库服务，提供ConfigInfo,GroupInfo在数据库的存取
 * 
 * @author boyan
 * @version 1.0 2010-5-5
 * @version 1.0.1 2011/04/02 调整为接口，分出DB和Offline两种实现
 * @version 2.0 2012/03/21 写 操作提供传入修改时间、用户名、源IP的接口（删除时这些参数没用, 删除依然是物理删除）
 * @since 1.0
 */

public interface PersistService {

    /**
     * 添加ConfigInfo到数据库
     * 
     * @param configInfo
     */
    public void addConfigInfo(final ConfigInfo configInfo);


    /**
     * 添加ConfigInfo到数据库, 并增加创建时间和源头IP、源头用户
     * 
     * @param srcIp
     * @param srcUser
     * @param date
     * @param configInfo
     */
    public void addConfigInfo(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfo configInfo);


    /**
     * 添加GroupInfo到数据库
     * 
     * @param groupInfo
     */
    public void addGroupInfo(final GroupInfo groupInfo);


    /**
     * 添加GroupInfo到数据库, 并增加创建时间和源头IP、源头用户
     * 
     * @param srcIp
     * @param srcUser
     * @param date
     * @param groupInfo
     */
    public void addGroupInfo(final String srcIp, final String srcUser, final Timestamp time, final GroupInfo groupInfo);


    /**
     * 根据dataId和group删除配置信息
     * 
     * @param dataId
     * @param group
     */
    public void removeConfigInfo(final String dataId, final String group);


    /**
     * 从数据库删除Group信息
     * 
     * @param dataId
     * @param group
     */
    public void removeGroupInfoByID(final long id);


    /**
     * 
     * @param configInfo
     */
    public void removeConfigInfo(ConfigInfo configInfo);


    /**
     * 
     * @param id
     */
    public void removeConfigInfoByID(final long id);


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
    public void updateGroup(final long id, final String newGroup);


    /**
     * 更新分组信息, 并增加更新时间和源头IP、源头用户
     * 
     * @param id
     * @param srcIp
     * @param srcUser
     * @param time
     * @param newGroup
     */
    public void updateGroup(final long id, final String srcIp, final String srcUser, final Timestamp time,
            final String newGroup);


    /**
     * 更新配置数据
     * 
     * @param configInfo
     */
    public void updateConfigInfo(final ConfigInfo configInfo);


    /**
     * 更新配置数据, 并增加更新时间和源头IP、源头用户
     * 
     * @param srcIp
     * @param srcUser
     * @param time
     * @param configInfo
     */
    public void updateConfigInfo(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfo configInfo);


    /**
     * 通过比较ConfigInfo和数据库记录的md5值来更新数据
     * 
     * @param configInfo
     * @return 更新的记录条数
     * @author leiwen
     */
    public int updateConfigInfoByMd5(final ConfigInfoEx configInfoEx);


    /**
     * 通过比较ConfigInfo和数据库记录的md5值来更新数据, 并增加更新时间和源头IP、源头用户
     * 
     * @param srcIp
     * @param srcUser
     * @param time
     * @param configInfoEx
     * @return
     */
    public int updateConfigInfoByMd5(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfoEx configInfoEx);


    /**
     * 根据dataId和group查询ConfigInfo
     * 
     * @param dataId
     * @param group
     * @return
     */
    public ConfigInfo findConfigInfo(final String dataId, final String group);


    public ConfigInfo findConfigInfoByID(long id);


    public GroupInfo findGroupInfoByID(long id);


    /**
     * 根据IP查找分组信息
     * 
     * @param address
     * @return
     */
    public GroupInfo findGroupInfoByAddressDataId(String address, String dataId);


    /**
     * 根据group查找该分组所有ip
     * 
     * @param group
     * @return
     */
    public List<GroupInfo> findGroupInfoByGroup(String group);


    /**
     * 加载所有的分组信息
     * 
     * @return
     */
    public List<GroupInfo> findAllGroupInfo();


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
    public Page<ConfigInfo> findConfigInfoByGroup(final int pageNo, final int pageSize, final String group);


    /**
     * 分页查询所有的配置信息
     * 
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ConfigInfo> findAllConfigInfo(final int pageNo, final int pageSize);


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
    public Page<ConfigInfo> findConfigInfoByDataId(final int pageNo, final int pageSize, final String dataId);


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
            final String group);


    /**
     * 查询dataId的总数
     * 
     * @return
     */
    public int countAllDataIds();
}
