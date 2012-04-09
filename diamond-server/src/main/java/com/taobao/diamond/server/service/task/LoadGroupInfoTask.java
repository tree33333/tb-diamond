/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.server.service.TimerTaskService;
import com.taobao.diamond.server.utils.SystemConfig;


/**
 * 从数据库加载分组信息任务
 * 
 * @author boyan
 * @date 2010-5-10
 */
public final class LoadGroupInfoTask implements Runnable {
    /**
     * 
     */
    private final TimerTaskService timerTaskService;

    static final Log log = LogFactory.getLog(DumpConfigInfoTask.class);

    private static volatile boolean firstLoad = true;

    /**
     * @param timerTaskService
     */
    public LoadGroupInfoTask(TimerTaskService timerTaskService) {
        this.timerTaskService = timerTaskService;
    }


    public void run() {
    	boolean loadGroupInfoSuccess = false;
        try {
            this.timerTaskService.getGroupService().loadGroupInfo();
            loadGroupInfoSuccess = true;
            firstLoad = false;
        }
        catch (Throwable e) {
			if (firstLoad) {
				try {
					this.timerTaskService.getGroupService().loadJSONFile();
				} catch (Exception e2) {
					String msg = "加载本地GroupInfo出错";
					System.out.println(msg);
					String msg2 = "error occured in load local group info.";
					System.out.println(msg2);
					SystemConfig.system_pause();
					System.exit(0);
				}
				firstLoad = false;
			}
        }        
        if(loadGroupInfoSuccess){
        	try {
        	 this.timerTaskService.getGroupService().dumpJSONFile();
        	}catch(Throwable e){
        		log.error("error occured in Dump Group Info ", e);
        		log.error("DUMP分组信息出错", e);        		
        	}
        }
    }
    public LoadGroupInfoTask(){
    	timerTaskService = new TimerTaskService();
    }
    public void runFlyingMode(){
    	try{
    	this.timerTaskService.getGroupService().loadJSONFile();
    	}catch(Exception e){
    		log.error("error occured in runFlyingMode", e);
    	}
    }
}