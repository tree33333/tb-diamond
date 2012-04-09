/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.exception;

/**
 * Diamond发布数据异常类
 * @author leiwen
 *
 */
public class DiamondPublisherException extends Exception {

	private static final long serialVersionUID = 1L;

	public DiamondPublisherException(String msg) {
		super(msg);
	}
}
