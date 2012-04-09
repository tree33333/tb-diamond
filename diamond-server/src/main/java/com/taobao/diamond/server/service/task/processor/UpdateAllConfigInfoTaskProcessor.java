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

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;
import com.taobao.diamond.server.service.AggregationService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.RealTimeNotifyTask;
import com.taobao.diamond.server.service.task.UpdateAllConfigInfoTask;
import com.taobao.diamond.server.utils.DiamondServerConstants;


public class UpdateAllConfigInfoTaskProcessor implements TaskProcessor {

    private static final Log log = LogFactory.getLog(UpdateAllConfigInfoTaskProcessor.class);
    private static final Log failCountLog = LogFactory.getLog("failLog");

    private ConfigService configService;

    private AggregationService aggrService;

    private TaskManagerService taskManagerService;

    private RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor;


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }


    public AggregationService getAggrService() {
        return aggrService;
    }


    public void setAggrService(AggregationService aggrService) {
        this.aggrService = aggrService;
    }


    public TaskManagerService getTaskManagerService() {
        return taskManagerService;
    }


    public void setTaskManagerService(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }


    public RealTimeNotifyTaskProcessor getRealTimeNotifyTaskProcessor() {
        return realTimeNotifyTaskProcessor;
    }


    public void setRealTimeNotifyTaskProcessor(RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor) {
        this.realTimeNotifyTaskProcessor = realTimeNotifyTaskProcessor;
    }


    public boolean process(String taskType, Task task) {
        UpdateAllConfigInfoTask updateTask = (UpdateAllConfigInfoTask) task;
        String dataId = updateTask.getDataId();
        String group = updateTask.getGroup();
        String content = updateTask.getContent();
        int failCount = updateTask.getFailCount();

        if (innerProcess(dataId, group, content)) {
            log.info("全量更新成功: dataId=" + dataId + ", group=" + group + ", content=" + content);
            updateTask.setFailCount(0);
            
            try {
                this.aggrService.aggregation(dataId, group);
            }
            catch (Exception e) {
                log.error("数据聚合出错, dataId=" + dataId + "group=" + group, e);
            }
            
            try {
                this.realTimeNotify(dataId, group);
            }
            catch (Exception e) {
                log.error("实时通知出错, dataId=" + dataId + "group=" + group, e);
            }
            return true;
        }
        else {
            log.warn("全量更新失败: dataId=" + dataId + ", group=" + group + ", content=" + content);
            failCount++;
            updateTask.setFailCount(failCount);
            if (failCount >= DiamondServerConstants.MAX_UPDATEALL_FAIL_COUNT) {
                failCountLog.info("全量更新数据失败次数达到上限: dataId=" + dataId + ", group=" + group + ", content=" + content);
                updateTask.setFailCount(0);
                return true;
            }
            return false;
        }
    }


    private boolean innerProcess(String dataId, String group, String content) {
        boolean result = false;
        try {
            ConfigInfo oldConfigInfo = this.configService.findConfigInfo(dataId, group);
            if (oldConfigInfo == null) {
                log.info("全量更新转换为新增, dataId=" + dataId + ", group=" + group + ", content=" + content);
                this.configService.addConfigInfo(dataId, group, content);
                result = true;
            }
            else {
                log.info("全量更新, dataId=" + dataId + ", group=" + group + ", content=" + content);
                this.configService.updateConfigInfo(dataId, group, content);
                result = true;
            }

            return result;
        }
        catch (Exception e) {
            log.error("全量更新配置出现错误：dataId=" + dataId + ", group=" + group + ", content=" + content, e);
            return false;
        }
    }


    private void realTimeNotify(String dataId, String group) {
        String taskType = dataId + "-" + group + "-pushit";
        RealTimeNotifyTask task = new RealTimeNotifyTask(dataId, group);
        task.setLastProcessTime(System.currentTimeMillis());
        task.setTaskInterval(2000L);
        this.taskManagerService.addPushitProcessor(taskType, realTimeNotifyTaskProcessor);
        this.taskManagerService.addPushitTask(taskType, task, false);
    }

}
