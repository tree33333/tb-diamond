/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.sdkapi.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.base.Joiner;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.BatchContextResult;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.ContextResult;
import com.taobao.diamond.domain.DiamondConf;
import com.taobao.diamond.domain.DiamondSDKConf;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.domain.PageContextResult;
import com.taobao.diamond.sdkapi.DiamondSDKManager;
import com.taobao.diamond.util.DiamondUtils;
import com.taobao.diamond.util.PatternUtils;
import com.taobao.diamond.util.RandomDiamondUtils;
import com.taobao.diamond.utils.JSONUtils;
import com.taobao.diamond.utils.SimpleFlowData;


/**
 * SDK对外开放的数据接口的功能实现
 * 
 * @filename DiamondSDKManagerImpl.java
 * @author libinbin.pt
 * @datetime 2010-7-16 下午04:00:19
 */
public class DiamondSDKManagerImpl implements DiamondSDKManager {

    private static final Log log = LogFactory.getLog("diamondSdkLog");

    private static final String CONFIG_CLIENT_PUBLISH_SUFFIX = "diamondRealTime";

    public static final String DIAMOND_REALTIME_GROUP = CONFIG_CLIENT_PUBLISH_SUFFIX;

    // DiamondSDKConf配置集map
    private Map<String, DiamondSDKConf> diamondSDKConfMaps;

    // 连接超时时间
    private final int connection_timeout;
    // 请求超时时间
    private final int require_timeout;

    private final SimpleFlowData flowData = new SimpleFlowData(10, 2000);


    // 构造时需要传入连接超时时间，请求超时时间
    public DiamondSDKManagerImpl(int connection_timeout, int require_timeout) throws IllegalArgumentException {
        if (connection_timeout < 0)
            throw new IllegalArgumentException("连接超时时间设置必须大于0[单位(毫秒)]!");
        if (require_timeout < 0)
            throw new IllegalArgumentException("请求超时时间设置必须大于0[单位(毫秒)]!");
        this.connection_timeout = connection_timeout;
        this.require_timeout = require_timeout;
        int maxHostConnections = 50;
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

        connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxHostConnections);
        connectionManager.getParams().setStaleCheckingEnabled(true);
        this.client = new HttpClient(connectionManager);
        // 设置连接超时时间
        client.getHttpConnectionManager().getParams().setConnectionTimeout(this.connection_timeout);
        // 设置读超时为1分钟
        client.getHttpConnectionManager().getParams().setSoTimeout(60 * 1000);
        client.getParams().setContentCharset("GBK");
        log.info("设置连接超时时间为: " + this.connection_timeout + "毫秒");
    }


    /**
     * 初始化
     * 
     * @throws Exception
     */
    public void init() throws Exception {
        // do nothing
    }


    /**
     * 得到diamondSDKConfMaps首个配置的键值
     * 
     * @return 返回首个配置的键值
     */
    private String getSingleKey() {
        String singleKey = null;
        if (null != diamondSDKConfMaps) {
            singleKey = diamondSDKConfMaps.keySet().iterator().next();
        }
        return singleKey;
    }


    /**
     * 设置diamondSDKConfMaps配置集的map
     * 
     * @param diamondSDKConfMaps
     */
    public synchronized void setDiamondSDKConfMaps(final Map<String, DiamondSDKConf> diamondSDKConfMaps) {
        this.diamondSDKConfMaps = diamondSDKConfMaps;
    }


    // ///////////////////////////接口方法实现////////////////////////////////////////

    /**
     * 得到diamondSDKConfMaps配置集的map
     * 
     * @return
     */
    public synchronized Map<String, DiamondSDKConf> getDiamondSDKConfMaps() {
        return diamondSDKConfMaps;
    }


    /**
     * 使用指定的diamond来发送分组信息
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextRestult 单个对象
     */
    public synchronized ContextResult publishGroup(String address, String dataId, String groupName, String serverId) {
        ContextResult response = null;
        if (validate(address, dataId, groupName, serverId)) {
            response = this.processPublishGroupInfoByDefinedServerId(address, dataId, groupName, serverId);
            return response;
        }
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("请确保address,dataId,groupName,serverId不为空");
        return response;
    }


    /**
     * 使用指定的diamond来推送数据
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult 单个对象
     */
    public synchronized ContextResult create(String dataId, String groupName, String context, String serverId) {
        ContextResult response = null;
        // 进行dataId,groupName,context,serverId为空验证
        if (validate(dataId, groupName, context)) {
            // 向diamondserver发布数据
            // 已有方法, srcIp和srcUser传入null, 并且needProof为false
            response = this.processPulishByDefinedServerId(dataId, groupName, context, serverId, null, null, false);
            /*
             * if (response.isSuccess()) { // 暂停600ms，实时通知数据到达 try {
             * Thread.sleep(600); } catch (InterruptedException e) {
             * e.printStackTrace(); } }
             */
            return response;
        }

        // 未通过为空验证
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("请确保dataId,group,content不为空");
        return response;
    }


    public ContextResult publish(String dataId, String group, String content, String serverId, String srcIp,
            String srcUser) {
        ContextResult response = null;

        if (validate(dataId, group, content)) {
            // 新增方法, 传入参数中的srcIp和srcUser, needProof为true
            response = this.processPulishByDefinedServerId(dataId, group, content, serverId, srcIp, srcUser, true);
            return response;
        }

        // 未通过非空验证
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("请确保dataId,group,content不为空");
        return response;
    }


    /**
     * 使用首个配置的diamond来推送数据
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult 单个对象
     */
    public synchronized ContextResult pulishFromDefaultServer(String dataId, String groupName, String context) {
        ContextResult response = null;
        // 进行dataId,groupName,context为空验证
        if (validate(dataId, groupName, context)) {
            // 向diamondserver发布数据
            response = this.processPulishByFirstServerId(dataId, groupName, context);
            /*
             * if (response.isSuccess()) { try { Thread.sleep(600); } catch
             * (InterruptedException e) { e.printStackTrace(); } }
             */
            return response;
        }
        response = new ContextResult();
        // 未通过为空验证
        response.setSuccess(false);
        response.setStatusMsg("请确保dataId,groupName,context不为空");
        return response;
    }


    /**
     * 使用指定的diamond来移动分组信息
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 单个对象
     */

    public synchronized ContextResult moveGroup(long id, String groupName, String serverId) {
        ContextResult response = null;
        response = this.processPublishGroupInfoAfterModifiedByDefinedServerId(id, groupName, serverId);
        return response;
    }


    /**
     * 使用指定的diamond来推送修改后的数据
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult 单个对象
     */
    public synchronized ContextResult pulishAfterModified(String dataId, String groupName, String context,
            String serverId) {

        ContextResult response = null;
        // 进行dataId,groupName,context,serverId为空验证
        if (validate(dataId, groupName, context)) {
            // 向diamondserver发布修改数据
            // 已有方法, src_ip和src_user传入null, needProof为false
            response =
                    this.processPulishAfterModifiedByDefinedServerId(dataId, groupName, context, serverId, null, null,
                        false);
            /*
             * if (response.isSuccess()) { try { Thread.sleep(600); } catch
             * (InterruptedException e) { e.printStackTrace(); } }
             */
            return response;
        }
        else {
            response = new ContextResult();
            // 未通过为空验证
            response.setSuccess(false);
            response.setStatusMsg("请确保dataId,group,content不为空");
            return response;
        }

    }


    public ContextResult publishAfterModified(String dataId, String group, String content, String serverId,
            String srcIp, String srcUser) {
        ContextResult response = null;

        if (validate(dataId, group, content)) {
            // 新增方法, 传入参数中的srcIp和srcUser, needProof为true
            response =
                    this.processPulishAfterModifiedByDefinedServerId(dataId, group, content, serverId, srcIp, srcUser,
                        true);
            return response;
        }

        // 未通过非空验证
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("请确保dataId,group,content不为空");
        return response;

    }


    /**
     * 使用首个配置的 diamond来推送修改后的数据
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult 单个对象
     */
    public synchronized ContextResult pulishFromDefaultServerAfterModified(String dataId, String groupName,
            String context) {
        ContextResult response = null;
        // 进行dataId,groupName,context为空验证
        if (validate(dataId, groupName, context)) {
            // 向diamondserver发布修改数据
            response = this.processPulishAfterModifiedByFirstServerId(dataId, groupName, context);
            /*
             * if (response.isSuccess()) { try { Thread.sleep(600); } catch
             * (InterruptedException e) { e.printStackTrace(); } }
             */
            return response;
        }
        else {
            response = new ContextResult();
            // 未通过为空验证
            response.setSuccess(false);
            response.setStatusMsg("修改推送数据时请确保dataId,groupName,context不为空");
            return response;
        }

    }


    // -------------------------模糊查询-------------------------------//
    /**
     * 使用指定的diamond来模糊查询数据
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> 单个对象
     * @throws SQLException
     */
    public synchronized PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern,
            String serverId, long currentPage, long sizeOfPerPage) {
        return processQuery(dataIdPattern, groupNamePattern, null, serverId, currentPage, sizeOfPerPage);
    }


    /**
     * 使用首个配置的diamond来模糊查询数据
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> 单个对象
     * @throws SQLException
     */
    public synchronized PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern,
            String groupNamePattern, long currentPage, long sizeOfPerPage) {
        String serverKey = this.getSingleKey();
        return processQuery(dataIdPattern, groupNamePattern, null, serverKey, currentPage, sizeOfPerPage);
    }


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

    public synchronized PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern,
            String contentPattern, String serverId, long currentPage, long sizeOfPerPage) {
        return processQuery(dataIdPattern, groupNamePattern, contentPattern, serverId, currentPage, sizeOfPerPage);
    }


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
    public synchronized PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern,
            String groupNamePattern, String contentPattern, long currentPage, long sizeOfPerPage) {
        String serverKey = this.getSingleKey();
        return processQuery(dataIdPattern, groupNamePattern, contentPattern, serverKey, currentPage, sizeOfPerPage);
    }


    // =====================精确查询 ==================================
    /**
     * 使用指定的diamond和指定的address,dataId来查询分组信息
     * 
     * @param address
     * @param dataId
     * @param serverId
     * @return ContextResult
     * @throws SQLException
     */
    public synchronized ContextResult queryByAddressAndDataId(String address, String dataId, String serverId) {
        ContextResult result = new ContextResult();
        ContextResult ContextResult = processQuery(address, dataId, serverId);
        result.setStatusMsg(ContextResult.getStatusMsg());
        result.setSuccess(ContextResult.isSuccess());
        result.setStatusCode(ContextResult.getStatusCode());
        if (ContextResult.isSuccess()) {
            List<GroupInfo> list = ContextResult.getReceive();
            if (list != null && !list.isEmpty()) {
                GroupInfo info = list.iterator().next();
                result.setGroupInfo(info);
                result.setReceiveResult(info.getGroup());
                result.setStatusCode(ContextResult.getStatusCode());

            }
        }
        return result;

    }


    /**
     * 使用指定的diamond和指定的dataId,groupName来精确查询数据
     * 
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 单个对象
     * @throws SQLException
     */
    public synchronized ContextResult queryByDataIdAndGroupName(String dataId, String groupName, String serverId) {
        ContextResult result = new ContextResult();
        PageContextResult<ConfigInfo> pageContextResult = processQuery(dataId, groupName, null, serverId, 1, 1);
        result.setStatusMsg(pageContextResult.getStatusMsg());
        result.setSuccess(pageContextResult.isSuccess());
        result.setStatusCode(pageContextResult.getStatusCode());
        if (pageContextResult.isSuccess()) {
            List<ConfigInfo> list = pageContextResult.getDiamondData();
            if (list != null && !list.isEmpty()) {
                ConfigInfo info = list.iterator().next();
                result.setConfigInfo(info);
                result.setReceiveResult(info.getContent());
                result.setStatusCode(pageContextResult.getStatusCode());

            }
        }
        return result;
    }


    /**
     * 使用首个配置的diamond和指定的dataId,groupName来精确查询数据
     * 
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 单个对象
     * @throws SQLException
     */
    public synchronized ContextResult queryFromDefaultServerByDataIdAndGroupName(String dataId, String groupName) {
        return queryByDataIdAndGroupName(dataId, groupName, getSingleKey());
    }

    // ========================精确查询结束==================================

    // /////////////////////////私有工具对象定义和工具方法实现////////////////////////////////////////

    private final HttpClient client;


    public String getRealTimeDataId(String dataId) {
        return dataId + "." + CONFIG_CLIENT_PUBLISH_SUFFIX;
    }


    // =========================== 推送 ===============================
    /**
     * 使用指定的serverId发布分组信息
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 返回发送信息的结果
     */

    private ContextResult processPublishGroupInfoByDefinedServerId(String address, String dataId, String groupName,
            String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("使用processPublishGroupInfoByDefinedServerId(" + address + "," + dataId + "," + groupName
                    + ")发送分组信息。");
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=addGroup");
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            NameValuePair dataId_value = new NameValuePair("dataId", dataId);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair address_value = new NameValuePair("address", address);
            // 设置参数
            post.setRequestBody(new NameValuePair[] { address_value, dataId_value, group_value });
            // 配置对象
            GroupInfo groupInfo = new GroupInfo();
            groupInfo.setDataId(dataId);
            groupInfo.setGroup(groupName);
            groupInfo.setAddress(address);
            if (log.isDebugEnabled())
                log.debug("待推送的GroupInfo: " + groupInfo);
            // 添加一个配置对象到响应结果中
            response.setGroupInfo(groupInfo);
            // 执行方法并返回http状态码
            int status = client.executeMethod(post);
            response.setReceiveResult(post.getResponseBodyAsString());
            log.info("状态码：" + status + ",响应结果：" + post.getResponseBodyAsString());
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("推送分组信息处理成功");
                log.info("推送分组信息处理成功");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("推送分组信息处理超时，默认超时时间为:" + require_timeout + "毫秒");
                log.info("推送分组信息处理超时，默认超时时间为:" + require_timeout + "毫秒");
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("推送分组信息处理失败，失败原因请通过ContextResult的getReceiveResult()方法查看");
                log.info("推送分组信息处理失败:" + response.getReceiveResult());
            }
            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setStatusMsg("推送方法执行过程发生HttpException ,详细如下：" + e.getMessage());
            log.error("在推送方法processPulishByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生HttpException: "
                    + e.getMessage());
        }
        catch (IOException e) {
            response.setStatusMsg("推送方法执行过程发生IOException：" + e.getMessage());
            log.error("在推送方法processPulishByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生IOException: "
                    + e.getMessage());
        }
        finally {
            // 释放连接资源
            post.releaseConnection();
        }
        return response;

    }


    /**
     * 使用指定的serverId处理推送动作
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @param needProof
     *            是否需要server端处理srcIp和srcUser, 为true,
     *            server端会把这里传入的srcIp和srcUser存入数据库, 为false,
     *            server会把请求的IP作为srcIp和srcUser存入数据库
     * @return ContextResult 返回推送响应结果
     * 
     *         增加参数srcIp和srcUser 2012-03-23 leiwen.zh
     */
    private ContextResult processPulishByDefinedServerId(String dataId, String groupName, String context,
            String serverId, String srcIp, String srcUser, boolean needProof) {
        flowControl();
        ContextResult response = new ContextResult();
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("使用processPulishByDefinedServerId(" + dataId + "," + groupName + "," + context + "," + serverId
                    + ")进行推送");

        String postUrl = "/diamond-server/admin.do?method=postConfig";
        if (needProof) {
            postUrl = "/diamond-server/admin.do?method=postConfigNew";
        }
        PostMethod post = new PostMethod(postUrl);
        // 设置请求超时时间
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            NameValuePair dataId_value = new NameValuePair("dataId", dataId);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair content_value = new NameValuePair("content", context);
            // 将srcIp和srcUser也放到request body中
            NameValuePair src_ip_value = new NameValuePair("src_ip", srcIp);
            NameValuePair src_user_value = new NameValuePair("src_user", srcUser);

            // 设置参数
            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value, src_ip_value,
                                                     src_user_value });
            // 配置对象
            ConfigInfo configInfo = new ConfigInfo();
            configInfo.setDataId(dataId);
            configInfo.setGroup(groupName);
            configInfo.setContent(context);
            if (log.isDebugEnabled())
                log.debug("待推送的ConfigInfo: " + configInfo);
            // 添加一个配置对象到响应结果中
            response.setConfigInfo(configInfo);
            // 执行方法并返回http状态码
            int status = client.executeMethod(post);
            response.setReceiveResult(post.getResponseBodyAsString());
            log.info("状态码：" + status + ",响应结果：" + post.getResponseBodyAsString());
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("推送处理成功");
                log.info("推送处理成功, dataId=" + dataId + ",group=" + groupName + ",content=" + context + ",serverId="
                        + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser);
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("推送处理超时, 默认超时时间为:" + require_timeout + "毫秒");
                log.error("推送处理超时，默认超时时间为:" + require_timeout + "毫秒, dataId=" + dataId + ",group=" + groupName
                        + ",content=" + context + ",serverId=" + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("推送处理失败, 状态码为:" + status);
                log.error("推送处理失败:" + response.getReceiveResult() + ",dataId=" + dataId + ",group=" + groupName
                        + ",content=" + context + ",serverId=" + serverId);
            }
            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setStatusMsg("推送处理发生HttpException：" + e.getMessage());
            log.error("推送处理发生HttpException: dataId=" + dataId + ",group=" + groupName + ",content=" + context
                    + ",serverId=" + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser, e);
        }
        catch (IOException e) {
            response.setStatusMsg("推送处理发生IOException：" + e.getMessage());
            log.error("推送处理发生IOException: dataId=" + dataId + ",group=" + groupName + ",content=" + context
                    + ",serverId=" + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser, e);
        }
        finally {
            // 释放连接资源
            post.releaseConnection();
        }

        return response;
    }


    /**
     * 使用第一个serverId处理推送动作
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult 返回推送响应结果
     */
    private ContextResult processPulishByFirstServerId(String dataId, String groupName, String context) {
        flowControl();
        ContextResult response = new ContextResult();
        // 登录
        String serverId = this.getSingleKey();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是首个配置的serverId为空或不存在");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("使用processPulishByFirstServerId(" + dataId + "," + groupName + "," + context + ")进行推送");
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=postConfig");
        // 设置请求超时时间
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            NameValuePair dataId_value = new NameValuePair("dataId", dataId);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair content_value = new NameValuePair("content", context);
            // 设置参数
            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });
            // 配置对象
            ConfigInfo configInfo = new ConfigInfo();
            configInfo.setDataId(dataId);
            configInfo.setGroup(groupName);
            configInfo.setContent(context);
            if (log.isDebugEnabled())
                log.debug("待推送的ConfigInfo: " + configInfo);
            // 添加一个配置对象到响应结果中
            response.setConfigInfo(configInfo);
            // 执行方法并返回http状态码
            int status = client.executeMethod(post);
            response.setReceiveResult(post.getResponseBodyAsString());
            log.info("状态码：" + status + ",响应结果：" + post.getResponseBodyAsString());
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("使用第一个diamond server推送处理成功");
                log.info("使用第一个diamond server推送处理成功");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("使用第一个diamond server推送处理超时，默认超时时间为:" + require_timeout + "毫秒");
                log.error("使用第一个diamond server推送处理超时，默认超时时间为:" + require_timeout + "毫秒, dataId=" + dataId + ",group="
                        + groupName + ",content=" + context + ",serverId=" + serverId);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("使用第一个diamond server推送处理失败，失败原因请通过ContextResult的getReceiveResult()方法查看");
                log.error("使用第一个diamond server推送处理失败:" + response.getReceiveResult() + ",dataId=" + dataId + ",group="
                        + groupName + ",content=" + context + ",serverId=" + serverId);
            }
            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setStatusMsg("推送方法执行过程发生HttpException：" + e.getMessage());
            log.error(
                "在推送方法processPulishByFirstServerId(String dataId, String groupName, String context)执行过程中发生HttpException：dataId="
                        + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
        }
        catch (IOException e) {
            response.setStatusMsg("推送方法执行过程发生IOException：" + e.getMessage());
            log.error(
                "在推送方法processPulishByFirstServerId(String dataId, String groupName, String context)执行过程中发生IOException：dataId="
                        + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
        }
        finally {
            // 释放连接资源
            post.releaseConnection();
        }

        return response;
    }

    // =========================== 推送结束 ===============================

    // =========================== 修改 ===============================
    /**
     * 使用指定的serverId移动分组信息
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult 返回修改后的分组信息
     */
    static final String LIST_FORMAT_URL_GROUP = "/diamond-server/admin.do?method=moveGroup&id=%d&newGroup=%s";


    private ContextResult processPublishGroupInfoAfterModifiedByDefinedServerId(long id, String groupName,
            String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空");
            return response;
        }
        log.info("使用processPublishGroupInfoAfterModifiedByDefinedServerId(" + id + "," + groupName + "," + serverId
                + ")进行推送修改");

        String url = String.format(LIST_FORMAT_URL_GROUP, id, groupName);
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            int status = client.executeMethod(method);
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("推送分组信息修改处理成功");
                log.info("推送分组信息修改处理成功");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("推送分组信息修改处理超时，默认超时时间为:" + require_timeout + "毫秒");
                log.info("推送分组信息修改处理超时，默认超时时间为:" + require_timeout + "毫秒");
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("推送分组信息修改处理失败,失败原因请通过ContextResult的getReceiveResult()方法查看");
                log.info("推送分组信息修改处理失败:" + response.getReceiveResult());
            }

            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("推送修改方法执行过程发生HttpException：" + e.getMessage());
            log.error("在推送修改方法processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生HttpException ："
                    + e.getMessage());
            return response;
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("推送修改方法执行过程发生IOException：" + e.getMessage());
            log.error("在推送修改方法processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生IOException ："
                    + e.getMessage());
            return response;
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }


    /**
     * 使用指定的serverId处理推送修改动作
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @param needProof
     *            是否需要server端处理srcIp和srcUser, 为true,
     *            server端会把这里传入的srcIp和srcUser存入数据库, 为false,
     *            server会把请求的IP作为srcIp和srcUser存入数据库
     * @return ContextResult 返回推送修改的响应结果
     */
    private ContextResult processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,
            String serverId, String srcIp, String srcUser, boolean needProof) {
        flowControl();
        ContextResult response = new ContextResult();
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("使用processPulishAfterModifiedByDefinedServerId(" + dataId + "," + groupName + "," + context + ","
                    + serverId + ")进行推送修改");
        // 是否存在此dataId,groupName的数据记录
        ContextResult result = null;
        result = queryByDataIdAndGroupName(dataId, groupName, serverId);
        if (null == result || !result.isSuccess()) {
            response.setSuccess(false);
            response.setStatusMsg("找不到需要修改的数据记录，记录不存在!");
            log.warn("找不到需要修改的数据记录，记录不存在! dataId=" + dataId + ",group=" + groupName + ",serverId=" + serverId);
            return response;
        }
        // 有数据，则修改
        else {
            String postUrl = "/diamond-server/admin.do?method=updateConfig";
            if (needProof) {
                postUrl = "/diamond-server/admin.do?method=updateConfigNew";
            }
            PostMethod post = new PostMethod(postUrl);
            // 设置请求超时时间
            post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
            try {
                NameValuePair dataId_value = new NameValuePair("dataId", dataId);
                NameValuePair group_value = new NameValuePair("group", groupName);
                NameValuePair content_value = new NameValuePair("content", context);
                NameValuePair src_ip_value = new NameValuePair("src_ip", srcIp);
                NameValuePair src_user_value = new NameValuePair("src_user", srcUser);
                // 设置参数
                post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value, src_ip_value,
                                                         src_user_value });
                // 配置对象
                ConfigInfo configInfo = new ConfigInfo();
                configInfo.setDataId(dataId);
                configInfo.setGroup(groupName);
                configInfo.setContent(context);
                if (log.isDebugEnabled())
                    log.debug("待推送的修改ConfigInfo: " + configInfo);
                // 添加一个配置对象到响应结果中
                response.setConfigInfo(configInfo);
                // 执行方法并返回http状态码
                int status = client.executeMethod(post);
                response.setReceiveResult(post.getResponseBodyAsString());
                log.info("状态码：" + status + ",响应结果：" + post.getResponseBodyAsString());
                if (status == HttpStatus.SC_OK) {
                    response.setSuccess(true);
                    response.setStatusMsg("推送修改处理成功");
                    log.info("推送修改处理成功");
                }
                else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                    response.setSuccess(false);
                    response.setStatusMsg("推送修改处理超时，默认超时时间为:" + require_timeout + "毫秒");
                    log.error("推送修改处理超时，默认超时时间为:" + require_timeout + "毫秒, dataId=" + dataId + ",group=" + groupName
                            + ",content=" + context + ",serverId=" + serverId);
                }
                else {
                    response.setSuccess(false);
                    response.setStatusMsg("推送修改处理失败,失败原因请通过ContextResult的getReceiveResult()方法查看");
                    log.error("推送修改处理失败:" + response.getReceiveResult() + ",dataId=" + dataId + ",group=" + groupName
                            + ",content=" + context + ",serverId=" + serverId);
                }

                response.setStatusCode(status);
            }
            catch (HttpException e) {
                response.setSuccess(false);
                response.setStatusMsg("推送修改方法执行过程发生HttpException：" + e.getMessage());
                log.error(
                    "在推送修改方法processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生HttpException：dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            catch (IOException e) {
                response.setSuccess(false);
                response.setStatusMsg("推送修改方法执行过程发生IOException：" + e.getMessage());
                log.error(
                    "在推送修改方法processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生IOException：dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            finally {
                // 释放连接资源
                post.releaseConnection();
            }

            return response;
        }
    }


    /**
     * 使用第一个serverId处理推送修改动作
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult 返回推送修改的响应结果
     */
    private ContextResult processPulishAfterModifiedByFirstServerId(String dataId, String groupName, String context) {
        flowControl();
        ContextResult response = new ContextResult();
        // 登录
        String serverId = this.getSingleKey();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是首个配置的serverId为空");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("使用processPulishAfterModifiedByFirstServerId(" + dataId + "," + groupName + "," + context
                    + ")进行推送修改");
        // 是否存在此dataId,groupName的数据记录
        ContextResult result = null;
        result = queryFromDefaultServerByDataIdAndGroupName(dataId, groupName);
        if (null == result || !result.isSuccess()) {
            response.setSuccess(false);
            response.setStatusMsg("找不到需要修改的数据记录，记录不存在!");
            log.warn("找不到需要修改的数据记录，记录不存在! dataId=" + dataId + ",group=" + groupName);
            return response;
        }
        // 有数据，则修改
        else {
            PostMethod post = new PostMethod("/diamond-server/admin.do?method=updateConfig");
            // 设置请求超时时间
            post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
            try {
                NameValuePair dataId_value = new NameValuePair("dataId", dataId);
                NameValuePair group_value = new NameValuePair("group", groupName);
                NameValuePair content_value = new NameValuePair("content", context);
                // 设置参数
                post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });
                // 配置对象
                ConfigInfo configInfo = new ConfigInfo();
                configInfo.setDataId(dataId);
                configInfo.setGroup(groupName);
                configInfo.setContent(context);
                if (log.isDebugEnabled())
                    log.debug("待推送的修改ConfigInfo: " + configInfo);
                // 添加一个配置对象到响应结果中
                response.setConfigInfo(configInfo);
                // 执行方法并返回http状态码
                int status = client.executeMethod(post);
                response.setReceiveResult(post.getResponseBodyAsString());
                log.info("状态码：" + status + ",响应结果：" + post.getResponseBodyAsString());
                if (status == HttpStatus.SC_OK) {
                    response.setSuccess(true);
                    response.setStatusMsg("使用第一个diamond server推送修改处理成功");
                    log.info("使用第一个diamond server推送修改处理成功");
                }
                else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                    response.setSuccess(false);
                    response.setStatusMsg("使用第一个diamond server推送修改处理超时，默认超时时间为:" + require_timeout + "毫秒");
                    log.error("使用第一个diamond server推送修改处理超时，默认超时时间为:" + require_timeout + "毫秒, dataId=" + dataId
                            + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId);
                }
                else {
                    response.setSuccess(false);
                    response.setStatusMsg("使用第一个diamond server推送修改处理失败,失败原因请通过ContextResult的getReceiveResult()方法查看");
                    log.error("使用第一个diamond server推送修改处理失败:" + response.getReceiveResult() + ",dataId=" + dataId
                            + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId);
                }

                response.setStatusCode(status);
            }
            catch (HttpException e) {
                response.setSuccess(false);
                response.setStatusMsg("推送修改方法执行过程发生HttpException：" + e.getMessage());
                log.error(
                    "在推送修改方法processPulishAfterModifiedByFirstServerId(String dataId, String groupName, String context)执行过程中发生HttpException：dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            catch (IOException e) {
                response.setSuccess(false);
                response.setStatusMsg("推送修改方法执行过程发生IOException：" + e.getMessage());
                log.error(
                    "在推送修改方法processPulishAfterModifiedByFirstServerId(String dataId, String groupName, String context)执行过程中发生IOException：dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            finally {
                // 释放连接资源
                post.releaseConnection();
            }

            return response;
        }

    }

    // =========================== 修改结束 ===============================

    /**
     * 利用 httpclient实现页面登录
     * 
     * @return 登录结果 true:登录成功,false:登录失败
     */

    ReentrantLock clientLock = new ReentrantLock();


    private boolean login(String serverId) {
        // serverId 为空判断
        if (StringUtils.isEmpty(serverId) || StringUtils.isBlank(serverId))
            return false;
        DiamondSDKConf defaultConf = diamondSDKConfMaps.get(serverId);
        log.info("[login] 登录使用serverId:" + serverId + ",该环境对象属性：" + defaultConf);
        if (null == defaultConf)
            return false;
        RandomDiamondUtils util = new RandomDiamondUtils();
        // 初始化随机取值器
        util.init(defaultConf.getDiamondConfs());
        if (defaultConf.getDiamondConfs().size() == 0)
            return false;
        boolean flag = false;
        log.info("[randomSequence] 此次访问序列为: " + util.getSequenceToString());
        // 最多重试次数为：某个环境的所有已配置的diamondConf的长度
        while (util.getRetry_times() < util.getMax_times()) {

            // 得到随机取得的diamondConf
            DiamondConf diamondConf = util.generatorOneDiamondConf();
            log.info("第" + util.getRetry_times() + "次尝试:" + diamondConf);
            if (diamondConf == null)
                break;
            client.getHostConfiguration().setHost(diamondConf.getDiamondIp(),
                Integer.parseInt(diamondConf.getDiamondPort()), "http");
            PostMethod post = new PostMethod("/diamond-server/login.do?method=login");
            // 设置请求超时时间
            post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
            // 填充用户名，密码
            NameValuePair username_value = new NameValuePair("username", diamondConf.getDiamondUsername());
            NameValuePair password_value = new NameValuePair("password", diamondConf.getDiamondPassword());
            // 设置请求内容
            post.setRequestBody(new NameValuePair[] { username_value, password_value });
            log.info("使用diamondIp: " + diamondConf.getDiamondIp() + ",diamondPort: " + diamondConf.getDiamondPort()
                    + ",diamondUsername: " + diamondConf.getDiamondUsername() + ",diamondPassword: "
                    + diamondConf.getDiamondPassword() + "登录diamondServerUrl: [" + diamondConf.getDiamondConUrl() + "]");

            try {
                int state = client.executeMethod(post);
                log.info("登录返回状态码：" + state);
                // 状态码为200，则登录成功,跳出循环并返回true
                if (state == HttpStatus.SC_OK) {
                    log.info("第" + util.getRetry_times() + "次尝试成功");
                    flag = true;
                    break;
                }

            }
            catch (HttpException e) {
                log.error("登录过程发生HttpException", e);
            }
            catch (IOException e) {
                log.error("登录过程发生IOException", e);
            }
            finally {
                post.releaseConnection();
            }
        }
        if (flag == false) {
            log.error("造成login失败的原因可能是：所有diamondServer的配置环境目前均不可用．serverId=" + serverId);
        }
        return flag;
    }

    static final String LIST_FORMAT_URL =
            "/diamond-server/admin.do?method=listConfig&group=%s&dataId=%s&pageNo=%d&pageSize=%d";
    static final String LIST_LIKE_FORMAT_URL =
            "/diamond-server/admin.do?method=listConfigLike&group=%s&dataId=%s&pageNo=%d&pageSize=%d";


    /**
     * 处理分组查询
     * 
     * @param address
     * @param dataId
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return
     */
    private ContextResult processQuery(String address, String dataId, String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        String url = "/diamond-server/admin.do?method=listGroup";
        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                try {
                    String json = getContent(method).trim();
                    List<GroupInfo> list = null;

                    if (!json.equals("null")) {
                        list =
                                (List<GroupInfo>) JSONUtils.deserializeObject(json,
                                    new TypeReference<List<GroupInfo>>() {
                                    });
                    }
                    // List<String> list = new ArrayList<String>();
                    List<GroupInfo> list1 = new ArrayList<GroupInfo>();
                    if (list != null) {
                        // for (Entry<String, Map<String, GroupInfo>> entry :
                        // map.entrySet())
                        // // {
                        // // list.add(entry.getKey());
                        // for(Entry<String,GroupInfo> inner:
                        // entry.getValue().entrySet()){
                        // // list.add(inner.getKey());
                        // // list.add(inner.getValue());
                        // // }
                        // list1.add(inner.getValue());
                        // }
                        for (Iterator<GroupInfo> iter = list.iterator(); iter.hasNext();) {
                            GroupInfo info = (GroupInfo) iter.next();
                            if (info.getAddress().equals(address) && info.getDataId().equals(dataId)) {
                                list1.add(info);
                                break;
                            }
                        }
                        if (list1.size() != 0) {
                            response.setSuccess(true);
                            response.setStatusMsg("指定diamond的分组信息查询完成");
                            response.setReceive(list1);
                            log.info("指定diamond的分组信息查询完成");
                        }
                        else {
                            response.setReceive(null);
                            response.setSuccess(false);
                            response.setStatusMsg("查询结果：无");
                            log.info("查询结果：无");
                        }
                    }
                    else {
                        response.setReceive(null);
                        response.setSuccess(false);
                        response.setStatusMsg("查询结果：无");
                        log.info("查询结果：无");
                    }
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("反序列化失败,错误信息为：" + e.getLocalizedMessage());
                    log.error("反序列化page对象失败", e);
                }
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("查询数据超时" + require_timeout + "毫秒");
                log.error("查询数据超时，默认超时时间为:" + require_timeout + "毫秒");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("查询数据出错，服务器返回状态码为" + status);
                log.error("查询数据出错，状态码为：" + status);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("查询数据出错,错误信息如下：" + e.getMessage());
            log.error("查询数据出错", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("查询数据出错,错误信息如下：" + e.getMessage());
            log.error("查询数据出错", e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }


    /**
     * 查询所有分组
     * 
     * @param serverId
     * @return
     */
    public ContextResult queryAllGroup(String serverId) {
        ContextResult response;
        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> map;
        GetMethod method;
        response = new ContextResult();
        map = new ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        String url = "/diamond-server/admin.do?method=listGroup";
        method = new GetMethod(url);
        configureGetMethod(method);
        try {
            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                try {
                    String json = getContent(method).trim();
                    List<GroupInfo> list = null;
                    if (!json.equals("null"))
                        if (!json.equals("null")) {
                            list =
                                    (List<GroupInfo>) JSONUtils.deserializeObject(json,
                                        new TypeReference<List<GroupInfo>>() {
                                        });
                        }

                    if (list != null) {
                        for (Iterator iter = list.iterator(); iter.hasNext();) {
                            GroupInfo info = (GroupInfo) iter.next();
                            ConcurrentHashMap<String, GroupInfo> data = map.get(info.getAddress());
                            if (data == null) {
                                ConcurrentHashMap<String, GroupInfo> newMap =
                                        new ConcurrentHashMap<String, GroupInfo>();
                                map.putIfAbsent(info.getAddress(), newMap);
                                data = newMap;
                            }
                            GroupInfo groupInfo = (GroupInfo) data.get(info.getDataId());
                            if (groupInfo == null)
                                data.putIfAbsent(info.getDataId(), info);
                        }

                    }
                    log.info(map.toString());
                    response.setSuccess(true);
                    response.setStatusMsg("反序列化完成！");
                    response.setMap(map);
                    break;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    response.setSuccess(false);
                    response.setStatusMsg("反序列化失败,错误信息为：" + e.getLocalizedMessage());
                    log.error("反序列化page对象失败", e);
                }
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("查询数据超时" + require_timeout + "毫秒");
                log.error("查询数据超时，默认超时时间为:" + require_timeout + "毫秒");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("查询数据出错，服务器返回状态码为" + status);
                log.error("查询数据出错，状态码为：" + status);
                break;
            }
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("查询数据出错,错误信息如下：" + e.getMessage());
            log.error("查询数据出错", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("查询数据出错,错误信息如下：" + e.getMessage());
            log.error("查询数据出错", e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }


    /**
     * 处理查询
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param contentPattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return
     */
    @SuppressWarnings("unchecked")
    private PageContextResult<ConfigInfo> processQuery(String dataIdPattern, String groupNamePattern,
            String contentPattern, String serverId, long currentPage, long sizeOfPerPage) {
        flowControl();
        PageContextResult<ConfigInfo> response = new PageContextResult<ConfigInfo>();
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("使用processQuery(" + dataIdPattern + "," + groupNamePattern + "," + contentPattern + ","
                    + serverId + ")进行查询");
        boolean hasPattern =
                PatternUtils.hasCharPattern(dataIdPattern) || PatternUtils.hasCharPattern(groupNamePattern)
                        || PatternUtils.hasCharPattern(contentPattern);
        String url = null;
        if (hasPattern) {
            if (!StringUtils.isBlank(contentPattern)) {
                log.warn("注意, 正在根据内容来进行模糊查询, dataIdPattern=" + dataIdPattern + ",groupNamePattern=" + groupNamePattern
                        + ",contentPattern=" + contentPattern);
                // 模糊查询内容，全部查出来
                url = String.format(LIST_LIKE_FORMAT_URL, groupNamePattern, dataIdPattern, 1, Integer.MAX_VALUE);
            }
            else
                url = String.format(LIST_LIKE_FORMAT_URL, groupNamePattern, dataIdPattern, currentPage, sizeOfPerPage);
        }
        else {
            url = String.format(LIST_FORMAT_URL, groupNamePattern, dataIdPattern, currentPage, sizeOfPerPage);
        }

        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                String json = "";
                try {
                    json = getContent(method).trim();

                    Page<ConfigInfo> page = null;

                    if (!json.equals("null")) {
                        page =
                                (Page<ConfigInfo>) JSONUtils.deserializeObject(json,
                                    new TypeReference<Page<ConfigInfo>>() {
                                    });
                    }
                    if (page != null) {
                        List<ConfigInfo> diamondData = page.getPageItems();
                        if (!StringUtils.isBlank(contentPattern)) {
                            Pattern pattern = Pattern.compile(contentPattern.replaceAll("\\*", ".*"));
                            List<ConfigInfo> newList = new ArrayList<ConfigInfo>();
                            // 强制排序
                            Collections.sort(diamondData);
                            int totalCount = 0;
                            long begin = sizeOfPerPage * (currentPage - 1);
                            long end = sizeOfPerPage * currentPage;
                            for (ConfigInfo configInfo : diamondData) {
                                if (configInfo.getContent() != null) {
                                    Matcher m = pattern.matcher(configInfo.getContent());
                                    if (m.find()) {
                                        // 只添加sizeOfPerPage个
                                        if (totalCount >= begin && totalCount < end) {
                                            newList.add(configInfo);
                                        }
                                        totalCount++;
                                    }
                                }
                            }
                            page.setPageItems(newList);
                            page.setTotalCount(totalCount);
                        }
                        response.setOriginalDataSize(diamondData.size());
                        response.setTotalCounts(page.getTotalCount());
                        response.setCurrentPage(currentPage);
                        response.setSizeOfPerPage(sizeOfPerPage);
                    }
                    else {
                        response.setOriginalDataSize(0);
                        response.setTotalCounts(0);
                        response.setCurrentPage(currentPage);
                        response.setSizeOfPerPage(sizeOfPerPage);
                    }
                    response.operation();
                    List<ConfigInfo> pageItems = new ArrayList<ConfigInfo>();
                    if (page != null) {
                        pageItems = page.getPageItems();
                    }
                    response.setDiamondData(pageItems);
                    response.setSuccess(true);
                    response.setStatusMsg("指定diamond的查询完成");
                    log.info("指定diamond的查询完成, url=" + url);
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("反序列化失败,错误信息为：" + e.getLocalizedMessage());
                    log.error("反序列化page对象失败, dataId=" + dataIdPattern + ",group=" + groupNamePattern + ",serverId="
                            + serverId + ",json=" + json, e);
                }
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("查询数据超时" + require_timeout + "毫秒");
                log.error("查询数据超时，默认超时时间为:" + require_timeout + "毫秒, dataId=" + dataIdPattern + ",group="
                        + groupNamePattern + ",serverId=" + serverId);
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("查询数据出错，服务器返回状态码为" + status);
                log.error("查询数据出错，状态码为：" + status + ",dataId=" + dataIdPattern + ",group=" + groupNamePattern
                        + ",serverId=" + serverId);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("查询数据出错,错误信息如下：" + e.getMessage());
            log.error("查询数据出错, dataId=" + dataIdPattern + ",group=" + groupNamePattern + ",serverId=" + serverId, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("查询数据出错,错误信息如下：" + e.getMessage());
            log.error("查询数据出错, dataId=" + dataIdPattern + ",group=" + groupNamePattern + ",serverId=" + serverId, e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }


    /**
     * 查看是否为压缩的内容
     * 
     * @param httpMethod
     * @return
     */
    boolean isZipContent(HttpMethod httpMethod) {
        if (null != httpMethod.getResponseHeader(Constants.CONTENT_ENCODING)) {
            String acceptEncoding = httpMethod.getResponseHeader(Constants.CONTENT_ENCODING).getValue();
            if (acceptEncoding.toLowerCase().indexOf("gzip") > -1) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取Response的配置信息
     * 
     * @param httpMethod
     * @return
     */
    String getContent(HttpMethod httpMethod) throws UnsupportedEncodingException {
        StringBuilder contentBuilder = new StringBuilder();
        if (isZipContent(httpMethod)) {
            // 处理压缩过的配置信息的逻辑
            InputStream is = null;
            GZIPInputStream gzin = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                is = httpMethod.getResponseBodyAsStream();
                gzin = new GZIPInputStream(is);
                isr = new InputStreamReader(gzin, ((HttpMethodBase) httpMethod).getResponseCharSet()); // 设置读取流的编码格式，自定义编码
                br = new BufferedReader(isr);
                char[] buffer = new char[4096];
                int readlen = -1;
                while ((readlen = br.read(buffer, 0, 4096)) != -1) {
                    contentBuilder.append(buffer, 0, readlen);
                }
            }
            catch (Exception e) {
                log.error("解压缩失败", e);
            }
            finally {
                try {
                    br.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    isr.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    gzin.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    is.close();
                }
                catch (Exception e1) {
                    // ignore
                }
            }
        }
        else {
            // 处理没有被压缩过的配置信息的逻辑
            String content = null;
            try {
                content = httpMethod.getResponseBodyAsString();
            }
            catch (Exception e) {
                log.error("获取配置信息失败", e);
            }
            if (null == content) {
                return null;
            }
            contentBuilder.append(content);
        }
        return StringEscapeUtils.unescapeHtml(contentBuilder.toString());
    }


    private void configureGetMethod(GetMethod method) {
        method.addRequestHeader(Constants.ACCEPT_ENCODING, "gzip,deflate");
        method.addRequestHeader("Accept", "application/json");
        // 设置请求超时时间
        method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
    }


    /**
     * 字段dataId,groupName,context为空验证,有一个为空立即返回false
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return
     */
    private boolean validate(String dataId, String groupName, String context) {
        if (StringUtils.isEmpty(dataId) || StringUtils.isEmpty(groupName) || StringUtils.isEmpty(context)
                || StringUtils.isBlank(dataId) || StringUtils.isBlank(groupName) || StringUtils.isBlank(context))
            return false;
        return true;
    }


    /**
     * 字段dataId,groupName,context,serverId为空验证,有一个为空立即返回false
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return
     */
    private boolean validate(String dataId, String groupName, String context, String serverId) {
        if (StringUtils.isEmpty(dataId) || StringUtils.isEmpty(groupName) || StringUtils.isEmpty(context)
                || StringUtils.isEmpty(serverId) || StringUtils.isBlank(dataId) || StringUtils.isBlank(groupName)
                || StringUtils.isBlank(context) || StringUtils.isBlank(serverId))
            return false;
        return true;
    }


    public synchronized ContextResult unpublish(String serverId, long id) {
        return processDelete(serverId, id);
    }


    public synchronized ContextResult unpublishFromDefaultServer(long id) {
        String serverKey = this.getSingleKey();
        return processDelete(serverKey, id);
    }


    public synchronized ContextResult deleteGroup(String serverId, long id) {
        return processDeleteGroup(serverId, id);
    }


    /**
     * 按照指定的id删除分组信息
     * 
     * @param serverId
     * @param id
     * @return
     */
    private ContextResult processDeleteGroup(String serverId, long id) {
        ContextResult response = new ContextResult();
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        String url = "/diamond-server/admin.do?method=deleteGroup&id=" + id;
        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                response.setSuccess(true);
                response.setStatusMsg("删除成功");
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("删除分组信息超时" + require_timeout + "毫秒");
                log.error("删除分组信息超时，默认超时时间为:" + require_timeout + "毫秒");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("删除分组信息出错，服务器返回状态码为" + status);
                log.error("删除分组信息出错，状态码为：" + status);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("删除分组信息出错,错误信息如下：" + e.getMessage());
            log.error("删除分组信息出错", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("删除分组信息出错,错误信息如下：" + e.getMessage());
            log.error("删除分组信息出错", e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }


    // /**
    // * 处理删除分组信息
    // * @param serverId
    // * @param id
    // * @return
    // */
    //
    // private ContextResult processDeleteGroup(String address, String dataId,
    // String serverId){
    // ContextResult response = new ContextResult();
    // // 登录
    // if (!login(serverId)) {
    // response.setSuccess(false);
    // response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
    // return response;
    // }
    // ContextResult result = null;
    // result = queryByAddressAndDataId(address, dataId, serverId);
    // long id = result.getGroupInfo().getId();
    // if (null == result || !result.isSuccess()) {
    // response.setSuccess(false);
    // response.setStatusMsg("找不到需要删除的分组信息，记录不存在！");
    // log.warn("找不到需要删除的分组信息，记录不存在！");
    // return response;
    // }
    // log.info("使用processDeleteGroup(" + serverId + "," + id);
    // String url = "/diamond-server/admin.do?method=deleteGroup&id=" + id;
    // GetMethod method = new GetMethod(url);
    // configureGetMethod(method);
    // try {
    //
    // int status = client.executeMethod(method);
    // response.setStatusCode(status);
    // switch (status) {
    // case HttpStatus.SC_OK:
    // response.setSuccess(true);
    // response.setStatusMsg("删除成功");
    // break;
    // case HttpStatus.SC_REQUEST_TIMEOUT:
    // response.setSuccess(false);
    // response.setStatusMsg("删除分组信息超时" + require_timeout + "毫秒");
    // log.error("删除分组信息超时，默认超时时间为:" + require_timeout + "毫秒");
    // break;
    // default:
    // response.setSuccess(false);
    // response.setStatusMsg("删除分组信息出错，服务器返回状态码为" + status);
    // log.error("删除分组信息出错，状态码为：" + status);
    // break;
    // }
    //
    // }
    // catch (HttpException e) {
    // response.setSuccess(false);
    // response.setStatusMsg("删除分组信息出错,错误信息如下：" + e.getStackTrace());
    // log.error("删除分组信息出错", e);
    // }
    // catch (IOException e) {
    // response.setSuccess(false);
    // response.setStatusMsg("删除分组信息出错,错误信息如下：" + e.getStackTrace());
    // log.error("删除分组信息出错", e);
    // }
    // finally {
    // // 释放连接资源
    // method.releaseConnection();
    // }
    //
    // return response;
    // }
    /**
     * 处理删除
     * 
     * @param serverId
     * @param id
     * @return
     */
    private ContextResult processDelete(String serverId, long id) {
        flowControl();
        ContextResult response = new ContextResult();
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        log.info("使用processDelete(" + serverId + "," + id);
        String url = "/diamond-server/admin.do?method=deleteConfig&id=" + id;
        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                response.setSuccess(true);
                response.setReceiveResult(getContent(method));
                response.setStatusMsg("删除成功, url=" + url);
                log.warn("删除配置数据成功, url=" + url);
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("删除数据超时" + require_timeout + "毫秒");
                log.error("删除数据超时，默认超时时间为:" + require_timeout + "毫秒, id=" + id + ",serverId=" + serverId);
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("删除数据出错，服务器返回状态码为" + status);
                log.error("删除数据出错，状态码为：" + status + ", id=" + id + ",serverId=" + serverId);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("删除数据出错,错误信息如下：" + e.getMessage());
            log.error("删除数据出错, id=" + id + ",serverId=" + serverId, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("删除数据出错,错误信息如下：" + e.getMessage());
            log.error("删除数据出错, id=" + id + ",serverId=" + serverId, e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }


    // /////////////////////////////////////加载分组信息接口实现/////////////////////////
    public synchronized ContextResult reloadGroup(String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空或不存在");
            return response;
        }
        String url = "/diamond-server/admin.do?method=reloadGroup";
        GetMethod method = new GetMethod(url);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                response.setSuccess(true);
                response.setStatusMsg("更新分组成功");
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("更新分组信息超时" + require_timeout + "毫秒");
                log.error("更新分组信息超时，默认超时时间为:" + require_timeout + "毫秒");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("更新分组信息出错，服务器返回状态码为" + status);
                log.error("更新分组信息出错，状态码为：" + status);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("更新分组信息出错,错误信息如下：" + e.getMessage());
            log.error("更新分组信息出错", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("更新分组信息出错,错误信息如下：" + e.getMessage());
            log.error("更行分组信息出错", e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }

        return response;
    }

    // /////////////////////////////////流控方法实现///////////////////////////////////////

    static final int FLOW_CONTROL_THRESHOLD = Integer.parseInt(System.getProperty("diamond.sdk.flow_control_threshold",
        "100"));


    private void flowControl() {
        flowData.addAndGet(1);
        // 超过留空即sleep
        while (flowData.getAverageCount() > FLOW_CONTROL_THRESHOLD) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }


    /**
     * 判断指定dataId和group的数据是否存在
     */
    public synchronized boolean exists(String dataId, String group, String serverId) {
        // 流控
        flowControl();
        // 登录
        if (!login(serverId)) {
            log.error("登录失败, 原因请查看客户端日志");
            throw new RuntimeException("调用exists(), 登录diamond-server失败, dataId=" + dataId + ",group=" + group
                    + ",serverId=" + serverId);
        }

        // 构造请求url
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("/diamond-server/config.co?");
        urlBuilder.append("dataId=").append(dataId).append("&");
        urlBuilder.append("group=").append(group);

        // 创建HTTP method
        GetMethod method = new GetMethod(urlBuilder.toString());
        configureGetMethod(method);

        try {
            // 执行HTTP method
            int status = client.executeMethod(method);
            switch (status) {
            case HttpStatus.SC_OK:
                log.info("调用exists(), 数据存在, dataId=" + dataId + ",group=" + group);
                return true;

            case HttpStatus.SC_NOT_FOUND:
                log.info("调用exists(), 数据不存在, dataId=" + dataId + ",group=" + group);
                return false;

            default:
                log.error("调用exists(), 查询数据是否存在发生错误, HTTP状态码=" + status + ",dataId=" + dataId + ",group=" + group);
                throw new RuntimeException("调用exists(), 查询数据是否存在发生错误, HTTP状态码=" + status + ",dataId=" + dataId
                        + ",group=" + group);
            }
        }
        catch (Exception e) {
            log.error("调用exists(), 查询数据是否存在发生错误, dataId=" + dataId + ",group=" + group, e);
            throw new RuntimeException(e);
        }
        finally {
            // 释放连接资源
            method.releaseConnection();
        }
    }


    // /////////////////////////////////zhidao.2011/07/13///////////////////////////////////////

    // ================================ 批量接口重写, leiwen.zh, 2012.3.3
    // ============================== //

    /**
     * 批量更新或新增, 已过时, 请使用batchAdd()或batchUpdate()
     */
    @Deprecated
    public ContextResult createOrUpdate(String groupName, List<String> dataIds, List<String> contents, String serverId) {
        flowControl();
        ContextResult response = new ContextResult();
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("登录失败,造成错误的原因可能是指定的serverId为空");
            return response;
        }

        PostMethod post = new PostMethod("/diamond-server/admin.do?method=updateOrCreateConfig");
        // 设置请求超时时间
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            String dataIdParams = Joiner.on(",").join(dataIds);
            String contentParams = Joiner.on(",").join(contents);

            NameValuePair dataId_value = new NameValuePair("dataIds", dataIdParams);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair content_value = new NameValuePair("contents", contentParams);
            // 设置参数
            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });
            // 配置对象

            // 添加一个配置对象到响应结果中

            // 执行方法并返回http状态码
            int status = client.executeMethod(post);
            String json = post.getResponseBodyAsString();
            try {
                // List<ConfigInfo> configInfos =
                // (List<ConfigInfo>)JSONUtils.deserializeObject(json,List.class);
                response.setReceiveResult(json);

            }
            catch (Exception e) {
                response.setSuccess(false);
            }
            // response.setReceiveResult(HtmlParserUtils.getErrorBody(post.getResponseBodyAsString()));

            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("推送修改处理成功");
                log.info("推送修改处理成功");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("推送修改处理超时，默认超时时间为:" + require_timeout + "毫秒");
                log.info("推送修改处理超时，默认超时时间为:" + require_timeout + "毫秒");
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("推送修改处理失败,失败原因请通过ContextResult的getReceiveResult()方法查看");
                log.info("推送修改处理失败:" + response.getReceiveResult());
            }

            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("推送修改方法执行过程发生HttpException：" + e.getMessage());
            log.error("在推送修改方法processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生HttpException ："
                    + e.getMessage());
            return response;
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("推送修改方法执行过程发生IOException：" + e.getMessage());
            log.error("在推送修改方法processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)执行过程中发生IOException ："
                    + e.getMessage());
            return response;
        }
        finally {
            // 释放连接资源
            post.releaseConnection();
        }

        return response;

    }


    public synchronized BatchContextResult<ConfigInfoEx> batchAddOrUpdate(String serverId, String groupName,
            String srcIp, String srcUser, Map<String/* dataId */, String/* content */> dataId2ContentMap) {
        // 流控
        flowControl();
        // 创建返回结果
        BatchContextResult<ConfigInfoEx> response = new BatchContextResult<ConfigInfoEx>();
        // 将dataId和content的map处理为用一个不可见字符分隔的字符串
        List<String> dataIdAndContentList = new LinkedList<String>();
        List<String> dataIdList = new LinkedList<String>();
        for (String dataId : dataId2ContentMap.keySet()) {
            String content = dataId2ContentMap.get(dataId);
            dataIdAndContentList.add(dataId + Constants.WORD_SEPARATOR + content);
            dataIdList.add(dataId);
        }
        String allDataIdAndContent = Joiner.on(Constants.LINE_SEPARATOR).join(dataIdAndContentList);
        String allDataId = Joiner.on(Constants.WORD_SEPARATOR).join(dataIdAndContentList);

        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("批量写操作,登录失败,造成错误的原因可能是指定的serverId为空, serverId=" + serverId);
            return response;
        }

        // 构造HTTP method
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=batchAddOrUpdate");
        // 设置请求超时时间
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            // 设置参数
            NameValuePair dataId_value = new NameValuePair("allDataIdAndContent", allDataIdAndContent);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair src_ip_value = new NameValuePair("src_ip", srcIp);
            NameValuePair src_user_value = new NameValuePair("src_user", srcUser);

            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, src_ip_value, src_user_value });

            // 执行方法并返回http状态码
            int status = client.executeMethod(post);
            response.setStatusCode(status);

            if (status == HttpStatus.SC_OK) {
                String json = null;
                try {
                    json = post.getResponseBodyAsString();

                    // 反序列化json字符串, 并将结果处理后放入BatchContextResult中
                    List<ConfigInfoEx> configInfoExList = new LinkedList<ConfigInfoEx>();
                    Object resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
                    });
                    if (!(resultObj instanceof List<?>)) {
                        throw new RuntimeException("批量写操作,反序列化后的结果不是List类型, json=" + json);
                    }
                    List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
                    for (ConfigInfoEx configInfoEx : resultList) {
                        configInfoExList.add(configInfoEx);
                    }
                    response.getResult().addAll(configInfoExList);
                    // 反序列化成功, 本次批量操作成功
                    response.setStatusMsg("批量写操作,请求成功, serverId=" + serverId + ",allDataId=" + allDataId + ",group="
                            + groupName);
                    log.info("批量写操作成功,serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent + ",group="
                            + groupName + "\njson=" + json);
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("批量写操作,反序列化失败, serverId=" + serverId + ",allDataId=" + allDataId + ",group="
                            + groupName);
                    log.error("批量写操作,反序列化失败, serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent
                            + ",group=" + groupName + "\njson=" + json, e);
                }
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("批量写操作,处理超时，默认超时时间为:" + require_timeout + "毫秒, serverId=" + serverId
                        + ",allDataId=" + allDataId + ",group=" + groupName);
                log.error("批量写操作,处理超时，默认超时时间为:" + require_timeout + "毫秒, serverId=" + serverId
                        + ",allDataIdAndContent=" + allDataIdAndContent + ",group=" + groupName);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("批量写操作,处理失败, HTTP状态码=" + status + "serverId=" + serverId + ",allDataId="
                        + allDataId + ",group=" + groupName);
                log.error("批量写操作,处理失败,状态码:" + status + ",serverId=" + serverId + ",allDataIdAndContent="
                        + allDataIdAndContent + ",group=" + groupName);
            }
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("批量写操作,发生HttpException：" + e.getMessage());
            log.error("批量写操作出错, serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent + ",group="
                    + groupName, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("批量写操作,发生IOException：" + e.getMessage());
            log.error("批量写操作出错, serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent + ",group="
                    + groupName, e);
        }
        finally {
            // 释放连接资源
            post.releaseConnection();
        }

        return response;
    }


    public synchronized BatchContextResult<ConfigInfoEx> batchQuery(String serverId, String groupName,
            List<String> dataIds) {
        // 流控
        flowControl();
        // 创建返回结果
        BatchContextResult<ConfigInfoEx> response = new BatchContextResult<ConfigInfoEx>();
        // 将dataId的list处理为用一个不可见字符分隔的字符串
        String dataIdStr = Joiner.on(Constants.WORD_SEPARATOR).join(dataIds);
        // 登录
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("批量查询登录失败,造成错误的原因可能是指定的serverId为空, serverId=" + serverId);
            return response;
        }

        // 构造HTTP method
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=batchQuery");
        // 设置请求超时时间
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            // 设置参数
            NameValuePair dataId_value = new NameValuePair("dataIds", dataIdStr);
            NameValuePair group_value = new NameValuePair("group", groupName);

            post.setRequestBody(new NameValuePair[] { dataId_value, group_value });

            // 执行方法并返回http状态码
            int status = client.executeMethod(post);
            response.setStatusCode(status);

            if (status == HttpStatus.SC_OK) {
                String json = null;
                try {
                    json = post.getResponseBodyAsString();

                    // 反序列化json字符串, 并将结果处理后放入BatchContextResult中
                    List<ConfigInfoEx> configInfoExList = new LinkedList<ConfigInfoEx>();
                    Object resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
                    });
                    if (!(resultObj instanceof List<?>)) {
                        throw new RuntimeException("批量查询,反序列化后的结果不是List类型, json=" + json);
                    }
                    List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
                    for (ConfigInfoEx configInfoEx : resultList) {
                        configInfoExList.add(configInfoEx);
                    }
                    response.getResult().addAll(configInfoExList);

                    // 反序列化成功, 本次批量查询成功
                    response.setSuccess(true);
                    response.setStatusMsg("批量查询请求成功, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group="
                            + groupName);
                    log.info("批量查询请求成功, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group=" + groupName
                            + "\njson=" + json);
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("批量查询反序列化失败, serverId=" + serverId + ",dataIdStr=" + dataIdStr + ",group="
                            + groupName);
                    log.error("批量查询反序列化失败, serverId=" + serverId + ",dataIdStr=" + dataIdStr + ",group=" + groupName
                            + "\njson=" + json, e);
                }

            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("批量查询处理超时，默认超时时间为:" + require_timeout + "毫秒, serverId=" + serverId + ",dataIds="
                        + dataIdStr + ",group=" + groupName);
                log.error("批量查询处理超时，默认超时时间为:" + require_timeout + "毫秒, serverId=" + serverId + ",dataIds=" + dataIdStr
                        + ",group=" + groupName);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("批量查询处理失败, HTTP状态码=" + status + ",serverId=" + serverId + ",dataIds=" + dataIdStr
                        + ",group=" + groupName);
                log.error("批量查询处理失败, 状态码:" + status + ",serverId=" + serverId + ",dataIds=" + dataIdStr + ",group="
                        + groupName);
            }
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("批量查询发生HttpException：" + e.getMessage());
            log.error("批量查询出错, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group=" + groupName, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("批量查询发生IOException：" + e.getMessage());
            log.error("批量查询出错, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group=" + groupName, e);
        }
        finally {
            // 释放连接资源
            post.releaseConnection();
        }

        return response;
    }


    /**
     * 不关心请求到底是新增还是更新的方法, 已过时
     * 
     * 请使用pulish(String dataId, String groupName, String content, String
     * serverId)
     */
    @SuppressWarnings("serial")
    @Deprecated
    public ContextResult createOrUpdate(String groupName, String dataId, final String content, String serverId) {
        final String dataId2 = dataId;
        final String content2 = content;
        return createOrUpdate(groupName, new ArrayList<String>() {
            {
                add(dataId2);
            }
        }, new ArrayList<String>() {
            {
                add(content2);
            }
        }, serverId);

    }


    /**
     * 不关心请求是新增还是更新的方法
     */
    public synchronized ContextResult pulish(String dataId, String groupName, String content, String serverId) {
        ContextResult cr = this.queryByDataIdAndGroupName(dataId, groupName, serverId);

        ConfigInfo ci = cr.getConfigInfo();
        if (ci == null) {
            log.info("调用pulish()新增, dataId=" + dataId + ",group=" + groupName + ",content=" + content + ",serverId="
                    + serverId);
            return this.create(dataId, groupName, content, serverId);
        }
        else {
            log.info("调用pulish()更新, dataId=" + dataId + ",group=" + groupName + ",content=" + content + ",serverId="
                    + serverId);
            return this.pulishAfterModified(dataId, groupName, content, serverId);
        }
    }

}
