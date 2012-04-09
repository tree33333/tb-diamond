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
import com.taobao.diamond.server.service.NotifyService;
import com.taobao.diamond.server.service.task.NotifyTask;
import com.taobao.diamond.server.utils.DiamondServerConstants;


public class NotifyTaskProcessor implements TaskProcessor {
    private static final Log log = LogFactory.getLog(NotifyTaskProcessor.class);
    private static final Log failCountLog = LogFactory.getLog("failLog");

    private NotifyService notifyService;


    public NotifyService getNotifyService() {
        return notifyService;
    }


    public void setNotifyService(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    public boolean process(String taskType, Task task) {
        NotifyTask notifyTask = (NotifyTask) task;
        String dataId = notifyTask.getDataId();
        String group = notifyTask.getGroup();
        int failCount = notifyTask.getFailCount();

        if (innerProcess(dataId, group)) {
            log.info("通知集群其他节点成功: dataId=" + dataId + ", group=" + group);
            notifyTask.setFailCount(0);
            return true;
        }
        else {
            log.warn("通知集群其他节点失败: dataId=" + dataId + ", group=" + group);
            failCount++;
            notifyTask.setFailCount(failCount);
            if (failCount >= DiamondServerConstants.MAX_NOTIFY_COUNT) {
                failCountLog.info("通知集群其他节点失败次数达到上限: dataId=" + dataId + ", group=" + group);
                notifyTask.setFailCount(0);
                return true;
            }
            return false;
        }
    }


    private boolean innerProcess(String dataId, String group) {
        try {
            this.notifyService.notifyConfigInfoChange(dataId, group);
            return true;
        }
        catch (Exception e) {
            log.error("通知集群其他节点出错: dataId=" + dataId + ", group=" + group + e);
            return false;
        }
    }

}
