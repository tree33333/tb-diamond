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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.domain.Aggregation;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.utils.ResourceUtils;


public class AggregationService {

    ConfigService configService;
    PersistService persistService;


    public PersistService getPersistService() {
        return persistService;
    }


    public void setPersistService(PersistService persistService) {
        this.persistService = persistService;
    }


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    static final Log log = LogFactory.getLog(AggregationService.class);
    List<String> suffiies = new ArrayList<String>();
    // Multimap<String, String> aggrSuffiies = HashMultimap.create();
    Map<String, String> suffix2aggr = new HashMap<String, String>();
    
    public static Set<String> aggrs = new HashSet<String>();
    {
        try {
            InputStream is = ResourceUtils.getResourceAsStream("aggregation.properties");
            Properties props = new Properties();
            props.load(is);
            for (Object o : props.keySet()) {
                String suffix = (String) o;
                String aggr = (String) props.get(suffix);
                if (suffix.equals(aggr))
                    continue;
                // aggrSuffiies.put(aggr, suffix);
                suffix2aggr.put(suffix, aggr);
                suffiies.add(suffix);
                aggrs.add(aggr);
            }
        }
        catch (Exception e) {
            log.error("aggregation.properties is not exist.");
        }

    }


    private String getSuffix(String dataId) {
        for (String suffix : suffiies) {
            if (dataId.endsWith(suffix)) {
                return suffix;
            }
        }
        return null;
    }


    public boolean isAggregation(String dataId) {
        for (String suffix : suffiies) {
            if (dataId.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }


    public boolean aggregation(String dataId, String group) {
        boolean needAggregation = isAggregation(dataId);
        if (!needAggregation)
            return false;
        boolean status = true;
        String suffix = this.getSuffix(dataId);
        String aggr = suffix2aggr.get(suffix);
        String aggrDataId = dataId.replace(suffix, aggr);
        String aggrGroup = group;
        ConfigInfo aggrOld = persistService.findConfigInfo(aggrDataId, aggrGroup);
        String pureDataId = dataId.replace(suffix, "");
        Aggregation aggregation = new Aggregation();
        for (String ssuffix : suffiies) {
            String aggrSuffix = suffix2aggr.get(ssuffix);
            if (aggrSuffix.equals(aggr)) {
                ConfigInfo info = persistService.findConfigInfo(pureDataId + ssuffix, aggrGroup);
                if (info != null)
                    aggregation.addItem(info);
            }
        }
        String aggrContent = aggregation.generateContent();

        if (aggrOld == null) {
            if (!aggregation.getItems().isEmpty()) {
                // 单项数据存在，聚合数据不存在，说明要新增聚合数据到server
                configService.addConfigInfo(aggrDataId, aggrGroup, aggrContent);
            }
            // 否则什么都不做
        }
        else {
            if (!aggregation.getItems().isEmpty()) {
                // 单项数据存在，聚合数据也存在，说明要更新聚合数据到server
                configService.updateConfigInfo(aggrDataId, aggrGroup, aggrContent);
            }
            else {
                // 单项数据不存在，但聚合数据存在，说明要删除server上的聚合数据
                configService.removeConfigInfo(aggrDataId, aggrGroup);
            }
        }
        return status;
    }

}
