package com.taobao.diamond.server.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.utils.JSONUtils;


public class GroupServiceUnitTest {

    private static class MyNotifyService extends NotifyService {
        private String changedDataId;
        private String changedGroup;
        private boolean groupChanged;


        @Override
        public void notifyConfigInfoChange(String dataId, String group) {
            this.changedDataId = dataId;
            this.changedGroup = group;
        }


        @Override
        public void notifyGroupChanged() {
            groupChanged = true;
        }


        public void reset() {
            this.changedDataId = null;
            this.changedGroup = null;
            this.groupChanged = false;
        }

    }

    private GroupService groupService;

    private PersistService persistService;

    private MyNotifyService notifyService;


    @Before
    public void setUp() {

        ApplicationContext ctx = new ClassPathXmlApplicationContext("application.xml");
        persistService = (PersistService) ctx.getBean("persistService");
        this.groupService = new GroupService();
        notifyService = new MyNotifyService();
        this.groupService.setPersistService(persistService);
        this.groupService.setNotifyService(notifyService);
        tearDown();
        this.groupService.loadGroupInfo();
    }


    @After
    public void tearDown() {
        ((DBPersistService) persistService).getJdbcTemplate().update("delete from group_info");
    }


    public void setUpTestData() {
        this.persistService.addGroupInfo(new GroupInfo("ip", "dataId1", "notify"));
        this.persistService.addGroupInfo(new GroupInfo("", "dataId1", "notify"));
        this.persistService.addGroupInfo(new GroupInfo("", "dataId2", "notify2"));
        this.persistService.addGroupInfo(new GroupInfo("", "dataId3", "notify2"));
        this.persistService.addGroupInfo(new GroupInfo("", "wildcard", "test"));
        this.groupService.loadGroupInfo();
    }


    @Test
    public void testGetGroupByAddress_默认分组() {
        setUpTestData();
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("localhost", null, null));
    }


    @Test
    public void testGetGroupByAddress_非默认分组() {
        setUpTestData();

        assertEquals("notify", this.groupService.getGroupByAddress("", "dataId1", null));
        assertEquals("notify", this.groupService.getGroupByAddress("", "dataId1", null));

        assertEquals("notify2", this.groupService.getGroupByAddress("", "dataId2", null));
        assertEquals("notify2", this.groupService.getGroupByAddress("", "dataId3", null));
    }


    @Test
    public void testGetGroupByAddress_模糊匹配分组() {
        setUpTestData();
        assertEquals("test", this.groupService.getGroupByAddress("", "wildcard", "mygroup1"));
        assertEquals("test", this.groupService.getGroupByAddress("", "wildcard", "mygroup2"));
        // 没有对应的dataId
        assertEquals("mygroup3", this.groupService.getGroupByAddress("", "mydataId", "mygroup3"));
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", "mydataId", null));
    }


    @Test
    public void testGetGroupByAddress_使用客户端分组() {
        assertEquals("hello", this.groupService.getGroupByAddress("", null, "hello"));
        assertEquals("world", this.groupService.getGroupByAddress("", null, "world"));
    }


    @Test
    public void testGetGroupByAddress_未设置分组() {
        setUpTestData();
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", null, null));
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", null, null));
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", null, null));
    }


    @Test
    public void testAddAddress2GroupMapping() {
        assertTrue(this.groupService.addAddress2GroupMapping("", "dataId", "test-group1"));
        assertTrue(this.notifyService.groupChanged);
        assertEquals("test-group1", this.groupService.getGroupByAddress("", "dataId", null));
        assertFalse(this.groupService.addAddress2GroupMapping("", "dataId", "test-group1"));
        assertEquals("test-group1", this.groupService.getGroupByAddress("", "dataId", null));// 保持不变
    }


    @Test
    public void testdeleteAddress() {
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", "dataId", null));
        assertTrue(this.groupService.addAddress2GroupMapping("", "dataId", "test-group1"));
        assertTrue(this.notifyService.groupChanged);
        assertEquals("test-group1", this.groupService.getGroupByAddress("", "dataId", null));
        this.notifyService.reset();
        GroupInfo groupInfo = this.persistService.findGroupInfoByAddressDataId("", "dataId");
        this.groupService.removeAddress2GroupMapping(groupInfo.getId());
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", "dataId", null));
        // 重新加载，数据库确认删除
        this.groupService.loadGroupInfo();
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress("", "dataId", null));
        assertTrue(this.notifyService.groupChanged);

    }


    @Test
    public void testDumpJson() throws Exception {
        DiskService diskService = new DiskService();
        ServletContext servletContext = EasyMock.createMock(ServletContext.class);
        diskService.setServletContext(servletContext);
        groupService.setDiskService(diskService);
        File tempFile = File.createTempFile("DumpJsonTest", ".tmp");
        try {
            String path = tempFile.getParent();
            EasyMock.expect(servletContext.getRealPath("/")).andReturn(path).once();
            EasyMock.replay(servletContext);

            groupService.addAddress2GroupMapping("", "notify", "boyan1");
            groupService.addAddress2GroupMapping("", "tc", "boyan2");

            groupService.dumpJSONFile();

            File file = new File(path + "/" + Constants.MAP_FILE);
            assertTrue(file.exists());
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();
            Map<String, Map<String, String>> map =
                    (Map<String, Map<String, String>>) JSONUtils.deserializeObject(line, HashMap.class);
            assertTrue(map.containsKey(""));
            assertEquals(1, map.size());
            assertTrue(map.get("").containsKey("notify"));
            assertTrue(map.get("").containsKey("tc"));
            assertEquals(2, map.get("").size());
            assertEquals("boyan1", map.get("").get("notify"));
            assertEquals("boyan2", map.get("").get("tc"));
            reader.close();

            file.delete();
            EasyMock.verify(servletContext);
        }
        finally {
            tempFile.delete();
        }
    }


    @Test
    public void testUpdateAddress2GroupMapping() {
        assertTrue(this.groupService.addAddress2GroupMapping("", "dataId", "test-group1"));
        assertEquals("test-group1", this.groupService.getGroupByAddress("", "dataId", null));
        GroupInfo groupInfo = this.persistService.findGroupInfoByAddressDataId("", "dataId");
        assertFalse(this.groupService.updateAddress2GroupMapping(100000, "new-group"));
        assertTrue(this.groupService.updateAddress2GroupMapping(groupInfo.getId(), "new-group"));
        assertEquals("new-group", this.groupService.getGroupByAddress("", "dataId", null));

    }


    @Test
    public void testAddGroupInfo_timestamp() {
        String address = "";
        String dataId = "groupServiceTimestamp";
        String group = "addGroup";
        String srcIp = "";
        String srcUser = "xxx";

        assertTrue(this.groupService.addAddress2GroupMapping(address, dataId, group, srcIp, srcUser));
        assertTrue(this.notifyService.groupChanged);
        assertEquals(group, this.groupService.getGroupByAddress(address, dataId, null));
        assertFalse(this.groupService.addAddress2GroupMapping(address, dataId, group, srcIp, srcUser));
        assertEquals(group, this.groupService.getGroupByAddress(address, dataId, null));// 保持不变
    }


    @Test
    public void testUpdateGroupInfo_timestamp() {
        String address = "";
        String dataId = "groupServiceTimestamp";
        String group = "updateGroup";
        String srcIp = "";
        String srcUser = "xxx";

        assertTrue(this.groupService.addAddress2GroupMapping(address, dataId, group, srcIp, srcUser));
        assertTrue(this.notifyService.groupChanged);
        assertEquals(group, this.groupService.getGroupByAddress(address, dataId, null));

        GroupInfo gInfo = this.persistService.findGroupInfoByAddressDataId(address, dataId);
        long id = gInfo.getId();
        String newGroup = "newGroup";
        srcUser = "xxx";

        assertTrue(this.groupService.updateAddress2GroupMapping(id, newGroup, srcIp, srcUser));
        assertTrue(this.notifyService.groupChanged);
        assertEquals(newGroup, this.groupService.getGroupByAddress(address, dataId, null));
    }

}
