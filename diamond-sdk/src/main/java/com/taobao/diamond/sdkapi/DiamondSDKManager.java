/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.sdkapi;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.taobao.diamond.domain.BatchContextResult;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.ContextResult;
import com.taobao.diamond.domain.DiamondSDKConf;
import com.taobao.diamond.domain.PageContextResult;


/**
 * 定义SDK对外开放的数据访问接口
 * 
 * @filename DiamondSDKManager.java
 * @author libinbin.pt
 * @datetime 2010-7-16 下午04:03:28
 * 
 *           {@link #exists(String, String, String)}
 */
public interface DiamondSDKManager {

    /**
     * 得到diamondSDKConfMaps配置集的map
     * 
     * @return Map<String, DiamondSDKConf>
     */
    public Map<String, DiamondSDKConf> getDiamondSDKConfMaps();


    // /////////////////////////////////////////推送数据接口定义////////////////////////////////////////
    /**
     * 使用指定的diamond来推送数据
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult 单个对象
     */
    public ContextResult pulish(String dataId, String groupName, String context, String serverId);


    /**
     * 使用首个配置的 diamond来推送数据
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult 单个对象
     */
    public ContextResult pulishFromDefaultServer(String dataId, String groupName, String context);


    /**
     * 使用指定的diamond发送分组信息
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 单个对象
     */
    public ContextResult publishGroup(String address, String dataId, String groupName, String serverId);


    // /////////////////////////////////////////推送修改后的数据接口定义////////////////////////////////////////
    /**
     * 使用指定的diamond来推送修改后的数据,修改前先检查数据存在性
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult 单个对象
     */
    public ContextResult pulishAfterModified(String dataId, String groupName, String context, String serverId);


    /**
     * 使用首个配置的 diamond来推送修改后的数据,修改前先检查数据存在性
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult 单个对象
     */
    public ContextResult pulishFromDefaultServerAfterModified(String dataId, String groupName, String context);


    /**
     * 按照id来修改分组信息
     * 
     * @param id
     * @param groupName
     * @param serverId
     * @return ContextResult 单个对象
     */
    public ContextResult moveGroup(long id, String groupName, String serverId);


    // /////////////////////////////////////////模糊查询接口定义////////////////////////////////////////
    /**
     * 根据指定的 dataId和组名到指定的diamond上查询数据列表 如果模式中包含符号'*',则会自动替换为'%'并使用[ like ]语句
     * 如果模式中不包含符号'*'并且不为空串（包括" "）,则使用[ = ]语句
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> 单个对象
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern, String serverId,
            long currentPage, long sizeOfPerPage);


    /**
     * 根据指定的 dataId和组名到首个配置的diamond来查询数据列表 如果模式中包含符号'*',则会自动替换为'%'并使用[ like ]语句
     * 如果模式中不包含符号'*'并且不为空串（包括" "）,则使用[ = ]语句
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> 单个对象
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern, String groupNamePattern,
            long currentPage, long sizeOfPerPage);


    /**
     * 根据指定的 dataId,组名和content到指定配置的diamond来查询数据列表 如果模式中包含符号'*',则会自动替换为'%'并使用[
     * like ]语句 如果模式中不包含符号'*'并且不为空串（包括" "）,则使用[ = ]语句
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param contentPattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> 单个对象
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern, String contentPattern,
            String serverId, long currentPage, long sizeOfPerPage);


    /**
     * 根据指定的 dataId,组名和content到首个配置的diamond来查询数据列表 如果模式中包含符号'*',则会自动替换为'%'并使用[
     * like ]语句 如果模式中不包含符号'*'并且不为空串（包括" "）,则使用[ = ]语句
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param contentPattern
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> 单个对象
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern, String groupNamePattern,
            String contentPattern, long currentPage, long sizeOfPerPage);


    // /////////////////////////////////////////精确查询接口定义////////////////////////////////////////
    /**
     * 根据指定的dataId和组名到指定的diamond上查询数据列表
     * 
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 单个对象
     * @throws SQLException
     */
    public ContextResult queryByDataIdAndGroupName(String dataId, String groupName, String serverId);


    /**
     * 根据指定的 dataId和组名到首个配置的diamond上查询数据列表
     * 
     * @param dataId
     * @param groupName
     * @return ContextResult 单个对象
     * @throws SQLException
     */
    public ContextResult queryFromDefaultServerByDataIdAndGroupName(String dataId, String groupName);


    /**
     * 根据指定的dataId和ip address到指定的diamond上查询分组信息
     * 
     * @param address
     * @param dataId
     * @param serverId
     * @return ContextResult 单个对象
     */
    public ContextResult queryByAddressAndDataId(String address, String dataId, String serverId);


    /**
     * 查询所有分组
     * 
     * @param serverId
     * @return ContextResult 单个对象
     */
    public ContextResult queryAllGroup(String serverId);


    // /////////////////////////////////////////移除信息接口定义////////////////////////////////////
    /**
     * 移除特定服务器上id指定的配置信息
     * 
     * @param serverId
     * @param id
     * @return ContextResult 单个对象
     */
    public ContextResult unpublish(String serverId, long id);


    /**
     * 移除默认服务器上id指定的配置信息
     * 
     * @param serverId
     * @param id
     * @return ContextResult 单个对象
     */
    public ContextResult unpublishFromDefaultServer(long id);


    /**
     * 移除指定服务器上的id指定的分组信息
     * 
     * @param serverId
     * @param id
     * @return ContextResult 单个对象
     */
    public ContextResult deleteGroup(String serverId, long id);


    /**
     * 重新加载分组信息
     * 
     * @param serverId
     * @return ContextResult 单个对象
     * 
     */
    public ContextResult reloadGroup(String serverId);


    /**
     * 判断指定的配置项是否存在
     * 
     * @param dataId
     * @param group
     * @param serverId
     * @return
     */
    public boolean exists(String dataId, String group, String serverId);


    /**
     * 已过时, 请使用batchAdd()和batchUpdate()
     * 
     * @param groupName
     * @param dataIds
     * @param contents
     * @param serverId
     * @return
     */
    @Deprecated
    public ContextResult createOrUpdate(String groupName, List<String> dataIds, List<String> contents, String serverId);


    /**
     * 批量查询
     * 
     * @param groupName
     * @param dataIds
     * @param serverId
     * @return
     */
    public BatchContextResult<ConfigInfoEx> batchQuery(String serverId, String groupName, List<String> dataIds);


    /**
     * 批量新增或更新
     * 
     * @param serverId
     * @param groupName
     * @param dataId2ContentMap
     *            key:dataId,value:content
     * @return
     */
    public BatchContextResult<ConfigInfoEx> batchAddOrUpdate(String serverId, String groupName, String srcIp,
            String srcUser, Map<String/* dataId */, String/* content */> dataId2ContentMap);


    /**
     * 已过时, 请使用batchAdd()和batchUpdate()
     * 
     * @param groupName
     * @param dataIds
     * @param contents
     * @param serverId
     * 
     * @return
     */
    @Deprecated
    public ContextResult createOrUpdate(String groupName, String dataId, String content, String serverId);


    /**
     * 获取发布的dataId
     * 
     * @param dataId
     * @return
     */
    public String getRealTimeDataId(String dataId);


    /**
     * 发布数据, 并记录发布者的IP和名称, 该接口主要提供给diamond-ops、rtools等运维工具使用
     * 
     * @param dataId
     * @param group
     * @param content
     * @param srcIp
     * @param srcUser
     * @return
     */
    public ContextResult publish(String dataId, String group, String content, String serverId, String srcIp,
            String srcUser);


    /**
     * 
     * 更新数据, 并记录发布者的IP和名称, 该接口主要提供给diamond-ops、rtools等运维工具使用
     * 
     * @param dataId
     * @param group
     * @param content
     * @param serverId
     * @param srcIp
     * @param srcUser
     * @return
     */
    public ContextResult publishAfterModified(String dataId, String group, String content, String serverId,
            String srcIp, String srcUser);

}
