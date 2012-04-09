package com.taobao.diamond.server.service;

import java.io.File;

import javax.servlet.ServletContext;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.server.service.task.processor.NotifyTaskProcessor;

public class AggregationServiceUnitTest {

    private AggregationService aggrService;
    private ConfigService configService;
    private PersistService persistService;
    private ValidationService validationService;
    private DiskService diskService;
    private NotifyService notifyService;
    private ServletContext servletContext;
    private String path;
    private IMocksControl mocksControl;
    
    @Before
    public void setUp() throws Exception {
        File tempFile = File.createTempFile("AggregationServiceUnitTest", "tmp");
        path = tempFile.getParent();
        mocksControl = EasyMock.createControl();
        servletContext = mocksControl.createMock(ServletContext.class);
        
        ApplicationContext ctx = new ClassPathXmlApplicationContext("application.xml");
        persistService = (PersistService) ctx.getBean("persistService");
        validationService = new ValidationService();
        diskService = new DiskService();
        diskService.setServletContext(servletContext);
        notifyService = new NotifyService();
        
        NotifyTaskProcessor notifyProcessor = new NotifyTaskProcessor();
        notifyProcessor.setNotifyService(notifyService);
        
        TaskManagerService taskManagerService = new TaskManagerService();
        TaskManager[] updateTaskManagers = new TaskManager[4];
        for(int i=0; i<4; i++) {
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
        
        configService = new ConfigService();
        configService.setValidationService(validationService);
        configService.setDiskService(diskService);
        configService.setNotifyService(notifyService);
        configService.setPersistService(persistService);
        configService.setNotifyTaskProcessor(notifyProcessor);
        configService.setTaskManagerService(taskManagerService);
        
        aggrService = new AggregationService();
        aggrService.setPersistService(persistService);
        aggrService.setConfigService(configService);
        truncateTable();
    }


    @After
    public void tearDown() {
        truncateTable();
    }
    
    
    @Test
    public void test_新增单项并生成聚合项() throws Exception {
        String dataId = "leiwen.test.ICO";
        String group = "test";
        String content = "ICO is a great game";
        
        String aggrDataId = "leiwen.test.PS2";
        String aggrGroup = "test";
        
        File file = new File(path + "/config-data/test/leiwen.test.ICO");
        File aggrFile = new File(path + "/config-data/test/leiwen.test.PS2");
        try {
            mockServletContext(dataId, aggrDataId, group);
            this.configService.addConfigInfo(dataId, group, content);
            this.aggrService.aggregation(dataId, group);
            
            ConfigInfo aggrConfigInfo = this.configService.findConfigInfo(aggrDataId, aggrGroup);
            String aggrContent = aggrConfigInfo.getContent();
            
            Assert.assertTrue(aggrContent.contains(content));
        }
        finally {
            file.delete();
            aggrFile.delete();
        }
    }
    
    
    @Test
    public void test_更新单项并更新聚合项() throws Exception {
        String dataId = "leiwen.test.ZELDA";
        String group = "test";
        String content = "ZELDA is an interesting game";
        String newContent = "SkySword is coming";
        
        String aggrDataId = "leiwen.test.WII";
        String aggrGroup = "test";
        
        File file = new File(path + "/config-data/test/leiwen.test.ZELDA");
        File aggrFile = new File(path + "/config-data/test/leiwen.test.WII");
        try {
            mockServletContext(dataId, aggrDataId, group);
            this.configService.addConfigInfo(dataId, group, content);
            this.aggrService.aggregation(dataId, group);
            
            ConfigInfo aggrConfigInfo = this.configService.findConfigInfo(aggrDataId, aggrGroup);
            String aggrContent = aggrConfigInfo.getContent();
            
            Assert.assertTrue(aggrContent.contains(content));
            
            //更新单项
            this.configService.updateConfigInfo(dataId, group, newContent);
            this.aggrService.aggregation(dataId, group);
            
            ConfigInfo newAggrConfigInfo = this.configService.findConfigInfo(aggrDataId, aggrGroup);
            String newAggrContent = newAggrConfigInfo.getContent();
            
            Assert.assertFalse(newAggrContent.contains(content));
            Assert.assertTrue(newAggrContent.contains(newContent));
        }
        finally {
            file.delete();
            aggrFile.delete();
        }
    }
    
    
    @Test
    public void test_删除单项并删除聚合项() throws Exception {
        String dataId = "leiwen.test.SSF4";
        String group = "test";
        String content = "SSF4 is my favourite ftg game";
        
        String aggrDataId = "leiwen.test.XBOX360";
        String aggrGroup = "test";
        
        File file = new File(path + "/config-data/test/leiwen.test.SSF4");
        File aggrFile = new File(path + "/config-data/test/leiwen.test.XBOX360");
        try {
            mockServletContext(dataId, aggrDataId, group);
            this.configService.addConfigInfo(dataId, group, content);
            this.aggrService.aggregation(dataId, group);
            
            ConfigInfo aggrConfigInfo = this.configService.findConfigInfo(aggrDataId, aggrGroup);
            String aggrContent = aggrConfigInfo.getContent();
            
            Assert.assertTrue(aggrContent.contains(content));
            
            this.configService.removeConfigInfo(dataId, group);
            this.aggrService.aggregation(dataId, group);
            
            Assert.assertEquals(null, this.configService.findConfigInfo(aggrDataId, aggrGroup));
        }
        finally {
            file.delete();
            aggrFile.delete();
        }
        
    }
    
    
    @Test
    public void test_不需要聚合() {
        String dataId = "leiwen.test.ZERO";
        String group = "test";
        
        Assert.assertFalse(this.aggrService.aggregation(dataId, group));
    }
    
    
    private void truncateTable() {
        ((DBPersistService)persistService).getJdbcTemplate().update("delete from config_info");
        ((DBPersistService)persistService).getJdbcTemplate().update("delete from group_info");
    }
    
    private void mockServletContext(String dataId, String aggrDataId, String group) {
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR))
            .andReturn(path + "/" + Constants.BASE_DIR).anyTimes();
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group)).andReturn(
            path + "/" + Constants.BASE_DIR + "/" + group).anyTimes();
        String dataPath = path + "/" + Constants.BASE_DIR + "/" + group + "/" + dataId;
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group + "/" + dataId)).andReturn(
            dataPath).anyTimes();
        String aggrDataPath = path + "/" + Constants.BASE_DIR + "/" + group + "/" + aggrDataId;
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group + "/" + aggrDataId)).andReturn(
            aggrDataPath).anyTimes();
        mocksControl.replay();
    }
    
}
