/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.exception;

public class PushitCodecException extends RuntimeException{

    /**
     * 
     */
    private static final long serialVersionUID = -2209044084701362520L;

    public PushitCodecException() {
        super();
      
    }

    public PushitCodecException(String message, Throwable cause) {
        super(message, cause);
      
    }

    public PushitCodecException(String message) {
        super(message);
      
    }

    public PushitCodecException(Throwable cause) {
        super(cause);
      
    }

}
