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

import com.taobao.diamond.server.service.FlowControlService;

public class FlowControlInterceptor extends HandlerInterceptorAdapter implements HandlerInterceptor {
	
	static final Log log = LogFactory.getLog(FlowControlInterceptor.class);   
	
	
	public static int ORDINAL_403_Forbidden = 403;
	@Autowired
	private FlowControlService flowControlService;	
	public FlowControlService getFlowControlService() {
		return flowControlService;
	}
	public void setFlowControlService(FlowControlService flowControlService) {
		this.flowControlService = flowControlService;
	}
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		boolean handlerOk = super.preHandle(request, response, handler);
		if(handlerOk){			
			boolean fcAccess = doACLAndFlowControl(request, response);
			if(!fcAccess) {
				String remoteIp = getRemoteIP(request);	   
				String msg = remoteIp+":trigger flow control:invoke per second:"+getCurrentCount(remoteIp);
				log.warn(msg);				
				System.out.println(msg);
				response.sendError(HttpServletResponse.SC_FORBIDDEN,"over_flow_control");				
				return false;
			}
			return true;
		}
		return false;
	}
	
	private boolean doACLAndFlowControl(HttpServletRequest request,HttpServletResponse response){
		
		 //boolean aclAccess = aCLService.check(remoteIp);
		 String remoteIp = getRemoteIP(request);	       
	      boolean fcAccess = flowControlService.check(remoteIp);
	      return fcAccess;
	}
	private int getCurrentCount(String remoteIp){
		return flowControlService.getCurrentCount(remoteIp);
	}


	private String getRemoteIP(HttpServletRequest request) {
		String remoteIP  = request.getRemoteAddr();
    	if(remoteIP.equals("127.0.0.1")){
    		remoteIP = request.getHeader("X-Real-IP");
    	}
		return remoteIP;
	}

//	public void postHandle(HttpServletRequest request,
//			HttpServletResponse response, Object handler,
//			ModelAndView modelAndView) throws Exception {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public void afterCompletion(HttpServletRequest request,
//			HttpServletResponse response, Object handler, Exception ex)
//			throws Exception {
//		// TODO Auto-generated method stub
//		
//	}

}
