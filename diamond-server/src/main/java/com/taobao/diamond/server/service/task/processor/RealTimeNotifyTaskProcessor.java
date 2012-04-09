/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.task.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;
import com.taobao.diamond.server.service.PushitService;
import com.taobao.diamond.server.service.task.RealTimeNotifyTask;
import com.taobao.diamond.server.utils.DiamondServerConstants;


public class RealTimeNotifyTaskProcessor implements TaskProcessor {
    
    private static final Log log = LogFactory.getLog(RealTimeNotifyTaskProcessor.class);
    private static final Log failCountLog = LogFactory.getLog("failLog");

    private PushitService pushitService;


    public PushitService getPushitService() {
        return pushitService;
    }


    public void setPushitService(PushitService pushitService) {
        this.pushitService = pushitService;
    }


    public boolean process(String taskType, Task task) {
        RealTimeNotifyTask realTimeNofityTask = (RealTimeNotifyTask) task;
        String dataId = realTimeNofityTask.getDataId();
        String group = realTimeNofityTask.getGroup();
        int failCount = realTimeNofityTask.getFailCount();
        
        if(innerProcess(dataId, group)) {
            log.info("实时通知成功: dataId=" + dataId + ", group=" + group);
            realTimeNofityTask.setFailCount(0);
            return true;
        }
        else {
            log.warn("实时通知失败: dataId=" + dataId + ", group=" + group);
            failCount++;
            realTimeNofityTask.setFailCount(failCount);
            if(failCount >= DiamondServerConstants.MAX_PUSHIT_FAIL_COUNT) {
                failCountLog.info("实时通知失败次数达到上限: dataId=" + dataId + ", group=" + group);
                realTimeNofityTask.setFailCount(0);
                return true;
            }
            return false;
        }
    }
    
    
    private boolean innerProcess(String dataId, String group) {
        try {
            this.pushitService.pushNotification(dataId, group, "subscribe");
            return true;
        }
        catch (Exception e) {
            log.error("实时通知失败: dataId=" + dataId + ", group=" + group, e);
            return false;
        }
    }

}
