package com.taobao.diamond.server.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;


public class PersistServiceUnitTest {
    private PersistService persistService;


    @Before
    public void setUp() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("application.xml");
        persistService = (PersistService) ctx.getBean("persistService");
        truncateTable();
    }


    @After
    public void tearDown() {
        // truncateTable();
    }


    private void truncateTable() {
        ((DBPersistService) persistService).getJdbcTemplate().update("delete from config_info");
        ((DBPersistService) persistService).getJdbcTemplate().update("delete from group_info");
    }


    @Test
    public void testAddConfigInfo_GetConfigInfo() {
        ConfigInfo configInfo = new ConfigInfo("test-dataId1", "test-group", "test content");
        this.persistService.addConfigInfo(configInfo);

        ConfigInfo configInfoFromDB = this.persistService.findConfigInfo(configInfo.getDataId(), configInfo.getGroup());
        assertNotSame(configInfo, configInfoFromDB);
        assertEquals(configInfo, configInfoFromDB);
    }


    @Test(expected = DataAccessException.class)
    public void testAddConfigInfo_NullDataId() {
        ConfigInfo configInfo = new ConfigInfo(null, "test-group", "test content");
        this.persistService.addConfigInfo(configInfo);

    }


    @Test(expected = DataAccessException.class)
    public void testAddConfigInfo_NullContent() {
        ConfigInfo configInfo = new ConfigInfo("test-dataId1", "test-group", null);
        this.persistService.addConfigInfo(configInfo);

    }


    @Test(expected = DataAccessException.class)
    public void testAddConfigInfo_NullGroup() {
        ConfigInfo configInfo = new ConfigInfo("test-dataId1", null, "test-content");
        this.persistService.addConfigInfo(configInfo);

    }


    @Test
    public void testGetConfigInfo() {
        ConfigInfo configInfoFromDB = this.persistService.findConfigInfo("test-dataId2", "test-group2");
        assertNull(configInfoFromDB);
        ConfigInfo configInfo = new ConfigInfo("test-dataId2", "test-group2", "test content");
        this.persistService.addConfigInfo(configInfo);
        configInfoFromDB = this.persistService.findConfigInfo("test-dataId2", "test-group2");
        assertNotNull(configInfoFromDB);
        assertNotSame(configInfo, configInfoFromDB);
        assertEquals(configInfo, configInfoFromDB);
    }


    @Test
    public void testRemoveConfigInfo() {
        ConfigInfo configInfo = new ConfigInfo("test-dataId3", "test-group3", "test content");
        this.persistService.addConfigInfo(configInfo);
        ConfigInfo configInfoFromDB = this.persistService.findConfigInfo("test-dataId3", "test-group3");
        assertNotNull(configInfoFromDB);
        assertNotSame(configInfo, configInfoFromDB);

        this.persistService.removeConfigInfo(configInfo);
        configInfoFromDB = this.persistService.findConfigInfo("test-dataId3", "test-group3");
        assertNull(configInfoFromDB);// 已经被删除

        // 测试根据id删除
        configInfo = new ConfigInfo("test-dataId3", "test-group3", "test content");
        this.persistService.addConfigInfo(configInfo);
        configInfoFromDB = this.persistService.findConfigInfo("test-dataId3", "test-group3");
        assertNotNull(configInfoFromDB);
        this.persistService.removeConfigInfoByID(configInfoFromDB.getId());
        configInfoFromDB = this.persistService.findConfigInfo("test-dataId3", "test-group3");
        assertNull(configInfoFromDB);// 已经被删除
    }


    @Test
    public void testRemoveConfigInfo_不存在的记录() {
        truncateTable();
        this.persistService.removeConfigInfo("test-dataId4", "test-group4");
    }


    // @Test
    // public void testAddConfigInfo_插入多个_findConfigInfoByGroup() {
    // truncateTable();
    // int count = 300;
    // final String group = "test-group";
    // for (int i = 0; i < count; i++) {
    // ConfigInfo configInfo = new ConfigInfo("test-dataId" + i, group,
    // "test content" + i);
    // this.persistService.addConfigInfo(configInfo);
    // }
    // int sum =
    // this.persistService.getJdbcTemplate().queryForInt(
    // "select count(ID) from config_info where group_id='test-group'");
    // assertEquals(count, sum);
    // Page<ConfigInfo> page = this.persistService.findConfigInfoByGroup(1,
    // count, group);
    // assertNotNull(page);
    // assertEquals(count, page.getPageItems().size());
    //
    // for (ConfigInfo info : page.getPageItems()) {
    // assertEquals(group, info.getGroup());
    // }
    // }

    // @Test
    // public void testAddConfigInfo_插入多个_findConfigInfoByDataId() {
    // truncateTable();
    // int count = 300;
    // final String dataId = "test-dataId";
    // for (int i = 0; i < count; i++) {
    // ConfigInfo configInfo = new ConfigInfo(dataId, "test-group" + i,
    // "test content" + i);
    // this.persistService.addConfigInfo(configInfo);
    // }
    // int sum =
    // this.persistService.getJdbcTemplate().queryForInt(
    // "select count(ID) from config_info where data_id='test-dataId'");
    // assertEquals(count, sum);
    // Page<ConfigInfo> page = this.persistService.findConfigInfoByDataId(1,
    // count, dataId);
    // assertNotNull(page);
    // assertEquals(count, page.getPageItems().size());
    //
    // for (ConfigInfo info : page.getPageItems()) {
    // assertEquals(dataId, info.getDataId());
    // }
    // }

    @Test
    public void testPagination() {
        truncateTable();
        int count = 300;
        final String dataId = "test-dataId";
        for (int i = 0; i < count; i++) {
            ConfigInfo configInfo = new ConfigInfo(dataId, "test-group" + i, "test content" + i);
            this.persistService.addConfigInfo(configInfo);
        }
        Page<ConfigInfo> page1 = this.persistService.findConfigInfoByDataId(1, 100, dataId);
        Page<ConfigInfo> page2 = this.persistService.findConfigInfoByDataId(2, 100, dataId);
        Page<ConfigInfo> page3 = this.persistService.findConfigInfoByDataId(3, 100, dataId);

        assertNotNull(page1);
        assertNotNull(page2);
        assertNotNull(page3);
        assertEquals(3, page1.getPagesAvailable());
        assertEquals(3, page2.getPagesAvailable());
        assertEquals(3, page3.getPagesAvailable());

        assertEquals(1, page1.getPageNumber());
        assertEquals(2, page2.getPageNumber());
        assertEquals(3, page3.getPageNumber());

        assertEquals(100, page1.getPageItems().size());
        assertEquals(100, page2.getPageItems().size());
        assertEquals(100, page3.getPageItems().size());

        assertFalse(page1.getPageItems().equals(page2.getPageItems()));
        assertFalse(page2.getPageItems().equals(page3.getPageItems()));
        assertFalse(page1.getPageItems().equals(page3.getPageItems()));
    }


    @Test
    public void testAddGroupInfo() {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setAddress("192.168.207.102");
        groupInfo.setGroup("notify");
        groupInfo.setDataId("dataId");
        this.persistService.addGroupInfo(groupInfo);

        GroupInfo groupInfoFromDB = this.persistService.findGroupInfoByAddressDataId("192.168.207.102", "dataId");
        assertNotSame(groupInfo, groupInfoFromDB);
        assertEquals(groupInfo, groupInfoFromDB);
    }


    @Test(expected = DataAccessException.class)
    public void testAddGroupInfo_Duplicate() {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setAddress("192.168.207.102");
        groupInfo.setGroup("notify");
        groupInfo.setDataId("leiwen");
        this.persistService.addGroupInfo(groupInfo);

        groupInfo = new GroupInfo();
        groupInfo.setAddress("192.168.207.102");
        groupInfo.setGroup("notify");
        groupInfo.setDataId("leiwen");
        this.persistService.addGroupInfo(groupInfo);
    }


    @Test
    public void testFindGroupInfoByGroup() {
        GroupInfo groupInfo1 = new GroupInfo();
        groupInfo1.setAddress("192.168.207.102");
        groupInfo1.setGroup("notify");
        groupInfo1.setDataId("dataId1");
        this.persistService.addGroupInfo(groupInfo1);

        GroupInfo groupInfo2 = new GroupInfo();
        groupInfo2.setAddress("192.168.207.101");
        groupInfo2.setGroup("notify");
        groupInfo2.setDataId("dataId2");
        this.persistService.addGroupInfo(groupInfo2);

        List<GroupInfo> list = this.persistService.findGroupInfoByGroup("notify");
        assertNotNull(list);
        assertEquals(2, list.size());
        assertTrue(list.contains(groupInfo1));
        assertTrue(list.contains(groupInfo2));

    }


    // @Test
    // public void testFindAllGroupInfo() {
    //
    //
    //
    // ApplicationContext ctx = new
    // ClassPathXmlApplicationContext("persistService.xml");
    // PersistService persistService = (PersistService)
    // ctx.getBean("persistService");
    // persistService.getJdbcTemplate().update("delete from config_info");
    // persistService.getJdbcTemplate().update("delete from group_info");
    // // PersistService.findAllConfigInfoHangMode = false;
    // int count = 100;
    // for (int i = 0; i < count; i++) {
    // GroupInfo groupInfo1 = new GroupInfo();
    // groupInfo1.setAddress("192.168.207.10" + i);
    // groupInfo1.setGroup("notify" + i);
    // groupInfo1.setDataId("dataId");
    // persistService.addGroupInfo(groupInfo1);
    // }
    //
    // long startTs = System.currentTimeMillis();
    // try{
    // List<GroupInfo> list = persistService.findAllGroupInfo();
    // }catch(RuntimeException e){
    // if(e.getMessage().indexOf("Query execution was interrupted")!=-1){
    //
    // }
    // System.out.println(e.getMessage());
    // //e.printStackTrace();
    // }
    // long endTs = System.currentTimeMillis();
    // long ts = endTs-startTs;
    // System.out.println("ts:"+ts);
    //
    //
    // }

    @Test
    public void testFindConfigInfoByID() {
        ConfigInfo configInfo = new ConfigInfo("dataId", "notify", "hello world");
        this.persistService.addConfigInfo(configInfo);
        ConfigInfo configInfoFromDB = this.persistService.findConfigInfo("dataId", "notify");
        ConfigInfo configInfoFromDB2 = this.persistService.findConfigInfoByID(configInfoFromDB.getId());
        assertNotSame(configInfo, configInfoFromDB);
        assertNotSame(configInfo, configInfoFromDB2);
        assertNotSame(configInfoFromDB2, configInfoFromDB);
        assertEquals(configInfo, configInfoFromDB);
        assertEquals(configInfoFromDB, configInfoFromDB2);
        this.persistService.removeConfigInfo(configInfo);

    }


    @Test
    public void testUpdateConfigInfo() {
        ConfigInfo configInfo = new ConfigInfo("dataId", "notify", "hello world");
        this.persistService.addConfigInfo(configInfo);
        ConfigInfo configInfoFromDB = this.persistService.findConfigInfo("dataId", "notify");

        configInfo.setContent("diamond");
        this.persistService.updateConfigInfo(configInfo);
        configInfoFromDB = this.persistService.findConfigInfoByID(configInfoFromDB.getId());
        assertEquals("diamond", configInfoFromDB.getContent());
    }


    @Test
    public void testUpdateGroupInfo_Remove() {
        GroupInfo groupInfo = new GroupInfo("1.1.1.1", "dataId", "test-group");
        this.persistService.addGroupInfo(groupInfo);

        GroupInfo groupInfoFromDB = this.persistService.findGroupInfoByAddressDataId("1.1.1.1", "dataId");
        assertNotSame(groupInfo, groupInfoFromDB);
        assertEquals(groupInfo, groupInfoFromDB);
        this.persistService.updateGroup(groupInfoFromDB.getId(), "new-group");
        groupInfoFromDB = this.persistService.findGroupInfoByAddressDataId("1.1.1.1", "dataId");
        assertEquals("new-group", groupInfoFromDB.getGroup());
        this.persistService.removeGroupInfoByID(groupInfoFromDB.getId());
        assertNull(this.persistService.findGroupInfoByAddressDataId("1.1.1.1", "dataId"));
    }


    @Test
    public void testFindAllConfigInfo() {
        truncateTable();
        int count = 100;
        final String dataId = "test-dataId";
        for (int i = 0; i < count; i++) {
            ConfigInfo configInfo = new ConfigInfo(dataId, "test-group" + i, "test content" + i);
            this.persistService.addConfigInfo(configInfo);
        }

        Page<ConfigInfo> page = this.persistService.findAllConfigInfo(1, 100);
        assertNotNull(page);
        assertEquals(count, page.getPageItems().size());
        assertEquals(1, page.getPagesAvailable());
        assertNull(this.persistService.findAllConfigInfo(2, 100));

    }


    @Test
    public void testFindGroupInfoByID() {
        this.persistService.addGroupInfo(new GroupInfo("localhost", "dataId", "test-group"));
        GroupInfo groupInfo = this.persistService.findGroupInfoByAddressDataId("localhost", "dataId");
        assertNotNull(groupInfo);
        GroupInfo groupInfoFromDB = this.persistService.findGroupInfoByID(groupInfo.getId());

        assertEquals(groupInfo, groupInfoFromDB);
    }


    @Test
    public void testFindConfigInfoLike_DataId() {
        truncateTable();
        int count = 100;
        final String dataId = "test-dataId";
        final String group = "test-group";
        for (int i = 0; i < count; i++) {
            ConfigInfo configInfo = new ConfigInfo(dataId, group + i, "test content" + i);
            this.persistService.addConfigInfo(configInfo);
        }
        Page<ConfigInfo> page = this.persistService.findConfigInfoLike(1, 100, dataId, null);
        assertNotNull(page);
        assertEquals(count, page.getPageItems().size());
        assertEquals(1, page.getPagesAvailable());
        assertNull(persistService.findConfigInfoLike(2, 100, dataId, null));
    }


    @Test
    public void testFindConfigInfoLike_Group() {
        truncateTable();
        int count = 100;
        final String dataId = "test-dataId";
        final String group = "test-group";
        for (int i = 0; i < count; i++) {
            ConfigInfo configInfo = new ConfigInfo(dataId, group + i, "test content" + i);
            this.persistService.addConfigInfo(configInfo);
        }
        Page<ConfigInfo> page = this.persistService.findConfigInfoLike(1, 100, null, group);
        assertNotNull(page);
        assertEquals(count, page.getPageItems().size());
        assertEquals(1, page.getPagesAvailable());
        assertNull(persistService.findConfigInfoLike(2, 100, null, group));
    }


    @Test
    public void testFindConfigInfoLike_Group_dataId() {
        truncateTable();
        int count = 100;
        final String dataId = "test-dataId";
        final String group = "test-group";
        for (int i = 0; i < count; i++) {
            ConfigInfo configInfo = new ConfigInfo(dataId, group + i, "test content" + i);
            this.persistService.addConfigInfo(configInfo);
        }
        Page<ConfigInfo> page = this.persistService.findConfigInfoLike(1, 100, dataId, group);
        assertNotNull(page);
        assertEquals(count, page.getPageItems().size());
        assertEquals(1, page.getPagesAvailable());
        assertNull(persistService.findConfigInfoLike(2, 100, dataId, group));
    }


    @Test
    public void testAddConfigInfo_timestamp() {
        String dataId = "testTimestamp";
        String group = "addConfig";
        String content = "test";
        String srcIp = "192.168.0.1";
        String srcUser = "leiwen.zh";
        Date d = new Date();
        Timestamp time = new Timestamp(d.getTime());

        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);

        this.persistService.addConfigInfo(srcIp, srcUser, time, configInfo);

        assertEquals(configInfo, this.persistService.findConfigInfo(dataId, group));
    }


    @Test
    public void testAddGroupInfo_timestamp() {
        String address = "127.0.0.1";
        String dataId = "testTimestamp";
        String group = "addGroup";
        String srcIp = "192.168.0.1";
        String srcUser = "leiwen.zh";
        Date d = new Date();
        Timestamp time = new Timestamp(d.getTime());

        GroupInfo groupInfo = new GroupInfo(address, dataId, group);

        this.persistService.addGroupInfo(srcIp, srcUser, time, groupInfo);

        assertEquals(groupInfo, this.persistService.findGroupInfoByAddressDataId(address, dataId));
    }


    @Test
    public void testUpdateGroupInfo_timestamp() {
        String address = "127.0.0.1";
        String dataId = "testTimestamp";
        String group = "updateGroup";
        String srcIp = "192.168.0.1";
        String srcUser = "leiwen.zh";
        Date d = new Date();
        Timestamp time = new Timestamp(d.getTime());

        GroupInfo groupInfo = new GroupInfo(address, dataId, group);

        this.persistService.addGroupInfo(srcIp, srcUser, time, groupInfo);

        assertEquals(groupInfo, this.persistService.findGroupInfoByAddressDataId(address, dataId));

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        GroupInfo gInfo = this.persistService.findGroupInfoByAddressDataId(address, dataId);
        long id = gInfo.getId();
        srcUser = "yinshi.nc";
        group = "newGroup";
        d = new Date();
        time = new Timestamp(d.getTime());

        this.persistService.updateGroup(id, srcIp, srcUser, time, group);

        GroupInfo newGroupInfo = new GroupInfo(address, dataId, group);
        assertEquals(newGroupInfo, this.persistService.findGroupInfoByAddressDataId(address, dataId));
    }


    @Test
    public void testUpdateConfigInfo_timestamp() {
        String dataId = "testTimestamp";
        String group = "updateConfig";
        String content = "test";
        String srcIp = "192.168.0.1";
        String srcUser = "leiwen.zh";
        Date d = new Date();
        Timestamp time = new Timestamp(d.getTime());

        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);

        this.persistService.addConfigInfo(srcIp, srcUser, time, configInfo);

        assertEquals(configInfo, this.persistService.findConfigInfo(dataId, group));

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        content = "test test";
        configInfo = new ConfigInfo(dataId, group, content);
        srcUser = "yinshi.nc";
        d = new Date();
        time = new Timestamp(d.getTime());

        this.persistService.updateConfigInfo(srcIp, srcUser, time, configInfo);

        assertEquals(configInfo, this.persistService.findConfigInfo(dataId, group));
    }


    @Test
    public void testUpdateConfigInfoByMd5_timestamp() {
        String dataId = "testTimestamp";
        String group = "updateConfigByMd5";
        String content = "test";
        String srcIp = "192.168.0.1";
        String srcUser = "leiwen.zh";
        Date d = new Date();
        Timestamp time = new Timestamp(d.getTime());

        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        String oldMd5 = configInfo.getMd5();

        this.persistService.addConfigInfo(srcIp, srcUser, time, configInfo);

        assertEquals(configInfo, this.persistService.findConfigInfo(dataId, group));

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        content = "test test";
        configInfo = new ConfigInfo(dataId, group, content);
        srcUser = "yinshi.nc";
        d = new Date();
        time = new Timestamp(d.getTime());

        ConfigInfoEx configInfoEx = new ConfigInfoEx(dataId, group, content);
        configInfoEx.setOldMd5(oldMd5);
        this.persistService.updateConfigInfoByMd5(srcIp, srcUser, time, configInfoEx);

        ConfigInfo configInfoFromDB = this.persistService.findConfigInfo(dataId, group);

        assertEquals(dataId, configInfoFromDB.getDataId());
        assertEquals(group, configInfoFromDB.getGroup());
        assertEquals(content, configInfoFromDB.getContent());
    }
}
