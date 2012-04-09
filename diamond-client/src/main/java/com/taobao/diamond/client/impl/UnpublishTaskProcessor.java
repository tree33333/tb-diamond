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

import com.taobao.diamond.client.task.UnpublishTask;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;


public class UnpublishTaskProcessor implements TaskProcessor {

    private static final Log log = LogFactory.getLog(UnpublishTaskProcessor.class);

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
        return processUnpublish((UnpublishTask) task);
    }


    /**
     * 同步处理
     * 
     * @param dataId
     * @param group
     * @param content
     * @param timeout
     * @return
     */
    public boolean syncProcess(String dataId, String group, String content, long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("不合法的超时时间, 超时时间必须大于0");
        }

        while (timeout > 0) {
            try {
                this.diamondPublisher.publishRemove(dataId, group, content);
                return true;
            }
            catch (Exception e) {
                log.error("同步删除数据出错, dataId=" + dataId + ",group=" + group + ",content=" + content + ",timeout="
                        + timeout + "ms", e);
                // 重新计算timeout
                timeout = timeout - Constants.ONCE_TIMEOUT;
                // 切换到下一台server尝试
                this.diamondPublisher.rotateToNextDomain();
            }
        }

        log.error("同步删除数据超时, dataId=" + dataId + ",group=" + group + ",content=" + content + ",timeout=" + timeout
                + "ms");
        return false;
    }


    private boolean processUnpublish(UnpublishTask unpublishTask) {
        int count = unpublishTask.getCount();
        int waitTime = getWaitTime(++count);
        log.info("删除数据，第" + count + "次尝试");
        unpublishTask.setCount(count);
        String dataId = unpublishTask.getDataId();
        String group = unpublishTask.getGroup();

        Iterator<String> it = unpublishTask.getContents().iterator();
        while (it.hasNext()) {
            String configInfo = it.next();
            if (innerUnpublish(unpublishTask, waitTime, dataId, group, configInfo)) {
                it.remove();
            }
            else {
                return false;
            }
        }
        return true;
    }


    private boolean innerUnpublish(UnpublishTask unpublishTask, int waitTime, String dataId, String group,
            String configInfo) {
        try {
            // 删除前根据dataId获取数据，决定是否需要删除
            String receivedConfigInfo = diamondSubscriber.getAvailableConfigureInfomation(dataId, group, 60000);

            if (receivedConfigInfo == null) {
                log.info("要删除数据的dataId不存在，直接返回");
                return true;
            }
            else if (!receivedConfigInfo.contains(configInfo)) {
                log.info("要删除数据的dataId存在，但数据不存在，直接返回");
                return true;
            }
            else {
                log.info("要删除的数据存在，删除");
                this.diamondPublisher.publishRemove(dataId, group, configInfo);
                return true;
            }
        }
        catch (Exception e) {
            log.error("删除数据出现异常: dataId=" + dataId + ", group=" + group, e);
            unpublishTask.setTaskInterval(waitTime * 1000);
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
