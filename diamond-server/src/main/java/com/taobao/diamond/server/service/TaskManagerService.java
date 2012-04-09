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

import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.notify.utils.task.TaskProcessor;


/**
 * TaskManager服务, 包装了各种TaskManager
 * 
 * @author leiwen.zh
 * 
 */
public class TaskManagerService {

    private static final int UPDATE_THREAD_COUNT = 4;

    private TaskManager[] updateTaskManagers;
    private TaskManager updateAllTaskManager;
    private TaskManager removeTaskManager;
    private TaskManager notifyTaskManager;
    private TaskManager pushitTaskManager;
    private TaskManager redisTaskManager;


    public TaskManager[] getUpdateTaskManagers() {
        return updateTaskManagers;
    }


    public void setUpdateTaskManagers(TaskManager[] updateTaskManagers) {
        this.updateTaskManagers = updateTaskManagers;
    }


    public TaskManager getUpdateAllTaskManager() {
        return updateAllTaskManager;
    }


    public void setUpdateAllTaskManager(TaskManager updateAllTaskManager) {
        this.updateAllTaskManager = updateAllTaskManager;
    }


    public TaskManager getRemoveTaskManager() {
        return removeTaskManager;
    }


    public void setRemoveTaskManager(TaskManager removeTaskManager) {
        this.removeTaskManager = removeTaskManager;
    }


    public TaskManager getNotifyTaskManager() {
        return notifyTaskManager;
    }


    public void setNotifyTaskManager(TaskManager notifyTaskManager) {
        this.notifyTaskManager = notifyTaskManager;
    }


    public TaskManager getPushitTaskManager() {
        return pushitTaskManager;
    }


    public void setPushitTaskManager(TaskManager pushitTaskManager) {
        this.pushitTaskManager = pushitTaskManager;
    }


    public TaskManager getRedisTaskManager() {
        return redisTaskManager;
    }


    public void setRedisTaskManager(TaskManager redisTaskManager) {
        this.redisTaskManager = redisTaskManager;
    }


    public void addUpdateTask(String taskType, Task task, boolean isNeedMerge) {
        int index = taskType.hashCode() % UPDATE_THREAD_COUNT;
        if (index < 0) {
            index = -index;
        }
        this.updateTaskManagers[index].addTask(taskType, task, isNeedMerge);
    }


    public void addUpdateProcessor(String taskType, TaskProcessor taskProcessor) {
        int index = taskType.hashCode() % UPDATE_THREAD_COUNT;
        if (index < 0) {
            index = -index;
        }
        this.updateTaskManagers[index].addProcessor(taskType, taskProcessor);
    }


    public void addUpdateAllTask(String taskType, Task task, boolean isNeedMerge) {
        this.updateAllTaskManager.addTask(taskType, task, isNeedMerge);
    }


    public void addUpdateAllProcessor(String taskType, TaskProcessor taskProcessor) {
        this.updateAllTaskManager.addProcessor(taskType, taskProcessor);
    }


    public void addRemoveTask(String taskType, Task task, boolean isNeedMerge) {
        this.removeTaskManager.addTask(taskType, task, isNeedMerge);
    }


    public void addRemoveProcessor(String taskType, TaskProcessor taskProcessor) {
        this.removeTaskManager.addProcessor(taskType, taskProcessor);
    }


    public void addNotifyTask(String taskType, Task task, boolean isNeedMerge) {
        this.notifyTaskManager.addTask(taskType, task, isNeedMerge);
    }


    public void addNotifyProcessor(String taskType, TaskProcessor taskProcessor) {
        this.notifyTaskManager.addProcessor(taskType, taskProcessor);
    }


    public void addPushitTask(String taskType, Task task, boolean isNeedMerge) {
        this.pushitTaskManager.addTask(taskType, task, isNeedMerge);
    }


    public void addPushitProcessor(String taskType, TaskProcessor taskProcessor) {
        this.pushitTaskManager.addProcessor(taskType, taskProcessor);
    }


    public void addRedisTask(String taskType, Task task, boolean isNeedMerge) {
        this.redisTaskManager.addTask(taskType, task, isNeedMerge);
    }


    public void addRedisProcessor(String taskType, TaskProcessor taskProcessor) {
        this.redisTaskManager.addProcessor(taskType, taskProcessor);
    }

}
