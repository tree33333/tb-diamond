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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.server.utils.RegExUtil;
import com.taobao.diamond.server.utils.ZFileUtil;
import com.taobao.diamond.utils.ResourceUtils;


public class ACLService {
    static final Log log = LogFactory.getLog(ACLService.class);


    public ACLService() {

    }

    private List<String> aclIps = new ArrayList<String>();
    private boolean allPass = false;
    {
        try {
            InputStream is = ResourceUtils.getResourceAsStream("acl.properties");
            aclIps = ZFileUtil.file2List(is);
            for (String p : aclIps) {
                if (p.trim().equals("*")) {
                    allPass = true;
                }
            }
        }
        catch (Exception e) {
            log.error("acl.properties is not exist.");
        }

    }


    public boolean check(String remoteIp) {
        if (allPass)
            return true;
        if (remoteIp == null)
            return false;
        for (String ip : aclIps) {
            String regex = RegExUtil.doRegularExpressionReplace(ip);
            if (remoteIp.equals(ip)) {
                return true;
            }
            if (remoteIp.matches(regex)) {
                return true;
            }
        }
        return false;
    }

}
