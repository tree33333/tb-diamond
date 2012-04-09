/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.md5.MD5;
import com.taobao.diamond.server.exception.ConfigServiceException;
import com.taobao.diamond.server.utils.SystemConfig;


/**
 * 磁盘操作服务
 * 
 * @author boyan
 * @date 2010-5-4
 */
// @Service
public class DiskService {
	
	private static final Log log = LogFactory.getLog(DiskService.class);

    // @Autowired
    private ServletContext servletContext;


    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * 修改标记缓存
     */
    private final ConcurrentHashMap<String, Boolean> modifyMarkCache = new ConcurrentHashMap<String, Boolean>();


    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    public void saveToDisk(Collection<ConfigInfo> infoList) throws IOException {
        if (infoList != null) {
            for (ConfigInfo info : infoList) {
                saveToDisk(info);
            }
        }
    }


    public void saveFile(final String fileName, String content) throws IOException {

        // 保留策略：
        // 1.最小保证有三个历史文件
        // 2.容量的最大保留
        // 3.配置项最多的保留

        long maxPreferedSpace = 100 * 1024L * 1024L;// 100MB.
        // 按时间由近至远，总共保存maxPrefer容量的历史配置
        // 1.时间离开现在最远的最先被删除

        String dirPath = WebUtils.getRealPath(servletContext, "/");
        File dir = new File(dirPath);
        File[] fs = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(fileName) ? true : false;
            }
        });
        // 按时间倒序排，最近的在最前面
        Comparator<File> sortedByLastModified = new Comparator<File>() {
            public int compare(File o1, File o2) {
                return (int) (o2.lastModified() - o1.lastModified());
            }
        };
        Arrays.sort(fs, sortedByLastModified);
        long currentUsedSpace = 0;
        int totalBackUp = 0;
        int minBackUp = 3;
        long maxCapacitySize = 0;
        long maxItemSize = 0;

        for (File f : fs) {
            boolean spaceFlag = false;
            boolean mininumFlag = false;
            boolean maxCapacity = false;
            boolean maxItems = false;
            mininumFlag = totalBackUp > minBackUp;
            maxCapacitySize = Math.max(maxItemSize, f.length());
            maxCapacity = (f.length() >= maxCapacitySize);

            if (currentUsedSpace > maxPreferedSpace) {
                spaceFlag = true;
            }
            currentUsedSpace += f.length();
            totalBackUp++;

        }

        String realPath = dir + "/" + fileName;
        File file = new File(realPath);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream out = null;
        PrintWriter writer = null;
        try {
            out = new FileOutputStream(file);
            BufferedOutputStream stream = new BufferedOutputStream(out);
            writer = new PrintWriter(stream);
            writer.write(content);
            writer.flush();
        }
        finally {
            if (writer != null)
                writer.close();
        }

    }


    public String readFile(String fileName) throws IOException {
        String realPath = WebUtils.getRealPath(servletContext, "/") + "/" + fileName;
        File file = new File(realPath);
        if (!file.exists()) {
            return "";
        }
        return com.taobao.diamond.utils.FileUtils.getFileContent(realPath);
    }


    /**
     * 仅用于测试
     * 
     * @return
     */
    public ConcurrentHashMap<String, Boolean> getModifyMarkCache() {
        return modifyMarkCache;
    }


    public void saveToDisk(ConfigInfo configInfo) throws IOException {
        if (configInfo == null)
            throw new IllegalArgumentException("configInfo不能为空");
        if (!StringUtils.hasLength(configInfo.getDataId()) || StringUtils.containsWhitespace(configInfo.getDataId()))
            throw new IllegalArgumentException("无效的dataId");

        if (!StringUtils.hasLength(configInfo.getGroup()) || StringUtils.containsWhitespace(configInfo.getGroup()))
            throw new IllegalArgumentException("无效的group");

        if (!StringUtils.hasLength(configInfo.getContent()))
            throw new IllegalArgumentException("无效的content");

        final String basePath = WebUtils.getRealPath(servletContext, Constants.BASE_DIR);
        createDirIfNessary(basePath);
        final String groupPath = WebUtils.getRealPath(servletContext, Constants.BASE_DIR + "/" + configInfo.getGroup());
        createDirIfNessary(groupPath);

        String group = configInfo.getGroup();

        String dataId = configInfo.getDataId();

        dataId = SystemConfig.encodeDataIdForFNIfUnderWin(dataId);

        final String dataPath = WebUtils.getRealPath(servletContext, Constants.BASE_DIR + "/" + group + "/" + dataId);
        File targetFile = createFileIfNessary(dataPath);

        File tempFile = File.createTempFile(group + "-" + dataId, ".tmp");
        FileOutputStream out = null;
        PrintWriter writer = null;
        try {
            out = new FileOutputStream(tempFile);
            BufferedOutputStream stream = new BufferedOutputStream(out);
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, Constants.ENCODE)));
            configInfo.dump(writer);
            writer.flush();
        }
        catch (Exception e) {
        	log.error("保存磁盘文件失败, tempFile:" + tempFile + ",targetFile:" + targetFile, e);
        }
        finally {
            if (writer != null)
                writer.close();
        }

        String cacheKey = generateCacheKey(configInfo.getGroup(), configInfo.getDataId());
        // 标记
        if (this.modifyMarkCache.putIfAbsent(cacheKey, true) == null) {
            try {
                // 文件内容不一样的情况下才进行拷贝
                if (!FileUtils.contentEquals(tempFile, targetFile)) {
                    try {
                        // TODO 考虑使用版本号, 保证文件一定写成功 , 或者知道文件没有写成功。
                        FileUtils.copyFile(tempFile, targetFile);
                    }
                    catch (Throwable t) {
                    	log.error("保存磁盘文件失败, tempFile:" + tempFile + ", targetFile:" + targetFile, t);
                        SystemConfig.system_pause();
                        throw new RuntimeException();

                    }
                }
                tempFile.delete();
            }
            finally {
                // 清除标记
                this.modifyMarkCache.remove(cacheKey);
            }
        }
        else
            throw new ConfigServiceException("配置信息正在被修改");
    }


    public boolean isModified(String dataId, String group) {
        return this.modifyMarkCache.get(generateCacheKey(group, dataId)) != null;
    }


    /**
     * 生成缓存key，用于标记文件是否正在被修改
     * 
     * @param group
     * @param dataId
     * 
     * @return
     */
    public final String generateCacheKey(String group, String dataId) {
        return group + "/" + dataId;
    }


    public void removeConfigInfo(String dataId, String group) throws IOException {
        if (!StringUtils.hasLength(dataId) || StringUtils.containsWhitespace(dataId))
            throw new IllegalArgumentException("无效的dataId");

        if (!StringUtils.hasLength(group) || StringUtils.containsWhitespace(group))
            throw new IllegalArgumentException("无效的group");

        final String basePath = WebUtils.getRealPath(servletContext, Constants.BASE_DIR);
        createDirIfNessary(basePath);
        final String groupPath = WebUtils.getRealPath(servletContext, Constants.BASE_DIR + "/" + group);
        final File groupDir = new File(groupPath);
        if (!groupDir.exists()) {
            return;
        }
        // 这里并没有去判断group目录是否为空并删除，也就是说哪怕group目录内没有任何dataId文件了也将仍然保留在磁盘上
        String fnDataId = SystemConfig.encodeDataIdForFNIfUnderWin(dataId);
        final String dataPath = WebUtils.getRealPath(servletContext, Constants.BASE_DIR + "/" + group + "/" + fnDataId);
        File dataFile = new File(dataPath);
        if (!dataFile.exists()) {
            return;
        }
        String cacheKey = generateCacheKey(group, dataId);
        // 标记
        if (this.modifyMarkCache.putIfAbsent(cacheKey, true) == null) {
            try {
                if (!dataFile.delete())
                    throw new ConfigServiceException("删除配置文件失败");
            }
            finally {
                this.modifyMarkCache.remove(cacheKey);
            }
        }
        else
            throw new ConfigServiceException("配置文件正在被修改");
    }


    public void removeConfigInfo(ConfigInfo configInfo) throws IOException {
        removeConfigInfo(configInfo.getDataId(), configInfo.getGroup());
    }


    private void createDirIfNessary(String path) {
        final File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }


    private File createFileIfNessary(String path) throws IOException {
        final File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (Exception e) {
            	log.error("创建文件失败, path=" + path, e);
            }
        }
        return file;
    }


    public static void main(String[] args) throws Exception {
        int c = 10;
        List<File> fs = new ArrayList<File>();
        for (int i = 0; i < c; i++) {
            File file = new File("" + i);
            file.createNewFile();
            fs.add(file);
            // Thread.sleep(10);
        }
        Comparator<File> sortedByLastModified = new Comparator<File>() {
            public int compare(File o1, File o2) {
                return (int) (o2.lastModified() - o1.lastModified());
            }
        };
        Collections.sort(fs, sortedByLastModified);
        for (File f : fs) {
            System.out.println(f.getName());
            f.delete();
        }
        int index = 1;
        NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
        Enumeration e = networkInterface.getNetworkInterfaces();

        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
    }


    public List<ConfigInfo> loadDiskConfigInfo() throws FileNotFoundException {
        List<ConfigInfo> configInfos = new ArrayList<ConfigInfo>();
        String basePath = WebUtils.getRealPath(this.servletContext, Constants.BASE_DIR);
        File file = new File(basePath);
        if (file.exists()) {

            for (File group : file.listFiles()) {

                for (File dataId : group.listFiles()) {
                    String groupName = group.getName();
                    String dataIdName = dataId.getName();
                    dataIdName = SystemConfig.decodeFnForDataIdIfUnderWin(dataIdName);
                    try {
                        String content = com.taobao.diamond.utils.FileUtils.getFileContent(dataId.getAbsolutePath());
                        ConfigInfo configInfo = new ConfigInfo(dataIdName, groupName, content);
                        configInfos.add(configInfo);
                    }
                    catch (IOException e) {
                        // log.error("加载ConfigInfo失败:dataId:"+dataIdName+":"+groupName);
                        // log.error(e.getMessage(),e.getCause());
                    }
                }
            }
        }
        return configInfos;
    }


    public String getContentMD5(String dataId, String group) throws FileNotFoundException, IOException {
        String basePath = WebUtils.getRealPath(this.servletContext, Constants.BASE_DIR);
        String fnDataId = SystemConfig.decodeFnForDataIdIfUnderWin(dataId);
        File file = new File(basePath + "/" + group + "/" + fnDataId);
        if (file.exists()) {
            try {
                String content = com.taobao.diamond.utils.FileUtils.getFileContent(file.getAbsolutePath());
                ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
                return MD5.getInstance().getMD5String(content);
            }
            catch (IOException e) {
                throw e;
            }
        }

        else {
            throw new FileNotFoundException(file.getAbsolutePath() + " is not exist!");
        }

    }

}
