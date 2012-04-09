/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.utils;

import com.taobao.monitor.MonitorLog;


/**
 * 
 * 
 * StatLog的包装类
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-6-2 下午03:15:37
 */

public final class DiamondStatLog {

    private DiamondStatLog() {

    }


    public static final void addStat(String appName, String keyOne, String keyTwo, String keyThree) {
        MonitorLog.addStat(keyOne, keyTwo, keyThree, 0L, 1L);
    }


    public static final void addStat(String appName, String keyOne, String keyTwo) {
        MonitorLog.addStat(keyOne, keyTwo, null, 0L, 1L);
    }


    public static final void addStat(String appName, String keyOne) {
        // StatLog.addStat(appName, keyOne);
        MonitorLog.addStat(keyOne, null, null, 0L, 1L);
    }


    public static final void addStatValue2(String appName, String keyOne, long value) {
        MonitorLog.addStat(keyOne, null, null, value, 1L);
    }


    public static final void addStatValue2(String appName, String keyOne, String keyTwo, long value) {
        MonitorLog.addStatValue2(keyOne, keyTwo, null, value);
    }


    public static final void addStatValue2(String appName, String keyOne, String keyTwo, String keyThree, long value) {
        MonitorLog.addStat(keyOne, keyTwo, keyThree, value, 1L);
    }

}
