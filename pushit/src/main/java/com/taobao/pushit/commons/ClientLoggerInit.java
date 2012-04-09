/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.commons;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * pushit-client日志初始化, pushit-client的日志单独打印到某个文件中, 该文件和应用的日志在同一个目录中， 如果应用没有在root
 * logger上设置file appender, 该文件的位置为${user.home}/diamond/logs/pushit-client.log
 * 
 * @author leiwen.zh
 * 
 */
public class ClientLoggerInit {

    private static final Log log = LogFactory.getLog(ClientLoggerInit.class);

    private static volatile boolean initOK = false;

    private static final String PUSHIT_CLIENT_LOG4J_FILE = "pushit_client_log4j.properties";
    private static final String PUSHIT_CLIENT_LOGGER = "com.taobao.pushit";


    public static void initLog() {

        if (initOK) {
            return;
        }

        URL url = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null || (url = cl.getResource(PUSHIT_CLIENT_LOG4J_FILE)) == null) {
            cl = ClientLoggerInit.class.getClassLoader();
            if (cl == null || (url = cl.getResource(PUSHIT_CLIENT_LOG4J_FILE)) == null) {
                fallback();
                return;
            }
        }

        final ClassLoader pre = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(PropertyConfigurator.class.getClassLoader());
            PropertyConfigurator.configure(url);
        }
        finally {
            Thread.currentThread().setContextClassLoader(pre);
        }

        // 获取应用在root logger上设置的file appender, 根据此appender确定pushit.log所在的目录
        FileAppender rootFileAppender = getFileAppender(Logger.getRootLogger());
        if (rootFileAppender == null) {
            log.warn("应用没有在root logger上设置file appender!!!");
            rootFileAppender = new FileAppender();
            rootFileAppender.setFile(System.getProperty("user.home") + "/diamond/logs/pushit-client.log");
        }

        // 设置pushit logger即可, 由于同一个log4j配置文件中的appender会被文件中所有的logger复用,
        // 所以gecko的日志也会采用pushit配置的appender
        setFileAppender(rootFileAppender, PUSHIT_CLIENT_LOGGER);

        initOK = true;
    }


    private static FileAppender getFileAppender(Logger logger) {
        Enumeration<?> allAppenders = logger.getAllAppenders();
        while (allAppenders.hasMoreElements()) {
            Object appender = allAppenders.nextElement();
            if (appender instanceof FileAppender) {
                return (FileAppender) appender;
            }
        }
        return null;
    }


    private static void setFileAppender(FileAppender rootFileAppender, String loggerName) {
        String rootLogDir = new File(rootFileAppender.getFile()).getParent();
        FileAppender logFileAppender = getFileAppender(Logger.getLogger(loggerName));
        File logFile = new File(rootLogDir, logFileAppender.getFile());
        String logFileAbsolutePath = logFile.getAbsolutePath();
        logFileAppender.setFile(logFileAbsolutePath);
        logFileAppender.activateOptions();
        log.warn("成功为" + loggerName + "添加Appender. 输出路径:" + logFileAbsolutePath);
    }


    private static void fallback() {
        log.warn("[Global] Failed to read pushit logger configuration, use root log configuration.");
    }
}
