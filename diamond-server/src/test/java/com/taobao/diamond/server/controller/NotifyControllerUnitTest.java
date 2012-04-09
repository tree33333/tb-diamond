package com.taobao.diamond.server.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.GroupInfo;


public class NotifyControllerUnitTest extends AbstractControllerUnitTest {

    private NotifyController notifyController;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.notifyController = new NotifyController();
        this.notifyController.setConfigService(configService);
        this.notifyController.setGroupService(groupService);
    }


    @Test
    public void testNotifyGroup() {
        // 测试流程:直接用persistService插入一个分组信息，不load
        // 然后调用notifyGroup时才reload，查找配置信息是否存在
        String address = "192.168.207.102";
        String dataId = "dataId";
        String group = "test-group";
        this.persistService.addGroupInfo(new GroupInfo(address, dataId, group));
        assertEquals(Constants.DEFAULT_GROUP, this.groupService.getGroupByAddress(address, dataId, null));

        assertEquals("200", this.notifyController.notifyGroup());
        assertEquals("test-group", this.groupService.getGroupByAddress(address, dataId, null));

    }


    @Test
    public void testNotifyGroupInfo() throws Exception {
        // 测试流程：直接通过PersistService插入一条配置信息，不保存到磁盘
        // 调用notifyGroupInfo通知加载配置信息并存入磁盘，检查文件存在并内容正确
        String dataId = "dataId";
        String group = "test-group";
        String content = "diamond server";

        File file = new File(path + "/config-data/test-group/dataId");
        assertFalse(file.exists());

        try {

            this.persistService.addConfigInfo(new ConfigInfo(dataId, group, content));
            file = new File(path + "/config-data/test-group/dataId");
            // 仍然不存在
            assertFalse(file.exists());
            mockServletContext(dataId, group, content);
            assertEquals("200", this.notifyController.notifyConfigInfo(dataId, group));
            file = new File(path + "/config-data/test-group/dataId");
            // 存在
            assertTrue(file.exists());
            // 校验内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();
            assertNotNull(line);
            assertEquals(content, line);
            reader.close();

        }
        finally {
            file.delete();
        }

    }

}
