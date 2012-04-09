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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.DiskService;
import com.taobao.diamond.server.service.GroupService;

public class CheckServlet extends HttpServlet {
    
    public static final String EMPTY_JSP_PAGE = "empty";

	private static final Log log = LogFactory.getLog("clientLog");
    
    private ConfigController configController;


    @Override
    public void init() throws ServletException {
        super.init();
        WebApplicationContext webApplicationContext =
                WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        configService = (ConfigService) webApplicationContext.getBean("configService");
        this.diskService = (DiskService) webApplicationContext.getBean("diskService");
        this.groupService = (GroupService) webApplicationContext.getBean("groupService");
        configController = new ConfigController();
        this.configController.setConfigService(configService);
        this.configController.setDiskService(diskService);
        this.configController.setGroupService(groupService);
//        if(!StringUtils.isEmptyOrWhitespaceOnly(TimerTaskService.fn))
//        SlfCheck.slfCheck(TimerTaskService.fn);
    }

    private ConfigService configService;

    private DiskService diskService;

    private GroupService groupService;


    /**
     * 查找真实的IP地址
     * 
     * @param request
     * @return
     */
    public String getRemortIP(HttpServletRequest request) {
        if (request.getHeader("x-forwarded-for") == null) {
            return request.getRemoteAddr();
        }
        return request.getHeader("x-forwarded-for");
    }


    public void forward(HttpServletRequest request, HttpServletResponse response, String page, String basePath,
            String postfix) throws IOException, ServletException {
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(basePath + page + postfix);
        requestDispatcher.forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException { 
    	List<ConfigInfo> configInfos = diskService.loadDiskConfigInfo();
    	List<ConfigExtInfo> configExtInfos = new ArrayList<ConfigExtInfo>();
    	for(ConfigInfo configInfo:configInfos){
    		ConfigExtInfo extInfo = new ConfigExtInfo(configInfo);
    		extInfo.setDiskMd5(configInfo.getMd5());
    		String memMd5 = this.configService.getContentMD5(configInfo.getDataId(), configInfo.getGroup());
    		if(memMd5==null){
    			memMd5="";
    		}
    		extInfo.setMemMd5(memMd5);
    		configExtInfos.add(extInfo);
    	}
    	request.setAttribute("configInfos", configExtInfos);
        forward(request, response, "check", "/jsp/", ".jsp");
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
    	doPost(request,response);

    }
}
