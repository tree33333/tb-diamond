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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.utils.ContentUtils;


/**
 * 
 * 增量发布数据缺省的格式为： [scheme://]address:[port]?query
 * 
 * @author mazhen
 * 
 */

public class URLContentIdentityPattern implements ContentIdentityPattern {

    private Pattern pattern = Pattern.compile("^(\\w+://)?([\\w\\.]+:)(\\d*)?(\\??.*)");


    public String getContentIdentity(String content) {

        ContentUtils.verifyIncrementPubContent(content);

        Matcher matcher = pattern.matcher(content);
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
        else {
            throw new IllegalArgumentException("发布的数据不符合格式要求[scheme://]address:[port]?query ," + content);
        }
        return buf.toString();
    }

}
