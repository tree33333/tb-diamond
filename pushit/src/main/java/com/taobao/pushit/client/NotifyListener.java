/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.client;

/**
 * 通知监听器
 * 
 * @author boyan
 * @Date 2011-5-25
 * 
 */
public interface NotifyListener {
    /**
     * 通知的回调方法
     * 
     * @param dataId
     * @param group
     * @param message
     */
    public void onNotify(String dataId, String group, String message);
}
