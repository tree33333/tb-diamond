package com.taobao.diamond.server.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.md5.MD5;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.server.service.task.processor.NotifyTaskProcessor;


public class ConfigServiceUnitTest {
    private ConfigService configService;
    private PersistService persistService;
    private DiskService diskService;

    private ServletContext servletContext;

    private IMocksControl mocksControl;

    private File tempFile;

    private String path;
    private NotifyService notifyService;
    private GroupService groupService;


    @Before
    public void setUp() throws Exception {
        configService = new ConfigService();
        ApplicationContext ctx = new ClassPathXmlApplicationContext("application.xml");
        persistService = (PersistService) ctx.getBean("persistService");
        this.diskService = new DiskService();
        this.configService.setPersistService(persistService);
        this.configService.setDiskService(diskService);
        notifyService = new NotifyService();
        this.configService.setNotifyService(notifyService);
        this.configService.setValidationService(new ValidationService());
        groupService = new GroupService();
        this.configService.setGroupService(groupService);
        mocksControl = EasyMock.createControl();
        servletContext = mocksControl.createMock(ServletContext.class);
        this.diskService.setServletContext(servletContext);
        tempFile = File.createTempFile("ConfigServiceUnitTest", "tmp");
        path = tempFile.getParent();

        TaskManagerService taskManagerService = new TaskManagerService();
        TaskManager[] updateTaskManagers = new TaskManager[4];
        for (int i = 0; i < 4; i++) {
            updateTaskManagers[i] = new TaskManager("update task manager " + i);
        }
        TaskManager updateAllTaskManager = new TaskManager("test update-all task manager");
        TaskManager removeTaskManager = new TaskManager("test remove task manager");
        TaskManager notifyTaskManager = new TaskManager("test notify task manager");
        TaskManager pushitTaskManager = new TaskManager("test pushit task manager");
        TaskManager redisTaskManager = new TaskManager("test redis task manager");
        taskManagerService.setUpdateTaskManagers(updateTaskManagers);
        taskManagerService.setUpdateAllTaskManager(updateAllTaskManager);
        taskManagerService.setRemoveTaskManager(removeTaskManager);
        taskManagerService.setNotifyTaskManager(notifyTaskManager);
        taskManagerService.setPushitTaskManager(pushitTaskManager);
        taskManagerService.setRedisTaskManager(redisTaskManager);

        NotifyTaskProcessor notifyProcessor = new NotifyTaskProcessor();
        notifyProcessor.setNotifyService(notifyService);

        configService.setTaskManagerService(taskManagerService);
        configService.setNotifyTaskProcessor(notifyProcessor);

        truncateTable();
    }


    private void truncateTable() {
        // persistService.getJdbcTemplate().update("delete from config_info");
        // persistService.getJdbcTemplate().update("delete from group_info");
    }


    @Test
    public void testGetConfigInfoPath() {
        String path = this.configService.getConfigInfoPath("dataId", "localhost", null);
        assertEquals("/" + Constants.BASE_DIR + "/" + Constants.DEFAULT_GROUP + "/dataId", path);

        path = this.configService.getConfigInfoPath("dataId", "localhost", "group");
        assertEquals("/" + Constants.BASE_DIR + "/group/dataId", path);

        groupService.getAllAddressGroupMapping().put("localhost", new ConcurrentHashMap<String, GroupInfo>());
        groupService.getAllAddressGroupMapping().get("localhost")
            .put("dataId", new GroupInfo("localhost", "dataId", "group2"));
        path = this.configService.getConfigInfoPath("dataId", "localhost", "group1");
        assertEquals("/config-data/group2/dataId", path);
    }


    @Test
    public void testGetContentMD5_UpdateContentMD5() {
        assertNull(this.configService.getContentMD5("dataId", "group"));

        this.persistService.addConfigInfo(new ConfigInfo("dataId", "group", "test content"));
        String md5 = this.configService.getContentMD5("dataId", "group");
        // assertNotNull(md5);
        // assertEquals(MD5.getInstance().getMD5String("test content"), md5);
        mockServletContext("dataId", "group", "hello world");
        this.configService.updateConfigInfo("dataId", "group", "hello world");
        md5 = this.configService.getContentMD5("dataId", "group");
        assertNotNull(md5);
        assertEquals(MD5.getInstance().getMD5String("hello world"), md5);
        this.mocksControl.verify();

        // 不通过数据库更新
        this.configService.updateMD5Cache(new ConfigInfo("dataId", "group", "boyan@taobao.com"));
        md5 = this.configService.getContentMD5("dataId", "group");
        assertEquals(MD5.getInstance().getMD5String("boyan@taobao.com"), md5);
    }


    @Test
    public void testGenerateMD5CacheKey() {
        assertEquals("group/dataid", this.configService.generateMD5CacheKey("dataid", "group"));
        assertEquals("test-group/test", this.configService.generateMD5CacheKey("test", "test-group"));
    }


    @Test
    public void testAdd_Update_RemoveConfigInfo() throws Exception {
        File file = null;
        try {
            assertNull(this.configService.findConfigInfo("dataId1", "group1"));
            assertNull(this.configService.getContentMD5("dataId1", "group1"));
            file = new File(path + "/" + "config-data/group1/dataId1");
            assertFalse(file.exists());

            // 插入数据，然后更新，查看数据库和文件是否都被更新
            ConfigInfo configInfo = new ConfigInfo("dataId1", "group1", "just a test");
            mockServletContext("dataId1", "group1", "just a test");
            this.configService.addConfigInfo(configInfo.getDataId(), configInfo.getGroup(), configInfo.getContent());
            ConfigInfo configInfoFromDB = this.configService.findConfigInfo("dataId1", "group1");
            assertNotNull(configInfoFromDB);
            assertEquals(configInfo, configInfoFromDB);
            assertEquals(MD5.getInstance().getMD5String("just a test"),
                configService.getContentMD5("dataId1", "group1"));
            file = new File(path + "/" + "config-data/group1/dataId1");
            assertTrue(file.exists());
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();
            assertEquals("just a test", line);
            reader.close();

            // 更新
            configInfo.setContent("new content");
            configInfo.setMd5(MD5.getInstance().getMD5String("new content"));
            this.configService.updateConfigInfo(configInfo.getDataId(), configInfo.getGroup(), configInfo.getContent());
            configInfoFromDB = this.configService.findConfigInfo("dataId1", "group1");
            assertNotNull(configInfoFromDB);
            assertEquals(configInfo, configInfoFromDB);
            assertEquals(MD5.getInstance().getMD5String("new content"),
                configService.getContentMD5("dataId1", "group1"));
            file = new File(path + "/" + "config-data/group1/dataId1");
            assertTrue(file.exists());
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            line = reader.readLine();
            assertEquals("new content", line);
            reader.close();

            // 删除
            this.configService.removeConfigInfo("dataId1", "group1");
            assertNull(this.configService.findConfigInfo("dataId1", "group1"));
            assertNull(this.configService.getContentMD5("dataId1", "group1"));
            file = new File(path + "/" + "config-data/group1/dataId1");
            assertFalse(file.exists());

            mocksControl.verify();

        }
        finally {
            file.delete();
        }

    }


    public void mockServletContext(String dataId, String group, String content) {
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR))
            .andReturn(path + "/" + Constants.BASE_DIR).anyTimes();
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group))
            .andReturn(path + "/" + Constants.BASE_DIR + "/" + group).anyTimes();
        String dataPath = path + "/" + Constants.BASE_DIR + "/" + group + "/" + dataId;
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group + "/" + dataId))
            .andReturn(dataPath).anyTimes();
        mocksControl.replay();
    }


    @Test
    public void testLoadConfigInfoToDisk() throws Exception {
        File file = new File(path + "/" + "config-data/group1/dataId1");
        assertFalse(file.exists());

        this.persistService.addConfigInfo(new ConfigInfo("dataId1", "group1", "hello world"));
        file = new File(path + "/" + "config-data/group1/dataId1");
        assertFalse(file.exists());
        mockServletContext("dataId1", "group1", "hello world");
        this.configService.loadConfigInfoToDisk("dataId1", "group1");
        file = new File(path + "/" + "config-data/group1/dataId1");
        assertTrue(file.exists());
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line = reader.readLine();
        assertEquals("hello world", line);
        reader.close();

        // 删除数据库再加载，应该删除文件
        this.persistService.removeConfigInfo("dataId1", "group1");
        this.configService.loadConfigInfoToDisk("dataId1", "group1");
        file = new File(path + "/" + "config-data/group1/dataId1");
        assertFalse(file.exists());

    }


    @Test
    public void testCountAllDataIds() throws Exception {
        System.out.println(this.configService.countAllDataIds());
    }


    @Test
    public void testAddConfigInfo_UpdateConfigInfo_timestamp() throws Exception {
        File file = null;
        try {
            String dataId = "configServiceTimestamp";
            String group = "addAndUpdateConfig";
            String content = "test";
            String newContent = "test new";
            String srcIp = "127.0.0.1";
            String srcUser = "leiwen.zh";

            assertNull(this.configService.findConfigInfo(dataId, group));
            assertNull(this.configService.getContentMD5(dataId, group));
            file = new File(path + "/" + "config-data/" + group + "/" + dataId);
            assertFalse(file.exists());

            // 插入数据，然后更新，查看数据库和文件是否都被更新
            ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
            mockServletContext(dataId, group, content);
            // 加入时间戳、源IP和源用户
            this.configService.addConfigInfo(dataId, group, content, srcIp, srcUser);

            ConfigInfo configInfoFromDB = this.configService.findConfigInfo(dataId, group);
            assertNotNull(configInfoFromDB);
            assertEquals(configInfo, configInfoFromDB);
            assertEquals(MD5.getInstance().getMD5String(content), configService.getContentMD5(dataId, group));

            file = new File(path + "/" + "config-data/" + group + "/" + dataId);
            assertTrue(file.exists());

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();
            assertEquals(content, line);
            reader.close();

            Thread.sleep(1000);

            // 更新
            configInfo.setContent(newContent);
            configInfo.setMd5(MD5.getInstance().getMD5String(newContent));
            this.configService.updateConfigInfo(dataId, group, newContent, srcIp, srcUser);

            configInfoFromDB = this.configService.findConfigInfo(dataId, group);
            assertNotNull(configInfoFromDB);
            assertEquals(configInfo, configInfoFromDB);
            assertEquals(MD5.getInstance().getMD5String(newContent), configService.getContentMD5(dataId, group));

            file = new File(path + "/" + "config-data/" + group + "/" + dataId);
            assertTrue(file.exists());

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            line = reader.readLine();
            assertEquals(newContent, line);
            reader.close();

            mocksControl.verify();
        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testUpdateConfigInfoByMd5_timestamp() throws Exception {
        File file = null;
        try {
            String dataId = "configServiceTimestamp";
            String group = "addAndUpdateConfigByMd5";
            String content = "test";
            String newContent = "test new";
            String srcIp = "127.0.0.1";
            String srcUser = "leiwen.zh";

            assertNull(this.configService.findConfigInfo(dataId, group));
            assertNull(this.configService.getContentMD5(dataId, group));
            file = new File(path + "/" + "config-data/" + group + "/" + dataId);
            assertFalse(file.exists());

            // 插入数据，然后更新，查看数据库和文件是否都被更新
            ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
            mockServletContext(dataId, group, content);
            // 加入时间戳、源IP和源用户
            this.configService.addConfigInfo(dataId, group, content, srcIp, srcUser);

            ConfigInfo configInfoFromDB = this.configService.findConfigInfo(dataId, group);
            assertNotNull(configInfoFromDB);
            assertEquals(configInfo, configInfoFromDB);
            assertEquals(MD5.getInstance().getMD5String(content), configService.getContentMD5(dataId, group));

            file = new File(path + "/" + "config-data/" + group + "/" + dataId);
            assertTrue(file.exists());

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();
            assertEquals(content, line);
            reader.close();

            Thread.sleep(1000);

            // 更新
            String oldMd5 = configInfo.getMd5();
            configInfo.setContent(newContent);
            configInfo.setMd5(MD5.getInstance().getMD5String(newContent));
            this.configService.updateConfigInfoByMd5(dataId, group, newContent, oldMd5, srcIp, srcUser);

            configInfoFromDB = this.configService.findConfigInfo(dataId, group);
            assertNotNull(configInfoFromDB);
            assertEquals(configInfo, configInfoFromDB);
            assertEquals(MD5.getInstance().getMD5String(newContent), configService.getContentMD5(dataId, group));

            file = new File(path + "/" + "config-data/" + group + "/" + dataId);
            assertTrue(file.exists());

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            line = reader.readLine();
            assertEquals(newContent, line);
            reader.close();

            mocksControl.verify();
        }
        finally {
            file.delete();
        }
    }


    @After
    public void tearDown() {
        tempFile.delete();
    }
}
