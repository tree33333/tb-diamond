/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.diamond.common.Constants;


/**
 * 分析url的工具类
 * 
 * @author leiwen.zh
 * 
 */
public class UrlAnalysisUtils {

    private static Pattern urlPattern = Pattern.compile("^(\\w+://)?([\\w\\.]+:)(\\d*)?(\\??.*)");


    public static String getContentIdentity(String content) {

        if (!verifyIncrementPubContent(content)) {
            return null;
        }

        Matcher matcher = urlPattern.matcher(content);
        StringBuilder buf = new StringBuilder();
        if (matcher.find()) {
            String scheme = matcher.group(1);
            String address = matcher.group(2);
            String port = matcher.group(3);
            if (scheme != null) {
                buf.append(scheme);
            }
            buf.append(address);
            if (port != null) {
                buf.append(port);
            }
        }
        return buf.toString();
    }


    private static boolean verifyIncrementPubContent(String content) {

        if (content == null || content.length() == 0) {
            return false;
        }
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\r' || c == '\n') {
                return false;
            }
            if (c == Constants.WORD_SEPARATOR.charAt(0)) {
                return false;
            }
        }
        return true;
    }
}
