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



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;



//import com.jcraft.jsch.Channel;
//import com.jcraft.jsch.ChannelSftp;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.JSchException;
//import com.jcraft.jsch.Session;
//import com.jcraft.jsch.SftpException;

/**
 * 
 * FileUtil.java
 * 
 * @author zhidao
 * @since 2009-9-3
 */
public class ZFileUtil {
	
	private static final String UTF_8 = "utf-8";

	public static List<String> url2list(String sURL,String encoding) {
		try {
			URL url = new URL(sURL);
			URLConnection conn = url.openConnection();

			InputStream is = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					encoding));
			List<String> list  = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			return list;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<String> url2list(String sURL){
		return url2list(sURL,UTF_8);
	}
	
	
	
	public static String md5( String s)
	throws NoSuchAlgorithmException {
String result = "";
java.security.MessageDigest m = java.security.MessageDigest.getInstance("MD5");
  m.update(s.getBytes(), 0, s.length());	      
  byte[] bytes = m.digest();
  for(int i=0;i<bytes.length;i++){
	  String vs = Integer.toHexString(bytes[i]&0xff);
	  if(vs.length()<2){
		  vs ='0'+vs;
	  }	    	  
	  result +=vs;
  }
return result;
}
	
	
	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	public static void writeFile(byte[] data, String fileName)
			throws IOException {
		OutputStream out = new FileOutputStream(fileName);
		out.write(data);
		out.close();
	}

	public static void writeFile(byte[] data, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		out.write(data);
		out.close();
	}

	public static void write2File(String content, String fileName)
			throws IOException {
		PrintStream out = new PrintStream(fileName, UTF_8);
		out.println(content);
		out.close();

	}
	
	public static void write2File(String[] lines, String fileName)
	throws IOException {
PrintStream out = new PrintStream(fileName);
for (String line : lines)
	out.println(line);
out.close();

}

	public static void write2File(List<String> lines, String fileName)
			throws IOException {
		PrintStream out = new PrintStream(fileName);
		for (String line : lines)
			out.println(line);
		out.close();

	}
	
	public static void write2File(Map<String,String> map, String fileName)
	throws IOException {
	PrintStream out = new PrintStream(fileName);
	for (Entry<String,String> e : map.entrySet())
		out.println(e.getKey()+"\t"+e.getValue());
				
	out.close();
	
	}

	public static void write2File(String content, String fileName,
			boolean append) throws IOException {
		PrintStream out = new PrintStream(
				new FileOutputStream(fileName, append));
		out.println(content);
		out.close();

	}

	public static void Copy(String oldPath, String newPath) {
		try {
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(oldPath);
			if (oldfile.exists()) {
				InputStream inStream = new FileInputStream(oldPath);
				File file = new File(newPath);
				if (!file.exists()) {
					file.createNewFile();
				}
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1444];
				int length;
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					System.out.println(bytesum);
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	// deal for jar file
	// 2009-09-03
	public static void processJarFile(String f) {
		try {
			JarFile jarF = new JarFile(f);
			Enumeration<JarEntry> je = jarF.entries();
			while (je.hasMoreElements()) {
				JarEntry entry = je.nextElement();
				String name = entry.getName();
				long size = entry.getSize();
				long compressedSize = entry.getCompressedSize();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String fileToString(String fileName) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName)));
			String line = br.readLine();
			while (line != null) {
				result += line;
				line = br.readLine();

			}
			br.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * will lost chr(13) chr(10) and etc...
	 * 
	 * @param filePath
	 * @return
	 * @throws java.io.IOException
	 */
	public static String readFileAsString(String filePath)
			throws java.io.IOException {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		FileInputStream f = new FileInputStream(filePath);
		f.read(buffer);
		return new String(buffer);
	}

	/**
	 * from http://www.cs.helsinki.fi/group/converge/
	 * webcore.utilities.FileUtils
	 * 
	 * @param fileName
	 * @return
	 * @throws java.io.IOException
	 */
	public static String readFileAsStringV2(String fileName)
			throws java.io.IOException {
		return readFileAsStringV2(new File(fileName));

	}

	public static String readFileAsStringV2(File file)
			throws java.io.IOException {
		java.io.DataInputStream in = new java.io.DataInputStream(
				new java.io.FileInputStream(file));
		String result = readInputStreamAsString(in);
		return result;
	}

	public static String readInputStreamAsString(java.io.DataInputStream in)
			throws IOException {
		byte b[] = new byte[in.available()];
		in.readFully(b);
		String result =  new String(b);
		return result;
	}

	/**
	 * write by myself(zhidao@taobao.com)
	 * 
	 * @param fileName
	 * @return
	 * @throws java.io.IOException
	 */
	public static String readFileAsStringByByete(String fileName)
			throws java.io.IOException {

		StringBuilder builder = new StringBuilder();
		InputStream is = new FileInputStream(fileName);
		String s = readInputStreamAsString(is);
		return s;
	}

	public static String readInputStreamAsString(InputStream in) throws FileNotFoundException, IOException {
		InputStreamReader fr = new InputStreamReader(in);
		char[] cbuf = new char[1024];

		int len = fr.read(cbuf);
		StringBuilder builder  = new StringBuilder();
		while (len > 0) {
			builder.append(cbuf, 0, len);
			len = fr.read(cbuf);
		}
		String s =  builder.toString();
		return s;
	}

	public static List<String> file2List(InputStream is) {
		List<String> result = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
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

	public static Map<String, List<String>> parseFile(String fileName) {
		Map<String, List<String>> res = new HashMap<String, List<String>>();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileName)));
			String line = null;
			String appName = null;
			while ((line = in.readLine()) != null) {
				if (line.indexOf("app:") != -1) {
					appName = line.split(":")[1];
					continue;
				}
				String[] lines = line.split("\t");
				List<String> ipList = res.get(appName);
				if (ipList == null) {
					ipList = new ArrayList<String>();
				}
				for (String lline : lines) {
					ipList.add(lline);
				}
				res.put(appName, ipList);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	public static PrintStream newPrintStream(String fileName)
			throws IOException {
		PrintStream out = new PrintStream(new FileOutputStream(fileName));
		return out;
	}

	public static String stream2String(InputStream is) throws IOException {
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
		String line = br.readLine();
		String result = "";
		while (line != null) {
			result += line;
			line = br.readLine();
		}
		br.close();
		return result;
	}

	public static void debug(String content) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream("debug.txt",
					true));
			out.println(content);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String url2String(String sURL) {
		try {
			URL url = new URL(sURL);
			URLConnection conn = url.openConnection();

			InputStream is = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is,
					UTF_8));
			String jsonString = "";
			String line;
			while ((line = br.readLine()) != null) {
				jsonString += line;
			}
			return jsonString;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String url2File(String sURL) {
		try {
			URL url = new URL(sURL);
			URLConnection conn = url.openConnection();

			InputStream is = conn.getInputStream();
			byte[] bytes = new byte[1024];
			File file = new File(""+System.currentTimeMillis()+"");
			if(!file.exists()){
				file.createNewFile();
			}
			int len = is.read(bytes);
			System.out.println("len:"+len);
			FileOutputStream fos = new FileOutputStream(file);
			while(len!=-1){
				fos.write(bytes,0, len);
				len = is.read(bytes);
				System.out.println("len:"+len);
			}
			return file.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	

	public static void url2File(String sURL, String fileName) {
		try {
			ZFileUtil.write2File(url2String(sURL), fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private InputStream loadFile(String fn) {
		InputStream is = this.getClass().getResourceAsStream(fn);
		InputStream is2 = this.getClass().getClassLoader().getResourceAsStream(
				"/" + fn);
		if (is != null)
			return is;
		if (is2 != null)
			return is2;
		return null;
	}
	
	
	public static  void wget(String sURL,String fileName)throws IOException{
		String content = url2String(sURL);
		write2File(content,fileName);
	}
	
//	public  void scp(String host, String userName, String password,
//			String remoteFile, String localFile) {
//		JSch jsch = new JSch();
//        Session session = null;
//        try {
//            session = jsch.getSession(userName, host, 22);
//            Hashtable table = new Hashtable();
//            table.put("StrictHostKeyChecking", "no");
//            session.setConfig(table);
//            session.setPassword(password);
//            session.connect();
//            Channel channel = session.openChannel("sftp");
//            channel.connect();
//            ChannelSftp sftpChannel = (ChannelSftp) channel;
//			sftpChannel.get(remoteFile, localFile);
//            sftpChannel.exit();
//            session.disconnect();
//        } catch (JSchException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (SftpException e) {
//            e.printStackTrace();
//        }
//	}
	
	
}