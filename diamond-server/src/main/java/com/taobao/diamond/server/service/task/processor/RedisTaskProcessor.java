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
import com.taobao.diamond.server.service.RedisService;
import com.taobao.diamond.server.service.task.RedisTask;
import com.taobao.diamond.server.utils.DiamondServerConstants;


public class RedisTaskProcessor implements TaskProcessor {

    private static final Log log = LogFactory.getLog(RedisTaskProcessor.class);
    private static final Log failCountLog = LogFactory.getLog("failLog");

    private RedisService redisService;


    public RedisService getRedisService() {
        return redisService;
    }


    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }


    public boolean process(String taskType, Task task) {
        RedisTask redisTask = (RedisTask) task;
        String ip = redisTask.getIp();
        String content = redisTask.getContent();
        int failCount = redisTask.getFailCount();

        if (this.addIpToContentPair(ip, content)) {
            log.info("增加ip与dataId映射到redis成功, ip=" + ip + "dataIds=" + content);
            redisTask.setFailCount(0);
            return true;
        }
        else {
            log.warn("增加ip与dataId映射到redis失败, ip=" + ip + "dataIds=" + content);
            failCount++;
            redisTask.setFailCount(failCount);
            if (failCount >= DiamondServerConstants.MAX_REDIS_FAIL_COUNT) {
                failCountLog.info("redis任务失败次数达到上限: ip=" + ip + ", dataIds=" + content);
                redisTask.setFailCount(0);
                return true;
            }
            return false;
        }
    }


    private boolean addIpToContentPair(String ip, String content) {
        try {
            String[] values = content.split(",");
            this.redisService.addAll("basestone-" + ip, values);
            return true;
        }
        catch (Exception e) {
            log.error("增加ip与发布dataId的映射到redis出错, ip为" + ip + ", dataId为" + content);
            return false;
        }
    }

}
