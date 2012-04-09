/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.client;

import com.taobao.pushit.client.PushitClient;


/**
 * 发布者接口
 * 
 * @author leiwen
 * 
 */
public interface DiamondPublisher {

    /**
     * 设置集群标识
     * 
     * @param clusterType
     */
    void setClusterType(String clusterType);


    /**
     * 增量发布数据, 使用默认的pattern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publish(String dataId, String group, String configInfo);


    /**
     * 增量发布数据，按照pattern提取content的唯一性标识
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param pattern
     */
    void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern);


    /**
     * 同步增量发布数据, 使用默认的pattern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout);


    /**
     * 同步增量发布数据, 按照pattern提取content的唯一性标识
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param pattern
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout, ContentIdentityPattern pattern);


    /**
     * 删除数据
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void unpublish(String dataId, String group, String configInfo);

    
    /**
     * 同步删除数据
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncUnpublish(String dataId, String group, String configInfo, long timeout);

    /**
     * 发布全量数据
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publishAll(String dataId, String group, String configInfo);
    
    
    /**
     * 同步发布全量数据
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublishAll(String dataId, String group, String configInfo, long timeout);


    /**
     * 获取diamond配置
     * 
     * @return
     */
    DiamondConfigure getDiamondConfigure();


    /**
     * 获取实时通知客户端
     * 
     * @return
     */
    PushitClient getPushitClient();


    /**
     * 设置diamond配置
     * 
     * @param diamondConfigure
     */
    void setDiamondConfigure(DiamondConfigure diamondConfigure);


    /**
     * 
     */
    void setDiamondSubscriber(DiamondSubscriber diamondSubscriber);


    /**
     * 启动发布者
     */
    void start();


    /**
     * 关闭发布者
     */
    void close();


    /**
     * 增加增量发布的dataId到缓存
     * 
     * @param dataId
     * @param group
     */
    void addDataId(String dataId, String group, String configInfo);


    /**
     * 
     * @param dataId
     * @param group
     */
    void removeDataId(String dataId, String group);


    /**
     * 等待发布任务完成
     * 
     * @throws InterruptedException
     */
    void awaitPublishFinish() throws InterruptedException;


    void scheduledReport();
}
