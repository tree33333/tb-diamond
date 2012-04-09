/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.taobao.diamond.server.service.AdminService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.DiskService;
import com.taobao.diamond.server.service.GroupService;


/**
 * 
 * @author zhidao
 *
 */
@Controller
@RequestMapping("/check.do")
public class CheckController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ConfigService configService;
    @Autowired
    private DiskService diskService;

    public DiskService getDiskService() {
		return diskService;
	}


	public void setDiskService(DiskService diskService) {
		this.diskService = diskService;
	}


	static final Log log = LogFactory.getLog(CheckController.class);


   






    

    /**
     * ²éÑ¯ÅäÖÃÐÅÏ¢
     * 
     * @param request
     * @param dataId
     * @param group
     * @param pageNo
     * @param pageSize
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=checkConfig", method = RequestMethod.GET)
    public String checkConfig(HttpServletRequest request, @RequestParam("dataId") String dataId,
            @RequestParam("group") String group,ModelMap modelMap) {
        String page = "";
      try{
    	  String diskMd5 = this.diskService.getContentMD5(dataId,group);      
    	  String memMd5   = this.configService.getContentMD5(dataId, group);
    	  page = "diskMd5:"+diskMd5;
    	  page       += "memMd5:"+memMd5;
      }catch(Exception e){
    	  page = e.getMessage();
      } 
        modelMap.put("page", page);
        return "/check_json";
        
    }
    
    @RequestMapping(params = "method=getMD5", method = RequestMethod.GET)
    public String getMD5(HttpServletRequest request, @RequestParam("dataId") String dataId,
            @RequestParam("group") String group,ModelMap modelMap) {
        String expectedMd5 = this.configService.getContentMD5(dataId, group); 
        String page = "";
        modelMap.put("page",page);
        return "/check_json";
        
    }
    
    
	public int checkStatusCode(String sURL) {
		try {
			URL url = new URL(sURL);
			URLConnection conn = url.openConnection();
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpUrl = (HttpURLConnection) conn;
				return httpUrl.getResponseCode();
			}
			return -1;
		} catch (Exception e) {
			return -1;
		}
	}
    public static String url2str(String sURL,String encoding) {
		try {
			URL url = new URL(sURL);
			URLConnection conn = url.openConnection();
if(conn instanceof HttpURLConnection){
	HttpURLConnection httpUrl = (HttpURLConnection)conn;
	httpUrl.getResponseCode();
}
			InputStream is = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					encoding));
			String r = "";
			char [] cbuf = new char[1024];
			while ( br.read(cbuf)>0){
				r +=new String(cbuf);
			}
			return r;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


    


    public AdminService getAdminService() {
        return adminService;
    }


    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }


    public GroupService getGroupService() {
        return groupService;
    }


    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

}
