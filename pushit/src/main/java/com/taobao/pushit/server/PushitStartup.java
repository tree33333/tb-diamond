/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.server;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.taobao.pushit.commons.ServerLoggerInit;


public class PushitStartup {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Useage:java com.taobao.pushit.server.PushitStartup server.properties");
            System.exit(1);
        }
        
        // ¼ÓÔØlog4j
        try {
            ServerLoggerInit.initLog();
        }
        catch(Throwable t) {
            // ignore
        }

        Properties props = new Properties();
        final FileReader reader = new FileReader(args[0]);
        props.load(reader);
        reader.close();
        PushitBroker broker = new PushitBroker(props);
        broker.startup();
    }
}
