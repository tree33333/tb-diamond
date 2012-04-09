/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.task;

import java.util.ArrayList;
import java.util.List;

import com.taobao.diamond.notify.utils.task.Task;


/**
 * 配置任务, 更新增量配置, 删除配置, 同步配置时使用
 * 
 * @author leiwen.zh
 * 
 */
public class ConfigInfoTask extends Task {

    private String dataId;
    private String group;
    private List<String> contents = new ArrayList<String>();
    // 任务失败次数
    private int failCount;


    public ConfigInfoTask(String dataId, String group, String content) {
        this.dataId = dataId;
        this.group = group;
        this.contents.add(content);
    }


    public ConfigInfoTask(String dataId, String group, List<String> contents) {
        this.dataId = dataId;
        this.group = group;
        this.contents = contents;
    }


    public String getDataId() {
        return dataId;
    }


    public void setDataId(String dataId) {
        this.dataId = dataId;
    }


    public String getGroup() {
        return group;
    }


    public void setGroup(String group) {
        this.group = group;
    }


    public List<String> getContents() {
        return contents;
    }


    public int getFailCount() {
        return failCount;
    }


    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }


    @Override
    public void merge(Task task) {
        // 将相同dataId和group的内容合并
        ConfigInfoTask anotherTask = (ConfigInfoTask) task;
        if (this.getDataId().equals(anotherTask.getDataId()) && this.getGroup().equals(anotherTask.getGroup())) {
            // 将旧任务的内容全部增加到新任务中
            this.contents.addAll(anotherTask.contents);
        }
    }


    @Override
    public String toString() {
        return this.getClass().getName() + ": dataId=" + dataId + ",group=" + group;
    }

}
