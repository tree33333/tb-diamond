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

import com.taobao.diamond.domain.ConfigInfo;

public class ConfigExtInfo extends ConfigInfo {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4630205689270776807L;
	public ConfigExtInfo(ConfigInfo configInfo){
		super(configInfo.getDataId(),configInfo.getGroup(),configInfo.getContent());
	}
	public ConfigExtInfo(){
		super();
	}
	public ConfigExtInfo(String dataId,String group,String content){
		super(dataId,group,content);
	}
	String diskMd5;
	String memMd5;
	public String getDiskMd5() {
		return diskMd5;
	}
	public void setDiskMd5(String diskMd5) {
		this.diskMd5 = diskMd5;
	}
	public String getMemMd5() {
		return memMd5;
	}
	public void setMemMd5(String memMd5) {
		this.memMd5 = memMd5;
	}
	public static ConfigExtInfo convert(ConfigInfo configInfo){
		return new ConfigExtInfo(configInfo);
	}
}
