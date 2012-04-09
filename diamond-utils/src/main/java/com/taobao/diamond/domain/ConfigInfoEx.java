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

/**
 * ConfigInfo的扩展类, 解决老版本的SDK与新版本的server由于反序列化字段错误而产生的不兼容问题
 * 
 * @author leiwen.zh
 * 
 */
public class ConfigInfoEx extends ConfigInfo {

    private static final long serialVersionUID = -1L;

    private String oldMd5;
    // 批量查询时, 单条数据的状态码, 具体的状态码在Constants.java中
    private int status;
    // 批量查询时, 单条数据的信息
    private String message;


    public ConfigInfoEx() {
        super();
    }


    public ConfigInfoEx(String dataId, String group, String content) {
        super(dataId, group, content);
    }


    public String getOldMd5() {
        return oldMd5;
    }


    public void setOldMd5(String oldMd5) {
        this.oldMd5 = oldMd5;
    }


    public int getStatus() {
        return status;
    }


    public void setStatus(int status) {
        this.status = status;
    }


    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    @Override
    public String toString() {
        return "ConfigInfoEx dataId=" + super.getDataId() + ",group=" + super.getGroup() + ",content="
                + super.getContent() + ",md5=" + super.getMd5() + ",oldMd5=" + oldMd5 + ",status=" + status
                + ",message=" + message;
    }

}
