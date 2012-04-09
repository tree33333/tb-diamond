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

public class InitPushitClientSingletonErrorException extends RuntimeException {

    static final long serialVersionUID = -1L;


    public InitPushitClientSingletonErrorException() {
        super();

    }


    public InitPushitClientSingletonErrorException(String arg0, Throwable arg1) {
        super(arg0, arg1);

    }


    public InitPushitClientSingletonErrorException(String arg0) {
        super(arg0);

    }


    public InitPushitClientSingletonErrorException(Throwable arg0) {
        super(arg0);

    }

}
