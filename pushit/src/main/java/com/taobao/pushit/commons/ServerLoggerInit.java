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

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;


public class ServerLoggerInit {

    private static final Log log = LogFactory.getLog(ServerLoggerInit.class);

    private static boolean initOK = false;

    private static final String PUSHIT_SERVER_LOG4J_FILE = "pushit_server_log4j.properties";


    public static void initLog() {

        if (initOK) {
            return;
        }

        URL url = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null || (url = cl.getResource(PUSHIT_SERVER_LOG4J_FILE)) == null) {
            cl = ServerLoggerInit.class.getClassLoader();
            if (cl == null || (url = cl.getResource(PUSHIT_SERVER_LOG4J_FILE)) == null) {
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
    }


    private static void fallback() {
        log.warn("[Global] Failed to read pushit logger configuration, use root log configuration.");
    }
}
