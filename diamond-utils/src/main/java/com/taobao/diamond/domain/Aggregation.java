/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.domain;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.taobao.diamond.common.Constants;


public class Aggregation {

    private final static String LINE_HEAD = "LINE_HEAD";

    private final static String LINE_END = "LINE_END";

    private final static String lineSeparator = Constants.DIAMOND_LINE_SEPARATOR;


    public ConfigInfo getConfigInfo() {
        System.out.println();
        return configInfo;
    }


    public void setConfigInfo(ConfigInfo configInfo) {
        this.configInfo = configInfo;
    }


    public ConfigInfo getAggregation() {
        return aggregation;
    }


    public void setAggregation(ConfigInfo aggregation) {
        this.aggregation = aggregation;
    }

    ConfigInfo configInfo;
    ConfigInfo aggregation;

    List<ConfigInfo> items = new ArrayList<ConfigInfo>();


    public List<ConfigInfo> getItems() {
        return items;
    }


    public Aggregation(ConfigInfo configInfo, ConfigInfo aggregation) {
        this.configInfo = configInfo;
        this.aggregation = aggregation;
    }


    public Aggregation() {

    }


    public void addItem(ConfigInfo configInfo) {
        items.add(configInfo);
    }


    public String generateContent() {

        String content = "";
        for (ConfigInfo info : items) {
            content += LINE_HEAD;
            content += lineSeparator;
            content += info.getDataId();
            content += lineSeparator;
            content += info.getGroup();
            content += lineSeparator;
            if (info.getMd5() == null) {
                content += "";
            }
            else {
                content += info.getMd5();
            }
            content += lineSeparator;
            if (info.getContent() == null) {
                content += "";
            }
            else {
                content += info.getContent();
            }
            content += lineSeparator;
            content += LINE_END;
            content += lineSeparator;
        }
        return content;
    }


    public static Map<String, ConfigInfo> parseAsMap(String aggregation) {
        return convert(parse(aggregation));
    }


    @SuppressWarnings("unchecked")
    public static List<ConfigInfo> parse(String aggregation) {
        List<ConfigInfo> result = new ArrayList<ConfigInfo>();
        StringReader reader = null;
        try {
            reader = new StringReader(aggregation);
            List<String> lines = IOUtils.readLines(reader);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(LINE_HEAD)) {
                    ConfigInfo configInfo = new ConfigInfo();
                    if (++i < lines.size()) {
                        configInfo.setDataId(lines.get(i));
                    }
                    if (++i < lines.size()) {
                        configInfo.setGroup(lines.get(i));
                    }
                    if (++i < lines.size()) {
                        if (!lines.get(i).equals(""))
                            configInfo.setMd5(lines.get(i));
                    }
                    String content = "";
                    int innerIndex = 0;
                    while (++i < lines.size()) {
                        line = lines.get(i);
                        if (line.equals(LINE_END))
                            break;
                        if (innerIndex++ > 0)
                            content += lineSeparator;
                        content += line;
                    }
                    if (!content.equals("")) {
                        configInfo.setContent(content);
                    }
                    result.add(configInfo);
                }
            }
        }
        catch(IOException e) {
            return parseAfterException(aggregation);
        }
        finally {
            reader.close();
        }
        
        return result;
    }


    private static List<ConfigInfo> parseAfterException(String aggregation) {
        List<ConfigInfo> result = new ArrayList<ConfigInfo>();
        String[] lines = aggregation.split(lineSeparator);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith(LINE_HEAD)) {
                ConfigInfo configInfo = new ConfigInfo();
                if (++i < lines.length) {
                    configInfo.setDataId(lines[i]);
                }
                if (++i < lines.length) {
                    configInfo.setGroup(lines[i]);
                }
                if (++i < lines.length) {
                    if (!lines[i].equals(""))
                        configInfo.setMd5(lines[i]);
                }
                String content = "";
                int innerIndex = 0;
                while (++i < lines.length) {
                    line = lines[i];
                    if (line.equals(LINE_END))
                        break;
                    if (innerIndex++ > 0)
                        content += lineSeparator;
                    content += line;
                }
                if (!content.equals("")) {
                    configInfo.setContent(content);
                }
                result.add(configInfo);
            }
        }
        return result;
    }
    
    
    private static String getSuffix(String dataId) {
        int pos = dataId.length() - 1;
        while (pos > -1 && dataId.charAt(pos) != '.')
            pos--;
        if (pos == -1)
            return null;
        return dataId.substring(pos);
    }


    public static Map<String, ConfigInfo> convert(List<ConfigInfo> configInfos) {
        Map<String, ConfigInfo> result = new HashMap<String, ConfigInfo>();
        for (ConfigInfo configInfo : configInfos) {
            String suffix = getSuffix(configInfo.getDataId());
            if (suffix == null)
                continue;
            result.put(suffix, configInfo);
        }
        return result;
    }
}
