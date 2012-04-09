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


import java.util.ArrayList;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;

public class SelectedAnnotationHandlerMapping extends DefaultAnnotationHandlerMapping
{

    public SelectedAnnotationHandlerMapping()
    {
    }

    public void setUrls(ArrayList arraylist)
    {
        a = arraylist;
    }

    public String[] getFiltered(String as[])
    {
        if(as == null)
            return null;
        ArrayList arraylist = new ArrayList();
        String as1[] = as;
        int i = as1.length;
        for(int j = 0; j < i; j++)
        {
            String s = as1[j];
            if(a.contains(s))
                arraylist.add(s);
        }

        return (String[])arraylist.toArray(new String[arraylist.size()]);
    }

    protected String[] determineUrlsForHandler(String s)
    {
        return getFiltered(super.determineUrlsForHandler(s));
    }

    private ArrayList a;
}