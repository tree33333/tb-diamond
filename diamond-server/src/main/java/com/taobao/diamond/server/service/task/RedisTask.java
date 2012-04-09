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


public class RedisTask extends Task {

    private String ip;
    private String content;
    private int failCount;


    public RedisTask(String ip, String content) {
        this.ip = ip;
        this.content = content;
    }


    public String getIp() {
        return ip;
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


    @Override
    public void merge(Task task) {
        // 将相同ip的内容进行合并
        RedisTask anotherTask = (RedisTask) task;
        if (this.getIp().equals(anotherTask.getIp())) {
            this.setContent(mergeContent(this.getContent(), anotherTask.getContent()));
        }
    }


    private String mergeContent(String oldContent, String newContent) {
        StringBuffer sb = new StringBuffer(oldContent);
        sb.append(",");
        sb.append(newContent);
        return sb.toString();
    }

}
