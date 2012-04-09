/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.client.impl;

import java.security.SecureRandom;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.client.task.PublishTask;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;


public class PublishAllTaskProcessor implements TaskProcessor {

    private static final Log log = LogFactory.getLog(PublishAllTaskProcessor.class);

    private DefaultDiamondPublisher diamondPublisher;
    private DefaultDiamondSubscriber diamondSubscriber;

    private final SecureRandom random = new SecureRandom();


    public void setDiamondPublisher(DefaultDiamondPublisher diamondPublisher) {
        this.diamondPublisher = diamondPublisher;
    }


    public void setDiamondSubscriber(DefaultDiamondSubscriber diamondSubscriber) {
        this.diamondSubscriber = diamondSubscriber;
    }


    /**
     * 异步处理
     */
    public boolean process(String taskType, Task task) {
        return processPublish((PublishTask) task);
    }
    
    
    public boolean syncProcess(String dataId, String group, String content, long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("不合法的超时时间, 超时时间必须大于0");
        }

        while (timeout > 0) {
            try {
                this.diamondPublisher.updateAll(dataId, group, content);
                return true;
            }
            catch (Exception e) {
                log.error("同步全量更新数据出错, dataId=" + dataId + ",group=" + group + ",content=" + content + ",timeout="
                        + timeout + "ms", e);
                // 重新计算timeout
                timeout = timeout - Constants.ONCE_TIMEOUT;
                // 切换到下一台server尝试
                this.diamondPublisher.rotateToNextDomain();
            }
        }

        log.error("同步全量更新数据超时, dataId=" + dataId + ",group=" + group + ",content=" + content + ",timeout=" + timeout
                + "ms");
        return false;
    }


    private boolean processPublish(PublishTask publishTask) {
        int count = publishTask.getCount();
        int waitTime = getWaitTime(++count);
        log.info("发布全量数据，第" + count + "次尝试");
        publishTask.setCount(count);
        String dataId = publishTask.getDataId();
        String group = publishTask.getGroup();

        Iterator<String> it = publishTask.getContents().iterator();
        while (it.hasNext()) {
            String configInfo = it.next();
            if (innerPublish(publishTask, waitTime, dataId, group, configInfo)) {
                it.remove();
            }
            else {
                return false;
            }
        }
        return true;
    }


    private boolean innerPublish(PublishTask publishTask, int waitTime, String dataId, String group, String configInfo) {
        try {
            String receivedConfigInfo = diamondSubscriber.getAvailableConfigureInfomation(dataId, group, 60000);

            if (receivedConfigInfo == null) {
                log.info("发布全新的全量数据: dataId=" + dataId + ", group=" + group + ", configInfo=" + configInfo);
                diamondPublisher.publishNew(dataId, group, configInfo);
                // 发布成功
                return true;
            }
            else if(!receivedConfigInfo.equals(configInfo)){
                log.info("更新全量数据: dataId=" + dataId + ", group=" + group + ", configInfo=" + configInfo);
                diamondPublisher.updateAll(dataId, group, configInfo);
                // 更新成功
                return true;
            }
            else {
                log.info("数据相同, 不需要全量更新: dataId=" + dataId + ", group=" + group + ", configInfo=" + configInfo);
                return true;
            }
        }
        catch (Exception e) {
            log.error("发布数据出现异常: dataId=" + dataId + ", group=" + group, e);
            publishTask.setTaskInterval(waitTime * 1000);
            this.diamondPublisher.rotateToNextDomain();
        }
        return false;
    }


    private int getWaitTime(int count) {
        if (count > 0 && count <= 5) {
            return random.nextInt(30) + 1;
        }
        else {
            return random.nextInt(30) + 31;
        }
    }

}
