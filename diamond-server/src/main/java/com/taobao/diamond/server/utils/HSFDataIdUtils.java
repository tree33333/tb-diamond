/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.utils;

/**
 * 处理HSF的dataId的工具类
 * 
 * @author leiwen.zh
 * 
 */
public class HSFDataIdUtils {

    public static final String ADDR_SUFFIX = ".ADDRESS";
    public static final String DIAMOND_ROUT_SUFFIX = ".ROUTINGRULE";
    public static final String CONFSRV_ROUT_SUFFIX = ".ROUTING.RULE";
    public static final String DIAMOND_AGGR_SUFFIX = ".FORCONSUMER";


    /**
     * 将config server端的HSF地址配置的dataId进行修改, 在末尾添加.ADDRESS
     * 
     * @param addrDataId
     *            config server端的HSF地址配置的dataId
     * @return 修改后的dataId
     */
    private static String c2dModifyAddress(String addrDataId) {
        StringBuilder result = new StringBuilder(addrDataId);
        result.append(ADDR_SUFFIX);
        return result.toString();
    }


    /**
     * 将diamond端的HSF地址配置等待dataId进行修改, 去掉末尾的.ADDRESS
     * 
     * @param addrDataId
     *            diamond端的HSF地址配置的dataId
     * @return 修改后的dataId
     */
    private static String d2cModifyAddress(String addrDataId) {
        return addrDataId.substring(0, addrDataId.indexOf(ADDR_SUFFIX));
    }


    /**
     * 将config server端的HSF路由规则配置的dataId进行修改, 将ROUTING.RULE的"."去掉
     * 
     * @param routDataId
     *            config server端的HSF路由规则配置的dataId
     * @return 修改后的dataId
     */
    private static String c2dModifyRoutingRule(String routDataId) {
        return routDataId.replace(CONFSRV_ROUT_SUFFIX, DIAMOND_ROUT_SUFFIX);
    }


    /**
     * 将diamond端的HSF路由规则配置的dataId进行修改, 将ROUTINGRULE改为ROUTING.RULE
     * 
     * @param routDataId
     *            diamond端的HSF路由规则配置的dataId
     * @return 修改后的dataId
     */
    private static String d2cModifyRoutingRule(String routDataId) {
        return routDataId.replace(DIAMOND_ROUT_SUFFIX, CONFSRV_ROUT_SUFFIX);
    }


    public static String d2cDataIdMapping(String dataId) {
        if (dataId.endsWith(HSFDataIdUtils.ADDR_SUFFIX)) {
            dataId = HSFDataIdUtils.d2cModifyAddress(dataId);
        }
        if (dataId.endsWith(HSFDataIdUtils.DIAMOND_ROUT_SUFFIX)) {
            dataId = HSFDataIdUtils.d2cModifyRoutingRule(dataId);
        }
        return dataId;
    }
    
    
    public static String c2dDataIdMapping(String dataId, boolean isUrl) {
        if (dataId.endsWith(HSFDataIdUtils.CONFSRV_ROUT_SUFFIX)) {
            dataId = HSFDataIdUtils.c2dModifyRoutingRule(dataId);
        }
        if ( isUrl ) {
            dataId = c2dModifyAddress(dataId);
        }
        
        return dataId;
    }
}
