/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.manager.impl;

import java.util.List;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.manager.BaseStoneSubManager;
import com.taobao.diamond.manager.ManagerListener;


/**
 * 软负载中心管理者，一个JVM中，一个dataId只能对应一个管理者
 * 
 * @author leiwen
 * 
 */
public class DefaultBaseStoneSubManager extends DefaultDiamondManager implements BaseStoneSubManager {

    /**
     * 订阅数据的构造方法，指定集群类型，指定监听器，指定是否使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListener
     * @param useRealTimeNotification
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            ManagerListener managerListener, boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListener, useRealTimeNotification, clusterType);
    }


    /**
     * 订阅数据的构造方法，指定集群类型，指定监听器，使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListener
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            ManagerListener managerListener) {
        super(subGroup, subDataId, managerListener, clusterType);
    }


    /**
     * 订阅数据的构造方法，使用默认的集群类型basestone，指定监听器，使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListener
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, ManagerListener managerListener) {
        super(subGroup, subDataId, managerListener, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * 订阅数据的构造方法，使用默认的集群类型basestone，指定监听器，指定是否使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListener
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, ManagerListener managerListener,
            boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListener, useRealTimeNotification, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * 订阅数据的构造方法，指定集群类型，指定监听器列表，指定是否使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            List<ManagerListener> managerListenerList, boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListenerList, useRealTimeNotification, clusterType);
    }


    /**
     * 订阅数据的构造方法，指定集群类型，指定监听器列表，使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            List<ManagerListener> managerListenerList) {
        super(subGroup, subDataId, managerListenerList, clusterType);
    }


    /**
     * 订阅数据的构造方法，使用默认的集群类型basestone，指定监听器列表，使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, List<ManagerListener> managerListenerList) {
        super(subGroup, subDataId, managerListenerList, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * 订阅数据的构造方法，使用默认的集群类型basestone，指定监听器列表，指定是否使用实时通知
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, List<ManagerListener> managerListenerList,
            boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListenerList, useRealTimeNotification, Constants.DEFAULT_BASESTONE_CLUSTER);
    }

}
