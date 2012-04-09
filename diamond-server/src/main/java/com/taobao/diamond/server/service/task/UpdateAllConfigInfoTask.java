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

import com.taobao.diamond.notify.utils.task.Task;


/**
 * 更新全量配置的任务
 * 
 * @author leiwen.zh
 * 
 */
public class UpdateAllConfigInfoTask extends Task {

    private String dataId;
    private String group;
    private String content;
    private int failCount;


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


    public String getContent() {
        return content;
    }


    public void setContent(String content) {
        this.content = content;
    }


    public int getFailCount() {
        return failCount;
    }


    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }


    public UpdateAllConfigInfoTask(String dataId, String group, String content) {
        this.dataId = dataId;
        this.group = group;
        this.content = content;
    }


    @Override
    public void merge(Task task) {
        // do nothing
    }

}
