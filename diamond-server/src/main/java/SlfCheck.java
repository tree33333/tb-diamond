/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class SlfCheck extends Thread{
	static String host = "localhost";
	static String port = "8080";
	CountDownLatch latch;
	int left;
	int right;
		volatile int _200Cnt;
		volatile int _404Cnt;
		volatile int _otherCnt;
		long timeCost = 0;
		
		public void dealSc(int code,String url){
			if(code==200)_200Cnt++;
			else if(code==404){
				_404Cnt++;
				System.out.println("404:"+url);
			}
			else _otherCnt++;
		}
		
	List<String> lines = null;
	
	public SlfCheck(List<String> lines,int left,int right,CountDownLatch latch){
		this.lines = lines;
		this.left = left;
		this.right = right;
		this.latch = latch;
	}
	
	public static  int checkStatusCode(String sURL) {
		//long t = System.nanoTime();
		int ret =-1;
		try {
			URL url = new URL(sURL);
			
			URLConnection conn = url.openConnection();
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpUrl = (HttpURLConnection) conn;
				ret =  httpUrl.getResponseCode();
			}
			
		} catch (Exception e) {
			ret = -1;
		}
		//long t2 = System.nanoTime();
		//System.out.println(t2-t);
		return ret;
	}
	
	public static void main(String[] args) throws Exception{
		int index = 0;
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-h")){
				if(i+1<args.length){
					host = args[i+1];
				}
			}
		}
		System.out.print("fn:"+args[0]);
		System.out.print("host:"+host);
		int processors = Runtime.getRuntime().availableProcessors();
		System.out.println("processors:"+processors);
		String fn = args[0];
		long time = System.currentTimeMillis();
		System.out.println("fn:"+args[0]);
		
		List<String> lines = file2List(fn);
		
		int nLines = lines.size();
		
		System.out.println("total lines:"+nLines);
		int nThreads = processors*2;
		int linePerThread  = nLines/nThreads;
		int left = 0;
		int right = left+linePerThread;
		SlfCheck[] checkers = new SlfCheck[nThreads];
		CountDownLatch latch = new CountDownLatch(nThreads);
		for(int i=0;i<nThreads;i++){
			SlfCheck checker = new SlfCheck(lines,left,right,latch);
			left+=linePerThread;
			right = left+linePerThread;
			checker.start();
			checkers[i] = checker;
			
		}
		latch.await();
		long time2 = System.currentTimeMillis();
		int _200  = 0;
		int _404 = 0;
		int _other = 0;
		
		for(int i=0;i<nThreads;i++){
			_200+=checkers[i]._200Cnt;
			_404+=checkers[i]._404Cnt;
			
			_other+= checkers[i]._otherCnt;
			
			//System.out.println("200:"+_200);
			//System.out.println("200:"+_404);
			//System.out.println("other:"+_other);
			
		}
		System.out.println("total 200:"+_200);
		System.out.println("total 404:"+_404);
		System.out.println("total other:"+_other);
		
		System.out.println("total time used:"+(time2-time)+" ms");
	
	}
private String assemble(String line){
	if(line.startsWith("http:"))return line;
	String[] args = line.split("\t");
	String dataId = args[1];
	String group = args[0];
	return "http://"+host+":"+port+"/diamond-server/config.do?group="+group+"&dataId="+dataId;
}
	public  void run() {
		int index = 0;
		long timeStart = System.currentTimeMillis();
		for(int lineNo = left;lineNo<right;lineNo++){
			String line = lines.get(lineNo);
			line = assemble(line);
			//System.out.println(line);
			int sc = checkStatusCode(line);		
			//System.out.println("status:"+sc);
				dealSc(sc,line);
				index++;
				
    			if(index%100==0)
    			System.out.println(lineNo+"/["+left+"-"+right+"]");
    			
    	}
		long timeEnd = System.currentTimeMillis();
		timeCost = timeEnd-timeStart;
		System.out.println("deal ["+left+"-"+right+"]"+" used:"+timeCost+" ms");
		latch.countDown();
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

