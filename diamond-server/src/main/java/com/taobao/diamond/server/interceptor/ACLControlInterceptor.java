/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.taobao.diamond.server.service.ACLService;
import com.taobao.diamond.server.service.FlowControlService;

public class ACLControlInterceptor extends HandlerInterceptorAdapter implements HandlerInterceptor {
	
	static final Log log = LogFactory.getLog(ACLControlInterceptor.class);   
	
	
	public static int ORDINAL_403_Forbidden = 403;
	@Autowired
	private ACLService aCLService;	
	
	
	
	public ACLService getaCLService() {
		return aCLService;
	}
	public void setaCLService(ACLService aCLService) {
		this.aCLService = aCLService;
	}
	long tpsMax = 0;
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		boolean handlerOk = super.preHandle(request, response, handler);
		if(handlerOk){	
			String remoteIp = getRemoteIP(request);	 
			boolean aclAccess = aCLService.check(remoteIp);
			if(!aclAccess) {	
				String msg = "["+remoteIp+"] access denied by ACL Control";
				System.out.println(msg);
				log.warn(msg);
				response.sendError(HttpServletResponse.SC_FORBIDDEN,"acl_control_denied");				
				return false;
			}
			return true;
		}
		return false;
	}
	private String getRemoteIP(HttpServletRequest request) {
		String remoteIP  = request.getRemoteAddr();
    	if(remoteIP==null || remoteIP.equals("127.0.0.1")){
    		remoteIP = request.getHeader("X-Real-IP");
    	}
		return remoteIP;
	}
}
