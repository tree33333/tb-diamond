package com.taobao.diamond.server.controller;

import static com.taobao.diamond.common.Constants.WORD_SEPARATOR;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.processor.NotifyTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RealTimeNotifyTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RedisTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RemoveConfigInfoTaskProcessor;
import com.taobao.diamond.server.service.task.processor.UpdateAllConfigInfoTaskProcessor;
import com.taobao.diamond.server.service.task.processor.UpdateConfigInfoTaskProcessor;
import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;


public class BaseStoneControllerUnitTest extends AbstractControllerUnitTest {

    private BaseStoneController baseStoneController;

    private PushitClient client;

    private final AtomicBoolean notified = new AtomicBoolean();
    private final AtomicReference<String> id = new AtomicReference<String>();
    private final AtomicReference<String> grp = new AtomicReference<String>();


    @Before
    public void setUp() throws Exception {
        super.setUp();
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

        RedisTaskProcessor redisTaskProcessor = new RedisTaskProcessor();
        redisTaskProcessor.setRedisService(redisService);
        RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor = new RealTimeNotifyTaskProcessor();
        realTimeNotifyTaskProcessor.setPushitService(pushitService);
        NotifyTaskProcessor notifyProcessor = new NotifyTaskProcessor();
        notifyProcessor.setNotifyService(notifyService);

        UpdateConfigInfoTaskProcessor updateConfigInfoTaskProcessor = new UpdateConfigInfoTaskProcessor();
        updateConfigInfoTaskProcessor.setConfigService(configService);
        updateConfigInfoTaskProcessor.setAggrService(aggregationService);
        updateConfigInfoTaskProcessor.setRealTimeNotifyTaskProcessor(realTimeNotifyTaskProcessor);
        updateConfigInfoTaskProcessor.setRedisTaskProcessor(redisTaskProcessor);
        updateConfigInfoTaskProcessor.setTaskManagerService(taskManagerService);

        RemoveConfigInfoTaskProcessor removeConfigInfoTaskProcessor = new RemoveConfigInfoTaskProcessor();
        removeConfigInfoTaskProcessor.setConfigService(configService);
        removeConfigInfoTaskProcessor.setAggrService(aggregationService);
        removeConfigInfoTaskProcessor.setRealTimeNotifyTaskProcessor(realTimeNotifyTaskProcessor);
        removeConfigInfoTaskProcessor.setTaskManagerService(taskManagerService);

        UpdateAllConfigInfoTaskProcessor updateAllConfigInfoTaskProcessor = new UpdateAllConfigInfoTaskProcessor();
        updateAllConfigInfoTaskProcessor.setConfigService(configService);
        updateAllConfigInfoTaskProcessor.setAggrService(aggregationService);
        updateAllConfigInfoTaskProcessor.setRealTimeNotifyTaskProcessor(realTimeNotifyTaskProcessor);
        updateAllConfigInfoTaskProcessor.setTaskManagerService(taskManagerService);

        this.baseStoneController = new BaseStoneController();
        this.baseStoneController.setConfigService(configService);
        this.baseStoneController.setPushitService(pushitService);
        this.baseStoneController.setTaskManagerService(taskManagerService);
        this.baseStoneController.setAggregationService(aggregationService);
        this.baseStoneController.setUpdateConfigInfoTaskProcessor(updateConfigInfoTaskProcessor);
        this.baseStoneController.setRemoveConfigInfoTaskProcessor(removeConfigInfoTaskProcessor);
        this.baseStoneController.setUpdateAllConfigInfoTaskProcessor(updateAllConfigInfoTaskProcessor);
        this.baseStoneController.setRedisTaskProcessor(redisTaskProcessor);
        this.baseStoneController.setRealTimeNotifyTaskProcessor(realTimeNotifyTaskProcessor);
        this.baseStoneController.getConfigService().setNotifyTaskProcessor(notifyProcessor);
        this.baseStoneController.getConfigService().setTaskManagerService(taskManagerService);

        initPushitClient(notified, id, grp);
    }


    @After
    public void tearDown() throws Exception {
        client.stop();
    }


    @Test
    public void testPostConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-test";
        final String group = "leiwen";
        final String content = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-test");
        try {
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert.assertEquals("200", this.baseStoneController.postConfig(request, response, dataId, group, content));
            file = new File(path + "/config-data/leiwen/basestone-test");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(content));
        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testPostConfigAndRedis() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-redis-test";
        final String group = "leiwen";
        final String content = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-redis-test");
        try {
            // 发布数据
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert.assertEquals("200", this.baseStoneController.postConfig(request, response, dataId, group, content));
            file = new File(path + "/config-data/leiwen/basestone-redis-test");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(content));
            // 验证ip和dataId的映射
            String value = dataId + Constants.WORD_SEPARATOR + group;
            Thread.sleep(1000);
            Assert.assertTrue(this.baseStoneController.getConfigService().getRedisService().get("basestone-127.0.0.1")
                .contains(value));
            this.baseStoneController.getConfigService().getRedisService().remove("basestone-127.0.0.1", value);
            Assert.assertFalse(this.baseStoneController.getConfigService().getRedisService().get("basestone-127.0.0.1")
                .contains(value));
        }
        finally {
            file.delete();
        }
    }


    @Test
    public void testPostConfigAndRealTimeNotify() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-pushit-test";
        final String group = "leiwen";
        final String content = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-pushit-test");
        try {
            this.client.interest(dataId, group);
            // 发布数据
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert.assertEquals("200", this.baseStoneController.postConfig(request, response, dataId, group, content));
            file = new File(path + "/config-data/leiwen/basestone-pushit-test");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(content));
            // 接收实时通知
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            assertEquals(dataId, id.get());
            assertEquals(group, grp.get());
        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    @Test
    public void testPostConfigFail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals("536", this.baseStoneController.postConfig(request, response, "", "hello", "test"));
        assertEquals("无效的DataId", response.getErrorMessage());

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        assertEquals("536", this.baseStoneController.postConfig(request, response, "notify", "hello test", "test"));
        assertEquals("无效的分组", response.getErrorMessage());

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        assertEquals("536", this.baseStoneController.postConfig(request, response, "notify", "hello", null));
        assertEquals("无效的内容", response.getErrorMessage());
    }


    @Test
    public void testUpdateConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-update-test";
        final String group = "leiwen";
        final String content = "testtesttest" + WORD_SEPARATOR + "testtesttest";
        final String realContent = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-update-test");
        try {
            this.client.interest(dataId, group);
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert
                .assertEquals("200", this.baseStoneController.updateConfig(request, response, dataId, group, content));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            notified.set(false);
            file = new File(path + "/config-data/leiwen/basestone-update-test");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(realContent));
            // 更新数据
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            // 更新的数据必须有分隔符
            final String newContent = "newnewnew" + WORD_SEPARATOR + "newnewnew";
            final String realNewContent = "newnewnew";
            Assert.assertEquals("200",
                this.baseStoneController.updateConfig(request, response, dataId, group, newContent));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertTrue(this.getContentList(file).contains(realNewContent));
            assertEquals(dataId, id.get());
            assertEquals(group, grp.get());
            // 增量更新, 老的数据仍然存在
            Assert.assertTrue(this.getContentList(file).contains(realContent));
        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    /**
     * 更新数据, 数据的标识符相同
     * 
     * @throws Exception
     */
    @Test
    public void testUpdateConfig2() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-update-test2";
        final String group = "leiwen";
        final String content = "testtesttest" + WORD_SEPARATOR + "testtesttest";
        final String realContent = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-update-test2");
        try {
            this.client.interest(dataId, group);
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert
                .assertEquals("200", this.baseStoneController.updateConfig(request, response, dataId, group, content));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            notified.set(false);
            file = new File(path + "/config-data/leiwen/basestone-update-test2");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(realContent));
            // 更新数据
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            // 数据的标识符和原来相同
            final String newContent = "testtesttest" + WORD_SEPARATOR + "newnewnew";
            final String realNewContent = "newnewnew";
            Assert.assertEquals("200",
                this.baseStoneController.updateConfig(request, response, dataId, group, newContent));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertTrue(this.getContentList(file).contains(realNewContent));
            assertEquals(dataId, id.get());
            assertEquals(group, grp.get());
            // 老的数据被覆盖, 因为标识符相同
            Assert.assertFalse(this.getContentList(file).contains(realContent));
        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    @Test
    public void testUpdateValidationFail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertEquals("536", this.baseStoneController.updateConfig(request, response, "", "hello", "test"));
        assertEquals("无效的DataId", response.getErrorMessage());

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        assertEquals("536", this.baseStoneController.updateConfig(request, response, "notify", "hello test", "test"));
        assertEquals("无效的分组", response.getErrorMessage());

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        assertEquals("536", this.baseStoneController.updateConfig(request, response, "notify", "hello", null));
        assertEquals("无效的内容", response.getErrorMessage());
    }


    @Test
    public void testUpdateAll() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-update-all-test";
        final String group = "leiwen";
        final String content = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-update-all-test");
        try {
            this.client.interest(dataId, group);
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert.assertEquals("200",
                this.baseStoneController.updateConfigAll(request, response, dataId, group, content));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            notified.set(false);
            file = new File(path + "/config-data/leiwen/basestone-update-all-test");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(content));
            // 更新数据
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            // 更新的数据必须有分隔符
            final String newContent = "newnewnew";
            Assert.assertEquals("200",
                this.baseStoneController.updateConfigAll(request, response, dataId, group, newContent));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertTrue(this.getContentList(file).contains(newContent));
            Assert.assertFalse(this.getContentList(file).contains(content));
            assertEquals(dataId, id.get());
            assertEquals(group, grp.get());
        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    @Test
    public void testDeleteConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-delete-test";
        final String group = "leiwen";
        final String content = "testtesttest" + WORD_SEPARATOR + "testtesttest";
        final String realContent = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-delete-test");
        try {
            this.client.interest(dataId, group);
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert
                .assertEquals("200", this.baseStoneController.updateConfig(request, response, dataId, group, content));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            notified.set(false);
            file = new File(path + "/config-data/leiwen/basestone-delete-test");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(realContent));
            // 更新数据
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            // 更新的数据必须有分隔符
            final String newContent = "newnewnew" + WORD_SEPARATOR + "newnewnew";
            final String realNewContent = "newnewnew";
            Assert.assertEquals("200",
                this.baseStoneController.updateConfig(request, response, dataId, group, newContent));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            notified.set(false);
            Assert.assertTrue(this.getContentList(file).contains(realNewContent));
            // 删除一条数据
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            final String deleteContent = "newnewnew";
            Assert.assertEquals("200",
                this.baseStoneController.deleteConfig(request, response, dataId, group, deleteContent));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertFalse(this.getContentList(file).contains(realNewContent));
            Assert.assertTrue(this.getContentList(file).contains(realContent));
        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    /**
     * 删除的dataId不存在
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteConfig2() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-delete-test-nonexistDataId";
        final String group = "leiwen";
        final String content = "testtesttest" + WORD_SEPARATOR + "testtesttest";
        mockServletContext(dataId, group, content);
        String result = this.baseStoneController.deleteConfig(request, response, dataId, group, content);
        Thread.sleep(1000);
        Assert.assertEquals("200", result);
    }


    /**
     * 删除的dataId存在, 内容不存在
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteConfig3() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-delete-test-nonexistContent";
        final String group = "leiwen";
        final String content = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-delete-test-nonexistContent");
        try {
            this.client.interest(dataId, group);
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert.assertEquals("200", this.baseStoneController.postConfig(request, response, dataId, group, content));
            file = new File(path + "/config-data/leiwen/basestone-delete-test-nonexistContent");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(content));

            Assert.assertEquals("200", this.baseStoneController.deleteConfig(request, response, dataId, group, "asdf"));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertTrue(file.exists());

        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    /**
     * 新增一条数据, 删除一条数据, 整个数据项也删除
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteConfig4() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String dataId = "basestone-delete-test-single";
        final String group = "leiwen";
        final String content = "testtesttest";
        File file = new File(path + "/config-data/leiwen/basestone-delete-test-single");
        try {
            this.client.interest(dataId, group);
            Assert.assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            Assert.assertEquals("200", this.baseStoneController.postConfig(request, response, dataId, group, content));
            file = new File(path + "/config-data/leiwen/basestone-delete-test-single");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(this.getContentList(file).contains(content));

            Assert
                .assertEquals("200", this.baseStoneController.deleteConfig(request, response, dataId, group, content));
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertFalse(file.exists());
        }
        finally {
            notified.set(false);
            id.set(null);
            grp.set(null);
            file.delete();
        }
    }


    @SuppressWarnings("unchecked")
    private List<String> getContentList(File file) throws Exception {
        List<String> result = new ArrayList<String>();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
            result = IOUtils.readLines(fileReader);
            return result;
        }
        finally {
            fileReader.close();
        }
    }


    private void initPushitClient(final AtomicBoolean notified, final AtomicReference<String> id,
            final AtomicReference<String> grp) throws Exception {
        String pushitServers = this.baseStoneController.getPushitService().getPushitServers();
        client = new PushitClient(pushitServers, new NotifyListener() {
            public void onNotify(String dataId, String group, String message) {
                notified.set(true);
                id.set(dataId);
                grp.set(group);
                System.out.println("receive config info:" + "dataId=" + dataId + ",group=" + group + ",message="
                        + message);
            }
        });
    }

}
