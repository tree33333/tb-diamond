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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;


/**
 * 实时通知服务
 * 
 * @author leiwen
 * 
 */
public class PushitService {
    private static final Log log = LogFactory.getLog(PushitService.class);

    private PushitClient pushitClient;

    private String pushitServers;


    public void init() {
        log.info("开始初始化实时通知客户端");
        try {
            pushitClient = new PushitClient(pushitServers, (NotifyListener) null, 3000L);
        }
        catch (Exception e) {
            log.error("初始化实时通知客户端出错", e);
        }
        log.info("实时通知客户端初始化完毕");
    }


    public void destroy() {
        log.info("开始关闭实时通知客户端");
        try {
            pushitClient.stop();
        }
        catch (IOException e) {
            log.error("关闭实时通知客户端出错", e);
        }
        log.info("关闭实时通知客户端完成");
    }


    public String getPushitServers() {
        return pushitServers;
    }


    public void setPushitServers(String pushitServers) {
        this.pushitServers = pushitServers;
    }


    public void pushNotification(String dataId, String group, String message) throws Exception {
        this.pushitClient.push(dataId, group, message);
    }

}
