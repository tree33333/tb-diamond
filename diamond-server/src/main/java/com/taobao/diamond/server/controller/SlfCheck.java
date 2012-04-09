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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SlfCheck {
	
	public static  int checkStatusCode(String sURL) {
		try {
			URL url = new URL(sURL);
			
			URLConnection conn = url.openConnection();
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpUrl = (HttpURLConnection) conn;
				return httpUrl.getResponseCode();
			}
			return -1;
		} catch (Exception e) {
			return -1;
		}
	}
	
	public static void main(String[] args) {
		String fn = args[0];
		//String fn = "D:/eclipse-jee-helios-SR2-win32-x86_64/eclipse/slf_check.sh";
		slfCheck(fn);
	
	}

	public static void slfCheck(String fn) {
		Map<Integer,Integer> scMap = new HashMap<Integer,Integer>();
		int current = 0;
		
		List<String> lines = file2List(fn);
		int total = lines.size();
		long timeStart = System.currentTimeMillis();
		for(String url:file2List(fn)){
				int sc = checkStatusCode(url);
				if(sc==404){
					System.out.println("404:"+url);
				}
    			Integer v  = scMap.get(sc);
    			if(v==null){
    				scMap.put(sc,1);
    			}else{
    				scMap.put(sc,v+1);
    			}
    			current++;
    			//System.out.println(current+"/"+total);
    		}
		long timeEnd = System.currentTimeMillis();
		
		long timeUsed = timeEnd-timeStart;
		System.out.println("time used:"+timeUsed);
    		for(Integer key:scMap.keySet()){
    			String stat = key+":"+scMap.get(key);
				//out.println(stat);
    			System.out.println(stat);
//    		}
    		System.out.println("total:"+total);
		}
	}
	public static List<String> file2List(String fileName) {
		List<String> result = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName)));
			String line = br.readLine();
			while (line != null) {
				result.add(line);
				line = br.readLine();
			}
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
