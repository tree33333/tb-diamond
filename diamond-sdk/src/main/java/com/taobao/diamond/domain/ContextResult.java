/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.domain;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据dataId,groupName精确查询返回的对象
 * 
 * @filename ContextResult.java
 * @author libinbin.pt
 * @datetime 2010-7-15 下午06:49:12
 */
/**
 * 
 * @filename ContextResult.java
 * @author libinbin.pt
 * @param <T>
 * @datetime 2010-7-16 下午05:48:54
 */
@SuppressWarnings("serial")
public class ContextResult implements Serializable {
	private boolean isSuccess = false;    		// 是否成功
	private int statusCode; 			  		// 状态码
	private String statusMsg = ""; 			  	// 状态信息
	private ConfigInfo configInfo=null;         // 配置对象包括[内容，dataId，groupName]
	private GroupInfo groupInfo=null;
	private String receiveResult=null;	  		// 回传信息
	private List<GroupInfo> receive;
	private ConcurrentHashMap<String,ConcurrentHashMap<String,GroupInfo>> map;

    
    public ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> getMap() {
        return map;
    }

    public void setMap(ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> map) {
        this.map = map;
    }

    public List<GroupInfo> getReceive() {
        return receive;
    }
    
    public void setReceive(List<GroupInfo> receive) {
		this.receive = receive;
	}

	public GroupInfo getGroupInfo() {
		return groupInfo;
	}

	public void setGroupInfo(GroupInfo groupInfo) {
		this.groupInfo = groupInfo;
	}

	public ContextResult() {

	}
	public boolean isSuccess() {
		return isSuccess;
	}


	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}


	public int getStatusCode() {
		return statusCode;
	}


	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}


	public String getStatusMsg() {
		return statusMsg;
	}


	public void setStatusMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}


	public ConfigInfo getConfigInfo() {
		return configInfo;
	}

	public void setConfigInfo(ConfigInfo configInfo) {
		this.configInfo = configInfo;
	}


	public String getReceiveResult() {
		return receiveResult;
	}


	public void setReceiveResult(String receiveResult) {
		this.receiveResult = receiveResult;
	}

	
	@Override
	public String toString() {
		return "[" + 
				"statusCode="+statusCode+
				",isSuccess="+isSuccess+
				",statusMsg="+statusMsg+
				",receiveResult="+receiveResult+
				",[groupInfo="+groupInfo+ "]"+
				",[configInfo="+configInfo+ "]]";
	}

}
