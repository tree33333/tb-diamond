/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.jmx;

import java.lang.management.ManagementFactory;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.server.service.ConfigService;


public class DataIdCountService implements DataIdCountServiceMBean {

    private static final Log log = LogFactory.getLog(DataIdCountService.class);

    private ConfigService configService;


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }


    public void init() {
        try {
            ObjectName oName =
                    new ObjectName(DataIdCountService.class.getPackage().getName() + ":type="
                            + DataIdCountService.class.getSimpleName());
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, oName);
        }
        catch (Exception e) {
            log.error("DataIdCountService×¢²ámbean³ö´í", e);
        }
    }


    public int countAllDataIds() {
        return this.configService.countAllDataIds();
    }

}
