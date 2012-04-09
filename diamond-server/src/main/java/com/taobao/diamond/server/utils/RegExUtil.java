/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.utils;

import java.util.regex.Pattern;



public class RegExUtil {

	public static boolean isValid(String s){
		try{
              Pattern  p = Pattern.compile(s);
              return true;
		}catch(Exception e){
			return false;
		}
	}
	public static void main(String[] args) {
		String[] regexes = new String[]{"*",".*","\\.","\\"};
		for(String regex:regexes){
       boolean isValid = RegExUtil.isValid(regex);
       System.out.println(regex+":"+isValid);
		}
	}
	
	public static boolean containsRegexChar(String s){
		if(s.indexOf("*")!=-1)return true;
		if(s.indexOf("?")!=-1)return true;
		return false;
	}
	
	public static String doRegularExpressionReplace(String s){
		if(s==null)return s;
		s = s.replaceAll("\\.", "\\\\.");
		s=s.replaceAll("\\*", "\\.\\*");
		s=s.replaceAll("\\?", "\\.{1}");
		return s;
}
}
