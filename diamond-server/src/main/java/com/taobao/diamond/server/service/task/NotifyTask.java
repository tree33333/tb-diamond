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


public class NotifyTask extends Task {

    private String dataId;
    private String group;
    private int failCount;


    public NotifyTask(String dataId, String group) {
        this.dataId = dataId;
        this.group = group;
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


    public int getFailCount() {
        return failCount;
    }


    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }


    @Override
    public void merge(Task task) {
        // 进行merge, 但什么都不做, 相同的dataId和group的任务，后来的会代替之前的

    }

}
