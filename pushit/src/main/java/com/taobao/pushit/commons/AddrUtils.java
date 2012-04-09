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

import java.net.InetSocketAddress;

import com.taobao.pushit.server.PushitBroker;

public class AddrUtils {

    public static String getUrlFromHost(String server) {
        int finalColon = server.lastIndexOf(':');
        if (finalColon > 0) {
            String hostPart = server.substring(0, finalColon);
            String portNum = server.substring(finalColon + 1);
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(hostPart, Integer.parseInt(portNum));
            return "pushit://" + inetSocketAddress.getAddress().getHostAddress() + ":" + inetSocketAddress.getPort();
        }
        else {
            return "pushit://" + server + ":" + PushitBroker.DEFAULT_PORT;
        }
    }

}
