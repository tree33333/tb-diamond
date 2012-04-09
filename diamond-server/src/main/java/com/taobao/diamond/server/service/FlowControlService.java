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

import com.taobao.diamond.server.utils.SimpleIPFlowData;



public class FlowControlService {
	
	public FlowControlService(){
		
	}
	
	
	int ipHashRange = 2000;	
	int interval = 500;//ms
	int threshold = 20;//invoke per ip
    private SimpleIPFlowData flowData = 	new SimpleIPFlowData(ipHashRange, interval);
    
	public boolean check(String remoteIp){	
	    	flowData.incrementAndGet(remoteIp);
			if(flowData.getCurrentCount(remoteIp) < threshold) {
				return true;
			}
	    	return false;
	}
	
	public int getCurrentCount(String remoteIp){
		return flowData.getCurrentCount(remoteIp);
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
	
	

}
