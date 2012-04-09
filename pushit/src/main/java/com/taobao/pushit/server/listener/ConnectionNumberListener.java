/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.server.listener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.exception.NotifyRemotingException;


public class ConnectionNumberListener implements ConnectionLifeCycleListener {

    private static final Log log = LogFactory.getLog(ConnectionNumberListener.class);

    // 相同的IP创建的连接数阈值
    private int connThreshold;
    // IP总数阈值
    private int ipCountThreshold;
    // 检查IP总数的任务执行间隔, 单位秒
    private int ipCheckTaskInterval;
    // IP总数有没有溢出
    private volatile boolean isOverflow;

    // 存放所有连接到pushit-server的远端IP, 以及个数
    private ConcurrentHashMap<String, AtomicInteger> connectionIpNumMap =
            new ConcurrentHashMap<String, AtomicInteger>();

    private Lock lock = new ReentrantLock();

    private ScheduledExecutorService scheduler;


    public ConnectionNumberListener(int connThreshold, int ipCountThreshold, int ipCheckTaskInterval) {
        this.connThreshold = connThreshold;
        this.ipCountThreshold = ipCountThreshold;
        this.ipCheckTaskInterval = ipCheckTaskInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("connection num control thread");
                t.setDaemon(true);
                return t;
            }
        });

        this.scheduler.scheduleAtFixedRate(new Runnable() {

            public void run() {
                int ipCount = ConnectionNumberListener.this.connectionIpNumMap.size();
                if (ipCount >= ConnectionNumberListener.this.ipCountThreshold) {
                    log.warn("IP总数超过阈值, 不再接受更多IP的连接, 当前IP数=" + ipCount + ", 阈值="
                            + ConnectionNumberListener.this.ipCountThreshold);
                    isOverflow = true;
                }
                else {
                    isOverflow = false;
                }
            }

        }, this.ipCheckTaskInterval, this.ipCheckTaskInterval, TimeUnit.SECONDS);
    }


    /**
     * 连接创建时, 控制连接数
     */
    public void onConnectionCreated(Connection conn) {

        // 获取远端IP
        String remoteIp = this.getRemoteIp(conn);

        try {
            // 根据远端IP在server端的连接数, 决定是否拒绝连接
            AtomicInteger connNum = this.connectionIpNumMap.get(remoteIp);
            if (connNum == null) {
                AtomicInteger newConnNum = new AtomicInteger(0);
                AtomicInteger oldConnNum = this.connectionIpNumMap.putIfAbsent(remoteIp, newConnNum);
                if (oldConnNum != null) {
                    connNum = oldConnNum;
                }
                else {
                    connNum = newConnNum;
                }
            }

            connNum.incrementAndGet();
            // 大于阈值, 拒绝连接
            if (isOverflow || connNum.get() > this.connThreshold) {
                // 宁可多杀一千，不可放过一个。哈哈
                log.warn("与pushit-server的连接数超过阈值, 拒绝连接, 当前连接数:" + connNum.get() + ",阈值:" + this.connThreshold);
                conn.close(false);
            }
        }
        catch (NotifyRemotingException e) {
            log.error("关闭连接错误, remoteIp=" + remoteIp, e);
        }
        catch (Exception e) {
            log.error("其他错误, remoteIp=" + remoteIp, e);
        }

    }


    public void onConnectionReady(Connection conn) {

    }


    public void onConnectionClosed(Connection conn) {
        String remoteIp = null;
        try {
            // 获取远端IP
            remoteIp = this.getRemoteIp(conn);
            // 关闭连接后减少连接数
            lock.lock();
            AtomicInteger connNum = this.connectionIpNumMap.get(remoteIp);
            if (connNum == null) {
                return;
            }
            if (connNum.decrementAndGet() <= 0) {
                this.connectionIpNumMap.remove(remoteIp);
            }
        }
        finally {
            lock.unlock();
        }
    }


    private String getRemoteIp(Connection connection) {
        // 获取远端IP
        String remoteAddr = RemotingUtils.getAddrString(connection.getRemoteSocketAddress());
        String remoteIp = null;
        if (remoteAddr.indexOf(":") == -1) {
            remoteIp = remoteAddr;
        }
        else {
            remoteIp = remoteAddr.substring(0, remoteAddr.indexOf(":"));
        }
        return remoteIp;
    }

}
