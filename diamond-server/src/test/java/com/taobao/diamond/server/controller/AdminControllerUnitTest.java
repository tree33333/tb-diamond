package com.taobao.diamond.server.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ModelMap;

import com.google.common.base.Joiner;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.processor.NotifyTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RealTimeNotifyTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RedisTaskProcessor;
import com.taobao.diamond.server.utils.GlobalCounter;
import com.taobao.diamond.utils.JSONUtils;


public class AdminControllerUnitTest extends AbstractControllerUnitTest {

    private AdminController adminController;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.adminController = new AdminController();
        adminController.setAdminService(adminService);
        configService.setRedisService(redisService);
        adminController.setConfigService(configService);
        adminController.setGroupService(groupService);
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

        adminController.setTaskManagerService(taskManagerService);
        adminController.setAggregationService(aggregationService);

        RedisTaskProcessor redisTaskProcessor = new RedisTaskProcessor();
        redisTaskProcessor.setRedisService(redisService);
        RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor = new RealTimeNotifyTaskProcessor();
        realTimeNotifyTaskProcessor.setPushitService(pushitService);
        NotifyTaskProcessor notifyProcessor = new NotifyTaskProcessor();
        notifyProcessor.setNotifyService(notifyService);

        adminController.setRedisTaskProcessor(redisTaskProcessor);
        adminController.setRealTimeNotifyTaskProcessor(realTimeNotifyTaskProcessor);

        adminController.getConfigService().setTaskManagerService(taskManagerService);
        adminController.getConfigService().setNotifyTaskProcessor(notifyProcessor);
    }


    @Test
    public void testPostConfig_listConfig_getConfigInfo_deleteConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();
        final String dataId = "notify";
        final String group = "boyan";
        final String content = "aaaaaaaaaaaaaaaaaaaaaaaaaa";
        File file = new File(path + "/config-data/boyan/notify");
        try {
            // 插入配置信息
            assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            assertEquals("/admin/config/list",
                this.adminController.postConfig(request, response, dataId, group, content, modelMap));
            file = new File(path + "/config-data/boyan/notify");
            assertTrue(file.exists());
            assertContent(content, file);
            assertEquals(dataId, modelMap.get("dataId"));
            assertEquals(group, modelMap.get("group"));
            Page<ConfigInfo> page = (Page<ConfigInfo>) modelMap.get("page");
            assertEquals(1, page.getPagesAvailable());
            assertEquals(1, page.getPageNumber());
            assertTrue(page.getPageItems().contains(new ConfigInfo(dataId, group, content)));

            // 查找配置信息
            modelMap = new ModelMap();
            assertEquals("/admin/config/edit",
                this.adminController.getConfigInfo(request, response, dataId, group, modelMap));
            ConfigInfo configInfo = (ConfigInfo) modelMap.get("configInfo");
            assertNotNull(configInfo);
            assertEquals(dataId, configInfo.getDataId());
            assertEquals(content, configInfo.getContent());
            assertEquals(group, configInfo.getGroup());

            // 删除配置信息
            long id = configInfo.getId();
            modelMap = new ModelMap();

            assertEquals("/admin/config/list", this.adminController.deleteConfig(request, response, id, modelMap));
            assertEquals("删除成功!", modelMap.get("message"));
            // 确认文件不存在
            file = new File(path + "/config-data/boyan/notify");
            assertFalse(file.exists());

        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testPostConfig_timestamp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();

        final String dataId = "adminControllerTimestamp";
        final String group = "postConfig";
        final String content = "test";
        final String srcIp = "192.168.0.1";
        final String srcUser = "leiwen.zh";

        File file = new File(path + "/config-data/" + group + "/" + dataId);
        try {
            // 插入配置信息
            assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            assertEquals("/admin/config/list",
                this.adminController.postConfig(request, response, dataId, group, content, srcIp, srcUser, modelMap));

            file = new File(path + "/config-data/" + group + "/" + dataId);
            assertTrue(file.exists());
            assertContent(content, file);
            assertEquals(dataId, modelMap.get("dataId"));
            assertEquals(group, modelMap.get("group"));
            Page<ConfigInfo> page = (Page<ConfigInfo>) modelMap.get("page");
            assertEquals(1, page.getPagesAvailable());
            assertEquals(1, page.getPageNumber());
            assertTrue(page.getPageItems().contains(new ConfigInfo(dataId, group, content)));
        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testUpdateConfig_timestamp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();

        final String dataId = "adminControllerTimestamp";
        final String group = "updateConfig";
        final String content = "test";
        final String srcIp = "192.168.0.1";
        final String srcUser = "leiwen.zh";

        File file = new File(path + "/config-data/" + group + "/" + dataId);
        try {
            // 插入配置信息
            assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            assertEquals("/admin/config/list",
                this.adminController.postConfig(request, response, dataId, group, content, srcIp, srcUser, modelMap));

            file = new File(path + "/config-data/" + group + "/" + dataId);
            assertTrue(file.exists());
            assertContent(content, file);

            Thread.sleep(1000);

            // 更新配置信息
            modelMap = new ModelMap();
            final String newContent = "test new";
            final String newSrcUser = "yinshi.nc";
            assertEquals("/admin/config/list", this.adminController.updateConfig(request, response, dataId, group,
                newContent, srcIp, newSrcUser, modelMap));
            assertEquals("提交成功!", modelMap.get("message"));
            // 确认文件内容已更新
            file = new File(path + "/config-data/" + group + "/" + dataId);
            assertTrue(file.exists());
            assertContent(newContent, file);
        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testDeleteConfigByDataId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();
        final String dataId = "notify";
        final String group = "leiwen";
        final String content = "test delete";
        File file = new File(path + "/config-data/leiwen/notify");
        try {
            // 插入配置信息
            assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            assertEquals("/admin/config/list",
                this.adminController.postConfig(request, response, dataId, group, content, modelMap));
            file = new File(path + "/config-data/leiwen/notify");
            assertTrue(file.exists());
            assertContent(content, file);
            assertEquals(dataId, modelMap.get("dataId"));
            assertEquals(group, modelMap.get("group"));
            Page<ConfigInfo> page = (Page<ConfigInfo>) modelMap.get("page");
            assertEquals(1, page.getPagesAvailable());
            assertEquals(1, page.getPageNumber());
            assertTrue(page.getPageItems().contains(new ConfigInfo(dataId, group, content)));

            // 删除配置信息
            modelMap = new ModelMap();

            assertEquals("/admin/config/list",
                this.adminController.deleteConfigByDataIdGroup(request, response, dataId, group, modelMap));
            assertEquals("删除成功!", modelMap.get("message"));
            // 确认文件不存在
            file = new File(path + "/config-data/leiwen/notify");
            assertFalse(file.exists());

        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testUpdateConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();
        final String dataId = "diamond";
        final String group = "leiwen";
        final String content = "test update";
        File file = new File(path + "/config-data/leiwen/diamond");
        try {
            // 插入配置信息
            assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            assertEquals("/admin/config/list",
                this.adminController.postConfig(request, response, dataId, group, content, modelMap));
            file = new File(path + "/config-data/leiwen/diamond");
            assertTrue(file.exists());
            assertContent(content, file);
            assertEquals(dataId, modelMap.get("dataId"));
            assertEquals(group, modelMap.get("group"));
            Page<ConfigInfo> page = (Page<ConfigInfo>) modelMap.get("page");
            assertEquals(1, page.getPagesAvailable());
            assertEquals(1, page.getPageNumber());
            assertTrue(page.getPageItems().contains(new ConfigInfo(dataId, group, content)));

            // 更新配置信息
            modelMap = new ModelMap();
            final String newContent = "test update new";
            assertEquals("/admin/config/list",
                this.adminController.updateConfig(request, response, dataId, group, newContent, modelMap));
            assertEquals("提交成功!", modelMap.get("message"));
            // 确认文件内容已更新
            file = new File(path + "/config-data/leiwen/diamond");
            assertTrue(file.exists());
            assertContent(newContent, file);

        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testUpload() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile multipartFile = new MockMultipartFile("test.xml", "<root>hello</root>".getBytes());
        ModelMap modelMap = new ModelMap();
        mockServletContext("dataId1", "test", "<root>hello</root>");
        assertEquals("/admin/config/list",
            this.adminController.upload(request, response, "dataId1", "test", multipartFile, modelMap));

        File file = new File(path + "/config-data/test/dataId1");
        assertTrue(file.exists());
        assertContent("<root>hello</root>", file);
        file.delete();

        assertEquals("提交成功!", modelMap.get("message"));

    }


    @Test
    public void testReupload() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile multipartFile = new MockMultipartFile("test.xml", "<root>hello</root>".getBytes());
        ModelMap modelMap = new ModelMap();
        mockServletContext("dataId1", "test", "<root>hello</root>");
        this.configService.addConfigInfo("dataId1", "test", "old content");
        File file = new File(path + "/config-data/test/dataId1");
        assertTrue(file.exists());
        assertContent("old content", file);
        assertEquals("/admin/config/list",
            this.adminController.reupload(request, response, "dataId1", "test", multipartFile, modelMap));
        file = new File(path + "/config-data/test/dataId1");
        assertTrue(file.exists());
        assertContent("<root>hello</root>", file);
        file.delete();
    }


    /**
     * 批量新增测试, 由于mockServletContext以group + dataId为粒度, 所以这里批量处理记录的个数为1,
     * 真实的批量测试在sdk的单元测试中进行
     * 
     * 这个用例测试批量新增成功, 即数据库中原来没有数据, 成功插入批量请求的数据
     */
    @Test
    public void testBatchAdd_成功() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();

        String dataId = UUID.randomUUID().toString() + "-batchAddDataId";
        String group = "test";
        String content = "batchAddSuccess";
        String srcIp = "127.0.0.1";
        String srcUser = "leiwen.zh";
        // 构造dataId和content的字符串
        Map<String/* dataId */, String/* content */> dataId2ContentMap = new HashMap<String, String>();
        for (int i = 0; i < 1; i++) {
            dataId2ContentMap.put(dataId, content);
        }
        String allDataIdAndContent = this.generateBatchOpString(dataId2ContentMap);
        // 新增
        mockServletContext(dataId, group, content);

        assertEquals("/admin/config/batch_result", this.adminController.batchAddOrUpdate(request, response,
            allDataIdAndContent, group, srcIp, srcUser, modelMap));

        // 反序列化, 验证结果
        String json = (String) modelMap.get("json");
        Object resultObj = null;
        try {
            resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
            });
            if (!(resultObj instanceof List<?>)) {
                throw new RuntimeException("反序列化失败, 类型不是List");
            }
        }
        catch (Exception e) {
            fail("反序列化失败, 类型不是List");
        }
        List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
        assertEquals(1, resultList.size());
        ConfigInfoEx result = resultList.get(0);

        assertEquals(dataId, result.getDataId());
        assertEquals(group, result.getGroup());
        assertEquals(content, result.getContent());
        assertEquals(Constants.BATCH_ADD_SUCCESS, result.getStatus());
        assertEquals("add success", result.getMessage());
    }


    /**
     * 这个用例测试批量更新成功, 即数据库原来有数据, 更新数据
     */
    @Test
    public void testBatchUpdate_成功() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();

        String dataId = UUID.randomUUID().toString() + "-batchUpdateDataId";
        String group = "test";
        String content = "batchUpdateSuccess";
        String newContent = "batchUpdateSuccess2";
        String srcIp = "127.0.0.1";
        String srcUser = "leiwen.zh";

        // 先新增一次
        mockServletContext(dataId, group, content);
        this.adminController.postConfig(request, response, dataId, group, content, modelMap);

        // 构造批量更新的字符串
        Map<String/* dataId */, String/* content */> dataId2ContentMap = new HashMap<String, String>();
        for (int i = 0; i < 1; i++) {
            dataId2ContentMap.put(dataId, newContent);
        }
        String allDataIdAndContent = this.generateBatchOpString(dataId2ContentMap);

        Thread.sleep(1000);

        // 批量更新
        assertEquals("/admin/config/batch_result", this.adminController.batchAddOrUpdate(request, response,
            allDataIdAndContent, group, srcIp, srcUser, modelMap));

        // 反序列化, 验证结果
        String json = (String) modelMap.get("json");
        Object resultObj = null;
        try {
            resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
            });
            if (!(resultObj instanceof List<?>)) {
                throw new RuntimeException("反序列化失败, 类型不是List");
            }
        }
        catch (Exception e) {
            fail("反序列化失败, 类型不是List");
        }
        List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
        assertEquals(1, resultList.size());
        ConfigInfoEx result = resultList.get(0);

        assertEquals(dataId, result.getDataId());
        assertEquals(group, result.getGroup());
        assertEquals(newContent, result.getContent());
        assertEquals(Constants.BATCH_UPDATE_SUCCESS, result.getStatus());
        assertEquals("update success", result.getMessage());
    }


    /**
     * 这个用例测试批量查询成功, 即数据库原来有数据
     */
    @Test
    public void testBatchQuery_成功() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();

        String dataId = UUID.randomUUID().toString() + "-batchQueryDataId";
        String group = "test";
        String content = "batchQuerySuccess";

        // 新增
        mockServletContext(dataId, group, content);
        this.adminController.postConfig(request, response, dataId, group, content, modelMap);

        // 构造批量查询的dataId字符串
        List<String> dataIdList = new LinkedList<String>();
        dataIdList.add(dataId);
        String dataIds = this.generateBatchQueryString(dataIdList);

        // 批量查询
        assertEquals("/admin/config/batch_result",
            this.adminController.batchQuery(request, response, dataIds, group, modelMap));

        // 反序列化, 验证结果
        String json = (String) modelMap.get("json");
        Object resultObj = null;
        try {
            resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
            });
            if (!(resultObj instanceof List<?>)) {
                throw new RuntimeException("反序列化失败, 类型不是List");
            }
        }
        catch (Exception e) {
            fail("反序列化失败, 类型不是List");
        }
        List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
        assertEquals(1, resultList.size());
        ConfigInfoEx result = resultList.get(0);

        assertEquals(dataId, result.getDataId());
        assertEquals(group, result.getGroup());
        assertEquals(content, result.getContent());
        assertEquals(Constants.BATCH_QUERY_EXISTS, result.getStatus());
        assertEquals("query success", result.getMessage());
    }


    /**
     * 这个用例测试批量查询失败, 即数据库中不存在数据
     */
    @Test
    public void testBatchQuery_失败() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRemoteAddr("0.0.0.0");
        ModelMap modelMap = new ModelMap();

        String dataId = UUID.randomUUID().toString() + "-batchQueryDataId";
        String group = "test";

        // 构造批量查询的dataId字符串
        List<String> dataIdList = new LinkedList<String>();
        dataIdList.add(dataId);
        String dataIds = this.generateBatchQueryString(dataIdList);

        // 批量查询
        assertEquals("/admin/config/batch_result",
            this.adminController.batchQuery(request, response, dataIds, group, modelMap));

        // 反序列化, 验证结果
        String json = (String) modelMap.get("json");
        Object resultObj = null;
        try {
            resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
            });
            if (!(resultObj instanceof List<?>)) {
                throw new RuntimeException("反序列化失败, 类型不是List");
            }
        }
        catch (Exception e) {
            fail("反序列化失败, 类型不是List");
        }
        List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
        assertEquals(1, resultList.size());
        ConfigInfoEx result = resultList.get(0);

        assertEquals(dataId, result.getDataId());
        assertEquals(group, result.getGroup());
        assertEquals(null, result.getContent());
        assertEquals(Constants.BATCH_QUERY_NONEXISTS, result.getStatus());
        assertEquals("query data does not exist", result.getMessage());
    }


    private String generateBatchOpString(Map<String, String> dataId2ContentMap) {
        List<String> dataIdAndContentList = new LinkedList<String>();
        for (String dataId : dataId2ContentMap.keySet()) {
            String content = dataId2ContentMap.get(dataId);
            dataIdAndContentList.add(dataId + Constants.WORD_SEPARATOR + content);
        }
        String allDataIdAndContent = Joiner.on(Constants.LINE_SEPARATOR).join(dataIdAndContentList);
        return allDataIdAndContent;
    }


    private String generateBatchQueryString(List<String> dataIds) {
        return Joiner.on(Constants.WORD_SEPARATOR).join(dataIds);
    }


    @Test
    public void testAddGroup_listGroup_moveGroup_deleteGroup() throws Exception {
        // 添加分组
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        final String address = "192.168.207.111";
        final String dataId = "notify";
        final String group = "boyan";
        assertEquals("/admin/group/list",
            this.adminController.addGroup(request, response, address, dataId, group, modelMap));
        assertEquals("添加成功!", modelMap.get("message"));
        // 查询
        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> allAddressGroupMap =
                (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        assertGroupInfo(address, dataId, group, allAddressGroupMap);

        GroupInfo groupInfo = allAddressGroupMap.get(address).get(dataId);
        long id = groupInfo.getId();

        // 移动分组
        modelMap = new ModelMap();
        assertEquals("/admin/group/list", this.adminController.moveGroup(request, response, id, "boyan-new", modelMap));
        assertEquals("移动分组成功!", modelMap.get("message"));
        allAddressGroupMap = (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        assertGroupInfo(address, dataId, "boyan-new", allAddressGroupMap);

        // 删除分组
        modelMap = new ModelMap();
        assertEquals("/admin/group/list", this.adminController.deleteGroup(request, response, id, modelMap));
        assertEquals("删除成功!", modelMap.get("message"));
        allAddressGroupMap = (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        // 仍然有地址，但是没有分组信息
        assertEquals(1, allAddressGroupMap.size());
        assertEquals(0, allAddressGroupMap.get(address).size());
    }


    @Test
    public void testAddGroup_timestamp() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        final String address = "192.168.207.111";
        final String dataId = "adminControllerTimestamp";
        final String group = "addGroup";
        final String srcIp = "127.0.0.1";
        final String srcUser = "leiwen.zh";

        assertEquals("/admin/group/list",
            this.adminController.addGroup(request, response, address, dataId, group, srcIp, srcUser, modelMap));

        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> allAddressGroupMap =
                (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        assertGroupInfo(address, dataId, group, allAddressGroupMap);
    }


    @Test
    public void testUpdateGroup_timestamp() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        final String address = "192.168.207.111";
        final String dataId = "adminControllerTimestamp";
        final String group = "updateGroup";
        final String srcIp = "127.0.0.1";
        final String srcUser = "leiwen.zh";

        assertEquals("/admin/group/list",
            this.adminController.addGroup(request, response, address, dataId, group, srcIp, srcUser, modelMap));

        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> allAddressGroupMap =
                (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        assertGroupInfo(address, dataId, group, allAddressGroupMap);

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // 更新
        GroupInfo gInfo = allAddressGroupMap.get(address).get(dataId);
        long id = gInfo.getId();
        final String newGroup = "newGroup";
        final String newSrcUser = "yinshi.nc";

        assertEquals("/admin/group/list",
            this.adminController.moveGroup(request, response, id, newGroup, srcIp, newSrcUser, modelMap));

        allAddressGroupMap = (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        assertGroupInfo(address, dataId, newGroup, allAddressGroupMap);
    }


    @Test
    public void testAddUser_listUser_changePassword_deleteUser() throws Exception {
        // 重复添加，失败
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        try {
            ModelMap modelMap = new ModelMap();

            assertEquals("/admin/user/list",
                this.adminController.addUser(request, response, "admin", "admin", modelMap));
            assertEquals("添加失败!", modelMap.get("message"));

            Map<String, String> userMap = (Map<String, String>) modelMap.get("userMap");
            assertNotNull(userMap);
            assertEquals("admin", userMap.get("admin"));
            assertEquals(1, userMap.size());

            // 添加新用户
            modelMap = new ModelMap();
            assertEquals("/admin/user/list",
                this.adminController.addUser(request, response, "boyan", "boyan", modelMap));
            assertEquals("添加成功!", modelMap.get("message"));
            userMap = (Map<String, String>) modelMap.get("userMap");
            assertNotNull(userMap);
            assertEquals("boyan", userMap.get("boyan"));
            assertEquals(2, userMap.size());

            // 修改密码
            // 1、修改不存在的用户密码
            modelMap = new ModelMap();
            assertEquals("/admin/user/list",
                this.adminController.changePassword(request, response, "test", "newpass", modelMap));
            assertEquals("更改失败!", modelMap.get("message"));

            // 2、修改boyan密码
            modelMap = new ModelMap();
            assertEquals("/admin/user/list",
                this.adminController.changePassword(request, response, "boyan", "newpass", modelMap));
            assertEquals("更改成功,下次登录请用新密码！", modelMap.get("message"));
            userMap = (Map<String, String>) modelMap.get("userMap");
            assertNotNull(userMap);
            assertEquals("newpass", userMap.get("boyan"));
            assertEquals(2, userMap.size());

            // 删除用户
            // 1、删除不存在的用户，失败
            modelMap = new ModelMap();
            assertEquals("/admin/user/list", this.adminController.deleteUser(request, response, "test", modelMap));
            assertEquals("删除失败!", modelMap.get("message"));

            // 2、删除boyan
            modelMap = new ModelMap();
            assertEquals("/admin/user/list", this.adminController.deleteUser(request, response, "boyan", modelMap));
            assertEquals("删除成功!", modelMap.get("message"));
            userMap = (Map<String, String>) modelMap.get("userMap");
            assertNotNull(userMap);
            assertNull(userMap.get("boyan"));
            assertEquals(1, userMap.size());
        }
        finally {
            this.adminController.deleteUser(request, response, "boyan", new ModelMap());
        }

    }


    @Test
    public void testReloadUser() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();

        assertEquals("/admin/user/list", this.adminController.reloadUser(request, response, modelMap));
        Map<String, String> allUserMap = this.adminController.getAdminService().getAllUsers();
        assertEquals("admin", allUserMap.get("admin"));
        assertEquals("加载成功!", modelMap.get("message"));
    }


    @Test
    public void testReloadGroup() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        this.adminController.listGroup(request, response, modelMap);
        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> allAddressGroupMap =
                (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");

        assertTrue(allAddressGroupMap.isEmpty());

        // System.out.println(allAddressGroupMap);

        // 直接插入数据
        this.persistService.addGroupInfo(new GroupInfo("localhost", "notify", "boyan"));

        // 查询仍然为空
        modelMap = new ModelMap();
        this.adminController.listGroup(request, response, modelMap);
        allAddressGroupMap = (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");
        assertTrue(allAddressGroupMap.isEmpty());

        // reload一下
        modelMap = new ModelMap();
        assertEquals("/admin/group/list", this.adminController.reloadGroupInfo(request, response, modelMap));

        allAddressGroupMap = (ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>) modelMap.get("groupMap");

        assertFalse(allAddressGroupMap.isEmpty());
        assertGroupInfo("localhost", "notify", "boyan", allAddressGroupMap);

    }


    private void assertGroupInfo(final String address, final String dataId, final String group,
            ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> allAddressGroupMap) {
        assertNotNull(allAddressGroupMap);
        assertEquals(1, allAddressGroupMap.size());
        assertTrue(allAddressGroupMap.containsKey(address));
        assertTrue(allAddressGroupMap.get(address).containsKey(dataId));
        assertEquals(group, allAddressGroupMap.get(address).get(dataId).getGroup());
    }


    @Test
    public void testSetRefuseRequstCount_GetRefuseRequestCount() {
        ModelMap modelMap = new ModelMap();
        // 设置的计数小于等于0
        assertEquals("/admin/count", this.adminController.setRefuseRequestCount(0, modelMap));
        assertEquals("非法的计数", modelMap.get("message"));
        assertNull(modelMap.get("count"));

        modelMap = new ModelMap();
        assertEquals("/admin/count", this.adminController.setRefuseRequestCount(100, modelMap));
        assertEquals("设置成功!", modelMap.get("message"));
        assertEquals(100L, modelMap.get("count"));

        GlobalCounter.getCounter().set(0);
    }


    @Test
    public void testPostConfigFail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/config/new",
            this.adminController.postConfig(request, response, "", "hello", "test", modelMap));
        assertEquals("无效的DataId", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        assertEquals("/admin/config/new",
            this.adminController.postConfig(request, response, "notify", "hello test", "test", modelMap));
        assertEquals("无效的分组", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        assertEquals("/admin/config/new",
            this.adminController.postConfig(request, response, "notify", "hello", null, modelMap));
        assertEquals("无效的内容", modelMap.get("message"));

    }


    @Test
    public void testUpdateConfigFail() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/config/edit",
            this.adminController.updateConfig(request, response, "", "hello", "test", modelMap));
        assertEquals("无效的DataId", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        assertEquals("/admin/config/edit",
            this.adminController.updateConfig(request, response, "notify", "hello test", "test", modelMap));
        assertEquals("无效的分组", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        assertEquals("/admin/config/edit",
            this.adminController.updateConfig(request, response, "notify", "hello", null, modelMap));
        assertEquals("无效的内容", modelMap.get("message"));
    }


    @Test
    public void testUploadFail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile multipartFile = new MockMultipartFile("test.xml", "<root>hello</root>".getBytes());
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/config/upload",
            this.adminController.upload(request, response, "", "hello", multipartFile, modelMap));
        assertEquals("无效的DataId", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        assertEquals("/admin/config/upload",
            this.adminController.upload(request, response, "notify", "hello test", multipartFile, modelMap));
        assertEquals("无效的分组", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        multipartFile = new MockMultipartFile("test.xml", "".getBytes());
        assertEquals("/admin/config/upload",
            this.adminController.upload(request, response, "notify", "hello", multipartFile, modelMap));
        assertEquals("无效的内容", modelMap.get("message"));
    }


    @Test
    public void testReuploadFail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile multipartFile = new MockMultipartFile("test.xml", "<root>hello</root>".getBytes());
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/config/edit",
            this.adminController.reupload(request, response, "", "hello", multipartFile, modelMap));
        assertEquals("无效的DataId", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        assertEquals("/admin/config/edit",
            this.adminController.reupload(request, response, "notify", "hello test", multipartFile, modelMap));
        assertEquals("无效的分组", modelMap.get("message"));

        response = new MockHttpServletResponse();
        modelMap = new ModelMap();
        multipartFile = new MockMultipartFile("test.xml", "".getBytes());
        assertEquals("/admin/config/edit",
            this.adminController.reupload(request, response, "notify", "hello", multipartFile, modelMap));
        assertEquals("无效的内容", modelMap.get("message"));
    }


    @Test
    public void addGroup_fail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/group/new",
            this.adminController.addGroup(request, response, "localhost", "hello", "", modelMap));
        assertEquals("无效的分组名称", modelMap.get("message"));

        modelMap = new ModelMap();
        assertEquals("/admin/group/new",
            this.adminController.addGroup(request, response, "localhost", "", "boyan", modelMap));
        assertEquals("无效的dataId", modelMap.get("message"));

        modelMap = new ModelMap();
        assertEquals("/admin/group/new",
            this.adminController.addGroup(request, response, "", "hello", "boyan", modelMap));
        assertEquals("无效的地址", modelMap.get("message"));
        this.adminController.addGroup(request, response, "localhost", "notify", "boyan", modelMap);
        assertEquals("添加成功!", modelMap.get("message"));
        // 重复添加
        assertEquals("/admin/group/list",
            this.adminController.addGroup(request, response, "localhost", "notify", "boyan", modelMap));
        assertEquals("地址已存在，请将该地址移动到新分组", modelMap.get("message"));

    }


    @Test
    public void moveGroup_fail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/group/list", this.adminController.moveGroup(request, response, 0, "", modelMap));
        assertEquals("无效的新分组名称", modelMap.get("message"));
    }


    @Test
    public void testAddUser_fail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/user/list", this.adminController.addUser(request, response, "", "123", modelMap));
        assertEquals("无效的用户名", modelMap.get("message"));

        modelMap = new ModelMap();
        assertEquals("/admin/user/new", this.adminController.addUser(request, response, "leiwen", "", modelMap));
        assertEquals("无效的密码", modelMap.get("message"));
    }


    @Test
    public void testDeleteUser_fail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/user/list", this.adminController.deleteUser(request, response, "", modelMap));
        assertEquals("无效的用户名", modelMap.get("message"));
    }


    @Test
    public void testChangePassword_fail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/user/list", this.adminController.changePassword(request, response, "", "123", modelMap));
        assertEquals("无效的用户名", modelMap.get("message"));

        modelMap = new ModelMap();
        assertEquals("/admin/user/list", this.adminController.changePassword(request, response, "leiwen", "", modelMap));
        assertEquals("无效的新密码", modelMap.get("message"));
    }


    private void assertContent(final String content, File file) throws FileNotFoundException, IOException {
        // 校验内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line = reader.readLine();
        assertNotNull(line);
        assertEquals(content, line);
        reader.close();
    }


    @Test
    public void testListConfigJson() throws Exception {
        ModelMap modelMap = new ModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        final String dataId = "test-dataId";
        final String group = "test-group";

        mockServletContext(dataId, group, "content");
        this.configService.addConfigInfo(dataId, group, "content");
        this.mocksControl.verify();
        this.mocksControl.reset();

        request.addHeader("Accept", "application/json");
        assertEquals("/admin/config/list_json",
            this.adminController.listConfig(request, response, dataId, group, 1, 15, modelMap));
        String json = (String) modelMap.get("pageJson");
        assertNotNull(json);
        Page<ConfigInfo> page =
                (Page<ConfigInfo>) JSONUtils.deserializeObject(json, new TypeReference<Page<ConfigInfo>>() {
                });
        assertEquals(1, page.getPageNumber());
        assertEquals(1, page.getPagesAvailable());
        assertEquals(1, page.getTotalCount());
        assertEquals(1, page.getPageItems().size());
        ConfigInfo configInfo = page.getPageItems().get(0);
        assertEquals(dataId, configInfo.getDataId());
        assertEquals(group, configInfo.getGroup());
        assertEquals("content", configInfo.getContent());
    }


    @Test
    public void testListConfigInfoLike() {

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 没有设置查询条件的情况
        ModelMap modelMap = new ModelMap();
        assertEquals("/admin/config/list",
            this.adminController.listConfigLike(new MockHttpServletRequest(), response, null, null, 1, 100, modelMap));
        assertEquals("模糊查询请至少设置一个查询参数", modelMap.get("message"));
        assertNull(modelMap.get("page"));

        // 预先添加部分数据
        final String dataId = "test-dataId";
        final String group = "test-group";
        for (int i = 0; i < 10; i++) {
            mockServletContext(dataId + i, group + i, "content" + i);
            this.configService.addConfigInfo(dataId + i, group + i, "content" + i);
            this.mocksControl.verify();
            this.mocksControl.reset();
        }

        // 模糊查询
        modelMap = new ModelMap();
        assertEquals("/admin/config/list", this.adminController.listConfigLike(new MockHttpServletRequest(), response,
            dataId, group, 1, 100, modelMap));
        assertEquals("listConfigLike", modelMap.get("method"));
        Page<ConfigInfo> page = (Page<ConfigInfo>) modelMap.get("page");
        assertNotNull(page);
        assertEquals(1, page.getPageNumber());
        assertEquals(1, page.getPagesAvailable());
        assertEquals(10, page.getPageItems().size());
    }


    @Test
    public void testListConfigInfoLikeJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 没有设置查询条件的情况
        ModelMap modelMap = new ModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals("/admin/config/list",
            this.adminController.listConfigLike(request, response, null, null, 1, 100, modelMap));
        assertEquals("模糊查询请至少设置一个查询参数", modelMap.get("message"));
        assertNull(modelMap.get("page"));

        // 预先添加部分数据
        final String dataId = "test-dataId";
        final String group = "test-group";
        for (int i = 0; i < 10; i++) {
            mockServletContext(dataId + i, group + i, "content" + i);
            this.configService.addConfigInfo(dataId + i, group + i, "content" + i);
            this.mocksControl.verify();
            this.mocksControl.reset();
        }

        // 模糊查询
        modelMap = new ModelMap();
        request.addHeader("Accept", "application/json");
        assertEquals("/admin/config/list_json",
            this.adminController.listConfigLike(request, response, dataId, group, 1, 100, modelMap));
        String json = (String) modelMap.get("pageJson");
        assertNotNull(json);
        Page<ConfigInfo> page =
                (Page<ConfigInfo>) JSONUtils.deserializeObject(json, new TypeReference<Page<ConfigInfo>>() {
                });
        assertEquals(1, page.getPageNumber());
        assertEquals(1, page.getPagesAvailable());
        assertEquals(10, page.getTotalCount());
        assertEquals(10, page.getPageItems().size());
    }
}
