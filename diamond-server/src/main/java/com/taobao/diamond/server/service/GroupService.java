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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.server.utils.DiamondUtils;
import com.taobao.diamond.utils.JSONUtils;


//@Service
public class GroupService {

    public GroupService() {

    }

    // @Autowired
    // @Qualifier("persistService")
    private PersistService persistService;


    public NotifyService getNotifyService() {
        return notifyService;
    }


    public PersistService getPersistService() {
        return persistService;
    }

    private static final Log log = LogFactory.getLog(GroupService.class);

    private static final String WILDCARD_CHAR = "*";

    // @Autowired
    private NotifyService notifyService;

    // @Autowired
    private DiskService diskService;

    private volatile ConcurrentHashMap<String/* address */, ConcurrentHashMap<String/* dataId */, GroupInfo/* groupInfo */>> addressGroupCache =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>();


    public void setNotifyService(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    public void setPersistService(PersistService persistService) {
        this.persistService = persistService;
    }


    public DiskService getDiskService() {
        return diskService;
    }


    public void setDiskService(DiskService diskService) {
        this.diskService = diskService;
    }


    /**
     * 将分组信息dump成json文件
     * 
     * @throws Exception
     */
    public void dumpJSONFile() throws Exception {
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();

        for (Map.Entry<String, ConcurrentHashMap<String, GroupInfo>> entry : this.addressGroupCache.entrySet()) {
            final String address = entry.getKey();
            HashMap<String, String> subMap = new HashMap<String, String>();
            map.put(address, subMap);
            final ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = entry.getValue();
            for (Map.Entry<String, GroupInfo> dataIdGroupEntry : dataIdGroupMap.entrySet()) {
                subMap.put(dataIdGroupEntry.getKey(), dataIdGroupEntry.getValue().getGroup());
            }
        }

        String serializedGroupInfo = JSONUtils.serializeObject(map);
        this.diskService.saveFile(Constants.MAP_FILE, serializedGroupInfo);

    }


    /**
     * 从数据库加载分组信息
     */
    // @PostConstruct
    public void loadGroupInfo() {
        List<GroupInfo> allGroupInfo = persistService.findAllGroupInfo();
        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> tempMap =
                new ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>();
        log.warn("开始从数据库加载分组信息...");
        if (allGroupInfo != null) {
            for (GroupInfo info : allGroupInfo) {
                String address = info.getAddress();
                String dataId = info.getDataId();
                if (tempMap.get(address) == null) {
                    tempMap.put(address, new ConcurrentHashMap<String, GroupInfo>());
                }
                tempMap.get(address).put(dataId, info);
            }
        }
        this.addressGroupCache = tempMap;
        log.warn("加载分组信息完成...总共加载" + (allGroupInfo != null ? allGroupInfo.size() : 0));
    }


    /**
     * 根据ip查找分组信息
     * 
     * @param address
     * @param dataId
     * @param clientGroup
     *            客户端指定的分组信息
     * @return
     */
    public String getGroupByAddress(String address, String dataId, String clientGroup) {
        ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = addressGroupCache.get(address);
        if (dataIdGroupMap != null) {
            GroupInfo groupInfo = dataIdGroupMap.get(dataId);
            if (groupInfo != null) {
                return groupInfo.getGroup();
            }
            else {
                return defaultGroupOrClientGroup(clientGroup);
            }
        }
        else {
            return defaultGroupOrClientGroup(clientGroup);
        }

        // 优先精确匹配
        /*
         * ConcurrentHashMap<String, GroupInfo> dataIdGroupMap =
         * addressGroupCache.get(address); if (dataIdGroupMap != null) {
         * GroupInfo groupInfo = dataIdGroupMap.get(dataId); if (groupInfo !=
         * null) { return groupInfo.getGroup(); } else { return
         * wildCardGroup(address, dataId, clientGroup); } } else { // 找不到, 模糊匹配
         * return wildCardGroup(address, dataId, clientGroup); }
         */
    }


    @SuppressWarnings("unused")
    private String wildCardGroup(String address, String dataId, String clientGroup) {
        for (String key : addressGroupCache.keySet()) {
            if (key.endsWith(WILDCARD_CHAR)) {
                String keyPrefix = key.substring(0, key.indexOf(WILDCARD_CHAR));
                if (address.startsWith(keyPrefix)) {
                    ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = addressGroupCache.get(key);
                    if (dataIdGroupMap != null) {
                        GroupInfo groupInfo = dataIdGroupMap.get(dataId);
                        if (groupInfo != null) {
                            return groupInfo.getGroup();
                        }
                    }
                }
            }
        }

        return defaultGroupOrClientGroup(clientGroup);
    }


    private String defaultGroupOrClientGroup(String clientGroup) {
        if (clientGroup != null)
            return clientGroup;
        else
            return Constants.DEFAULT_GROUP;
    }


    public ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> getAllAddressGroupMapping() {
        return this.addressGroupCache;
    }


    /**
     * 添加ip到分组的映射
     * 
     * @param address
     * @param dataId
     * @param group
     */
    @Deprecated
    public boolean addAddress2GroupMapping(String address, String dataId, String group) {
        synchronized (this) {
            if (this.addressGroupCache.containsKey(address)) {
                ConcurrentHashMap<String, GroupInfo> subMap = this.addressGroupCache.get(address);
                if (subMap != null && subMap.containsKey(dataId))
                    return false;
            }
            ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = this.addressGroupCache.get(address);
            if (dataIdGroupMap == null) {
                dataIdGroupMap = new ConcurrentHashMap<String, GroupInfo>();
                ConcurrentHashMap<String, GroupInfo> oldMap =
                        this.addressGroupCache.putIfAbsent(address, dataIdGroupMap);
                if (oldMap != null) {
                    dataIdGroupMap = oldMap;
                }
            }
            GroupInfo groupInfo = new GroupInfo(address, dataId, group);
            this.persistService.addGroupInfo(groupInfo);
            // 从数据库加载，这是为了获取id放入缓存
            groupInfo = this.persistService.findGroupInfoByAddressDataId(address, dataId);
            dataIdGroupMap.put(dataId, groupInfo);
        }
        // 通知其他节点
        this.notifyService.notifyGroupChanged();
        return true;
    }


    /**
     * 添加分组规则, 并将时间戳、源头IP和源头用户添加到数据库表中
     * 
     * @param address
     * @param dataId
     * @param group
     * @param srcIp
     * @param srcUser
     * @return
     */
    public boolean addAddress2GroupMapping(String address, String dataId, String group, String srcIp, String srcUser) {
        synchronized (this) {
            if (this.addressGroupCache.containsKey(address)) {
                ConcurrentHashMap<String, GroupInfo> subMap = this.addressGroupCache.get(address);
                if (subMap != null && subMap.containsKey(dataId))
                    return false;
            }
            ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = this.addressGroupCache.get(address);
            if (dataIdGroupMap == null) {
                dataIdGroupMap = new ConcurrentHashMap<String, GroupInfo>();
                ConcurrentHashMap<String, GroupInfo> oldMap =
                        this.addressGroupCache.putIfAbsent(address, dataIdGroupMap);
                if (oldMap != null) {
                    dataIdGroupMap = oldMap;
                }
            }
            GroupInfo groupInfo = new GroupInfo(address, dataId, group);
            // 获取当前时间
            Timestamp currentTime = DiamondUtils.getCurrentTime();
            this.persistService.addGroupInfo(srcIp, srcUser, currentTime, groupInfo);
            // 从数据库加载，这是为了获取id放入缓存
            groupInfo = this.persistService.findGroupInfoByAddressDataId(address, dataId);
            dataIdGroupMap.put(dataId, groupInfo);
        }
        // 通知其他节点
        this.notifyService.notifyGroupChanged();
        return true;
    }


    @Deprecated
    public boolean updateAddress2GroupMapping(long id, String newGroup) {
        synchronized (this) {
            GroupInfo groupInfo = this.persistService.findGroupInfoByID(id);
            if (groupInfo == null)
                return false;

            ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = this.addressGroupCache.get(groupInfo.getAddress());
            if (dataIdGroupMap == null)
                return false;
            if (dataIdGroupMap.get(groupInfo.getDataId()) == null)
                return false;
            this.persistService.updateGroup(id, newGroup);
            dataIdGroupMap.get(groupInfo.getDataId()).setGroup(newGroup);
        }
        this.notifyService.notifyGroupChanged();
        return true;
    }


    /**
     * 更新分组规则, 并将时间戳、源头IP和源头用户更新到数据库表中
     * 
     * @param id
     * @param newGroup
     * @param srcIp
     * @param srcUser
     * @return
     */
    public boolean updateAddress2GroupMapping(long id, String newGroup, String srcIp, String srcUser) {
        synchronized (this) {
            GroupInfo groupInfo = this.persistService.findGroupInfoByID(id);
            if (groupInfo == null)
                return false;

            ConcurrentHashMap<String, GroupInfo> dataIdGroupMap = this.addressGroupCache.get(groupInfo.getAddress());
            if (dataIdGroupMap == null)
                return false;
            if (dataIdGroupMap.get(groupInfo.getDataId()) == null)
                return false;

            Timestamp currentTime = DiamondUtils.getCurrentTime();
            this.persistService.updateGroup(id, srcIp, srcUser, currentTime, newGroup);
            dataIdGroupMap.get(groupInfo.getDataId()).setGroup(newGroup);
        }
        this.notifyService.notifyGroupChanged();
        return true;
    }


    public void removeAddress2GroupMapping(long id) {
        synchronized (this) {
            GroupInfo groupInfo = this.persistService.findGroupInfoByID(id);
            if (groupInfo == null)
                return;
            this.persistService.removeGroupInfoByID(id);
            ConcurrentHashMap<String, GroupInfo> dataIdMap = this.addressGroupCache.get(groupInfo.getAddress());
            if (dataIdMap != null) {
                dataIdMap.remove(groupInfo.getDataId());
            }
        }
        this.notifyService.notifyGroupChanged();
    }


    public void loadJSONFile() throws Exception {
        String fn = Constants.MAP_FILE;
        String content = this.diskService.readFile(fn);
        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> map =
                new ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> map2 =
                (Map<String, Map<String, String>>) JSONUtils.deserializeObject(content, HashMap.class);
        for (String key : map2.keySet()) {
            Map<String, String> map3 = map2.get(key);
            if (map != null) {
                ConcurrentHashMap<String, GroupInfo> cMap = new ConcurrentHashMap<String, GroupInfo>();
                for (Entry<String, String> e : map3.entrySet()) {
                    GroupInfo gInfo = new GroupInfo();
                    gInfo.setGroup(e.getValue());
                    gInfo.setDataId(e.getKey());
                    gInfo.setAddress(key);
                    cMap.put(e.getKey(), gInfo);
                }
                map.put(key, cMap);
            }
        }
        this.addressGroupCache = map;
    }
}
