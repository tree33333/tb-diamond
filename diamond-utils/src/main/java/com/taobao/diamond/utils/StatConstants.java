/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.utils;

public class StatConstants {
    private StatConstants() {
    }

    public static final String APP_NAME = "diamond";

    public static final String STAT_AVERAGE_HTTP_GET_OK = "AverageHttpGet_OK";

    public static final String STAT_AVERAGE_HTTP_GET_NOT_MODIFIED = "AverageHttpGet_Not_Modified";

    public static final String STAT_AVERAGE_HTTP_GET_OTHER = "AverageHttpGet_Other_Status";

    public static final String STAT_AVERAGE_HTTP_POST_CHECK = "AverageHttpPost_Check";
    
    public static final String STAT_CLIENT_SUCCESS = "success";
    
    public static final String STAT_CLIENT_FAILURE = "failure";
}
