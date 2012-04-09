package com.taobao.diamond.server.controller;

import java.io.File;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Ignore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.server.service.ACLService;
import com.taobao.diamond.server.service.AdminService;
import com.taobao.diamond.server.service.AggregationService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.DBPersistService;
import com.taobao.diamond.server.service.DiskService;
import com.taobao.diamond.server.service.FlowControlService;
import com.taobao.diamond.server.service.GroupService;
import com.taobao.diamond.server.service.NotifyService;
import com.taobao.diamond.server.service.PersistService;
import com.taobao.diamond.server.service.PushitService;
import com.taobao.diamond.server.service.RedisService;
import com.taobao.diamond.server.service.ValidationService;


@Ignore
public class AbstractControllerUnitTest {

    protected GroupService groupService;
    protected DiskService diskService;
    protected ConfigService configService;
    protected PersistService persistService;
    
    protected NotifyService notifyService;
    protected ServletContext servletContext;
    protected IMocksControl mocksControl = EasyMock.createControl();
    protected File tempFile;
    protected String path;
    protected AdminService adminService;
    protected FlowControlService flowControlService;
    protected RedisService redisService;
    protected ACLService aCLService;
    protected PushitService pushitService;
    protected AggregationService aggregationService;

    public void setUp() throws Exception {
        configService = new ConfigService();

        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"application.xml"});

        persistService = (PersistService) ctx.getBean("persistService");
        redisService = (RedisService)ctx.getBean("redisService");
        pushitService = (PushitService)ctx.getBean("pushitService");
        aggregationService = new AggregationService();
        this.diskService = new DiskService();
        this.configService.setPersistService(persistService);
        this.configService.setDiskService(diskService);
        this.configService.setRedisService(redisService);
        notifyService = new NotifyService();
        this.configService.setNotifyService(notifyService);
        this.configService.setValidationService(new ValidationService());
        groupService = new GroupService();
        groupService.setDiskService(diskService);
        groupService.setNotifyService(notifyService);
        groupService.setPersistService(persistService);
        this.configService.setGroupService(groupService);
        mocksControl = EasyMock.createControl();
        servletContext = mocksControl.createMock(ServletContext.class);
        // 必须设置该mock代理是线程安全的, 否则不同的线程调用同一个mock会报错 by leiwen
        EasyMock.makeThreadSafe(servletContext, true);
        this.diskService.setServletContext(servletContext);
        tempFile = File.createTempFile("ConfigServiceUnitTest", "tmp");
        path = tempFile.getParent();
        this.adminService=new AdminService();
        aCLService =  (ACLService) ctx.getBean("aCLService");
        flowControlService = (FlowControlService) ctx.getBean("flowControlService");
        truncateTable();

    }


    public void mockServletContext(String dataId, String group, String content) {
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR))
            .andReturn(path + "/" + Constants.BASE_DIR).anyTimes();
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group)).andReturn(
            path + "/" + Constants.BASE_DIR + "/" + group).anyTimes();
        String dataPath = path + "/" + Constants.BASE_DIR + "/" + group + "/" + dataId;
        EasyMock.expect(servletContext.getRealPath("/" + Constants.BASE_DIR + "/" + group + "/" + dataId)).andReturn(
            dataPath).anyTimes();
        mocksControl.replay();
    }


    void truncateTable() {
    	DBPersistService dps = (DBPersistService)persistService;
    	dps.getJdbcTemplate().update("delete from config_info");
    	dps.getJdbcTemplate().update("delete from group_info");
    }

}
