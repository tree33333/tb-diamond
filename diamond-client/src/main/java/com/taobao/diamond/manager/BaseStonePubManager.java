/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.manager;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondConfigure;


/**
 * 软负载发布者接口，一个dataId只能对应一个发布者
 * 
 * @author leiwen
 * 
 */
public interface BaseStonePubManager {

    /**
     * 异步发布数据
     * 
     * @param configInfo
     *            待发布的数据
     */
    void publish(String configInfo);


    /**
     * 异步发布数据，指定dataId，使用默认分组，如果dataId为null，将使用构造时传入的dataId
     * 
     * @param dataId
     * @param configInfo
     */
    void publish(String dataId, String configInfo);


    /**
     * 异步发布数据，指定dataId和group，如果dataId或group为null，将使用构造时传入的dataId和group
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publish(String dataId, String group, String configInfo);


    /**
     * 异步发布数据, 指定dataId和group, 指定内容唯一标识的parrern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param pattern
     */
    void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern);


    /**
     * 同步发布数据
     * 
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String configInfo, long timeout);


    /**
     * 同步发布数据, 指定dataId, 使用默认分组，如果dataId为null，将使用构造时传入的dataId
     * 
     * @param dataId
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String dataId, String configInfo, long timeout);


    /**
     * 同步发布数据，指定dataId和group，如果dataId或group为null，将使用构造时传入的dataId和group
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout);


    /**
     * 同步发布数据, 指定dataId和group, 指定内容唯一标识的parrern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @param pattern
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout, ContentIdentityPattern pattern);


    /**
     * 删除单条配置（不是删除整条数据），使用构造时传入的dataId和group
     * 
     * @param configInfo
     */
    void unpublish(String configInfo);


    /**
     * 删除单条配置（不是删除整条数据）
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void unpublish(String dataId, String group, String configInfo);


    /**
     * 删除单条配置（不是删除整条数据），使用默认分组
     * 
     * @param dataId
     * @param configInfo
     */
    void unpublish(String dataId, String configInfo);


    /**
     * 同步删除单条配置, 使用默认分组
     * 
     * @param dataId
     * @param configInfo
     * @return
     */
    boolean syncUnpublish(String dataId, String configInfo, long timeout);


    /**
     * 同步删除单条配置
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @return
     */
    boolean syncUnpublish(String dataId, String group, String configInfo, long timeout);


    /**
     * 发布全量数据, 该方法会将已经存在的数据全部更新, 慎用
     * 
     * @param configInfo
     */
    void publishAll(String configInfo);


    /**
     * 发布全量数据, 指定dataId和group, 该方法会将已经存在的数据全部更新, 慎用
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publishAll(String dataId, String group, String configInfo);


    /**
     * 发布全量数据, 使用默认分组, 该方法会将已经存在的数据全部更新, 慎用
     * 
     * @param dataId
     * @param configInfo
     */
    void publishAll(String dataId, String configInfo);


    /**
     * 同步发布全量数据, 使用构造时传入的dataId和group, 该方法会将已经存在的数据全部更新, 慎用
     * 
     * @param configInfo
     * @return
     */
    boolean syncPublishAll(String configInfo, long timeout);


    /**
     * 同步发布全量数据, 使用默认分组, 该方法会将已经存在的数据全部更新, 慎用
     * 
     * @param dataId
     * @param configInfo
     * @return
     */
    boolean syncPublishAll(String dataId, String configInfo, long timeout);


    /**
     * 同步发布全量数据, 该方法会将已经存在的数据全部更新, 慎用
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @return
     */
    boolean syncPublishAll(String dataId, String group, String configInfo, long timeout);


    /**
     * 获取发布相关的配置
     * 
     * @return
     */
    DiamondConfigure getDiamondConfigure();


    /**
     * 设置发布相关的配置
     * 
     * @param diamondConfigure
     */
    void setDiamondConfigure(DiamondConfigure diamondConfigure);


    /**
     * 关闭发布者
     */
    // void close();

    /**
     * 等待发布完成
     */
    void awaitPublishFinish() throws InterruptedException;


    /**
     * 周期性的报告自己的ip和发布的dataId, 当发布地址时可以通过此方法来实现机器下线时地址的自动删除
     */
    void scheduledReport();
}
