/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.controller;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.server.service.AggregationService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.PushitService;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.ConfigInfoTask;
import com.taobao.diamond.server.service.task.RealTimeNotifyTask;
import com.taobao.diamond.server.service.task.RedisTask;
import com.taobao.diamond.server.service.task.UpdateAllConfigInfoTask;
import com.taobao.diamond.server.service.task.processor.RealTimeNotifyTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RedisTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RemoveConfigInfoTaskProcessor;
import com.taobao.diamond.server.service.task.processor.UpdateAllConfigInfoTaskProcessor;
import com.taobao.diamond.server.service.task.processor.UpdateConfigInfoTaskProcessor;
import com.taobao.diamond.server.utils.DiamondUtils;


/**
 * 软负载客户端发布数据专用控制器
 * 
 * @author leiwen
 * 
 */
@Controller
@RequestMapping("/basestone.do")
public class BaseStoneController {
    private static final Log log = LogFactory.getLog(BaseStoneController.class);

    // private static final int UPDATE_FAILED = 535;
    private static final int INVALID_PARAM = 536;

    @Autowired
    private ConfigService configService;

    @Autowired
    private PushitService pushitService;

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private TaskManagerService taskManagerService;

    @Autowired
    private UpdateConfigInfoTaskProcessor updateConfigInfoTaskProcessor;

    @Autowired
    private RemoveConfigInfoTaskProcessor removeConfigInfoTaskProcessor;

    @Autowired
    private UpdateAllConfigInfoTaskProcessor updateAllConfigInfoTaskProcessor;

    @Autowired
    private RedisTaskProcessor redisTaskProcessor;

    @Autowired
    private RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor;

    static AtomicInteger id = new AtomicInteger(0);

    static String idPrefix = UUID.randomUUID().toString();


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }


    public TaskManagerService getTaskManagerService() {
        return taskManagerService;
    }


    public void setTaskManagerService(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }


    public PushitService getPushitService() {
        return pushitService;
    }


    public void setPushitService(PushitService pushitService) {
        this.pushitService = pushitService;
    }


    public AggregationService getAggregationService() {
        return aggregationService;
    }


    public void setAggregationService(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }


    public UpdateConfigInfoTaskProcessor getUpdateConfigInfoTaskProcessor() {
        return updateConfigInfoTaskProcessor;
    }


    public void setUpdateConfigInfoTaskProcessor(UpdateConfigInfoTaskProcessor updateConfigInfoTaskProcessor) {
        this.updateConfigInfoTaskProcessor = updateConfigInfoTaskProcessor;
    }


    public RemoveConfigInfoTaskProcessor getRemoveConfigInfoTaskProcessor() {
        return removeConfigInfoTaskProcessor;
    }


    public void setRemoveConfigInfoTaskProcessor(RemoveConfigInfoTaskProcessor removeConfigInfoTaskProcessor) {
        this.removeConfigInfoTaskProcessor = removeConfigInfoTaskProcessor;
    }


    public UpdateAllConfigInfoTaskProcessor getUpdateAllConfigInfoTaskProcessor() {
        return updateAllConfigInfoTaskProcessor;
    }


    public void setUpdateAllConfigInfoTaskProcessor(UpdateAllConfigInfoTaskProcessor updateAllConfigInfoTaskProcessor) {
        this.updateAllConfigInfoTaskProcessor = updateAllConfigInfoTaskProcessor;
    }


    public RedisTaskProcessor getRedisTaskProcessor() {
        return redisTaskProcessor;
    }


    public void setRedisTaskProcessor(RedisTaskProcessor redisTaskProcessor) {
        this.redisTaskProcessor = redisTaskProcessor;
    }


    public RealTimeNotifyTaskProcessor getRealTimeNotifyTaskProcessor() {
        return realTimeNotifyTaskProcessor;
    }


    public void setRealTimeNotifyTaskProcessor(RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor) {
        this.realTimeNotifyTaskProcessor = realTimeNotifyTaskProcessor;
    }


    @RequestMapping(params = "method=postConfig", method = RequestMethod.POST)
    public String postConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content) {
        response.setCharacterEncoding("GBK");

        String remoteIp = getRemoteIp(request);

        boolean checkSuccess = true;
        String errorMessage = "参数错误";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "无效的DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "无效的分组";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "无效的内容";
        }
        if (!checkSuccess) {
            try {
                response.sendError(INVALID_PARAM, errorMessage);
            }
            catch (Exception e) {
                log.error("发送response信息出错:" + e.getMessage(), e);
            }
            return "536";
        }

        // 同步新增
        this.configService.addConfigInfo(dataId, group, content);
        try {
            // 进行数据聚合
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("数据聚合出错", e);
        }
        try {
            // 实时通知
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("实时通知出错", e);
        }
        try {
            // 增加ip与发布dataId的映射到redis
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis出错", e);
        }
        return "200";
    }


    @RequestMapping(params = "method=updateConfig", method = RequestMethod.POST)
    public String updateConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content) {
        response.setCharacterEncoding("GBK");

        String remoteIp = getRemoteIp(request);

        boolean checkSuccess = true;
        String errorMessage = "参数错误";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "无效的DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "无效的分组";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "无效的内容";
        }
        if (!checkSuccess) {
            try {
                response.sendError(INVALID_PARAM, errorMessage);
            }
            catch (IOException e) {
                log.error("发送response信息出错", e);
            }
            return "536";
        }

        // 异步更新
        this.updateConfigInfo(dataId, group, content);

        try {
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch(Exception e) {
            log.error("redis出错", e);
        }
        return "200";
    }


    @RequestMapping(params = "method=deleteConfig", method = RequestMethod.POST)
    public String deleteConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content) {
        response.setCharacterEncoding("GBK");

        boolean checkSuccess = true;
        String errorMessage = "参数错误";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "无效的DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "无效的分组";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "无效的内容";
        }
        if (!checkSuccess) {
            try {
                response.sendError(INVALID_PARAM, errorMessage);
            }
            catch (IOException e) {
                log.error("发送response信息出错", e);
            }
            return "536";
        }

        this.removeConfigInfo(dataId, group, content);

        return "200";
    }


    @RequestMapping(params = "method=updateAll", method = RequestMethod.POST)
    public String updateConfigAll(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content) {
        response.setCharacterEncoding("GBK");

        String remoteIp = getRemoteIp(request);

        boolean checkSuccess = true;
        String errorMessage = "参数错误";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "无效的DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "无效的分组";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "无效的内容";
        }
        if (!checkSuccess) {
            try {
                response.sendError(INVALID_PARAM, errorMessage);
            }
            catch (IOException e) {
                log.error("发送response信息出错", e);
            }
            return "536";
        }

        // 异步更新
        this.updateAllConfigInfo(dataId, group, content);
        try {
            // 增加ip与发布dataId的映射到redis
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch(Exception e) {
            log.error("redis出错", e);
        }
        return "200";
    }


    private void updateConfigInfo(String dataId, String group, String content) {
        String taskType = dataId + "-" + group + "-update";
        ConfigInfoTask updateConfigInfoTask = new ConfigInfoTask(dataId, group, content);
        this.taskManagerService.addUpdateProcessor(taskType, updateConfigInfoTaskProcessor);
        this.taskManagerService.addUpdateTask(taskType, updateConfigInfoTask, true);
    }


    private void removeConfigInfo(String dataId, String group, String content) {
        String taskType = dataId + "-" + group + "-rm";
        ConfigInfoTask rmTask = new ConfigInfoTask(dataId, group, content);
        this.taskManagerService.addRemoveProcessor(taskType, removeConfigInfoTaskProcessor);
        this.taskManagerService.addRemoveTask(taskType, rmTask, true);
    }


    private void updateAllConfigInfo(String dataId, String group, String content) {
        String taskType = dataId + "-" + group + "-updateAll";
        UpdateAllConfigInfoTask task = new UpdateAllConfigInfoTask(dataId, group, content);
        this.taskManagerService.addUpdateAllProcessor(taskType, updateAllConfigInfoTaskProcessor);
        this.taskManagerService.addUpdateAllTask(taskType, task, false);
    }


    private void addIpToDataIdAndGroup(String remoteIp, String dataId, String group) {
        String taskType = remoteIp + "-redis";
        RedisTask task = new RedisTask(remoteIp, dataId + Constants.WORD_SEPARATOR + group);
        this.taskManagerService.addRedisProcessor(taskType, redisTaskProcessor);
        this.taskManagerService.addRedisTask(taskType, task, false);
    }


    private void realTimeNotify(String dataId, String group) {
        String taskType = dataId + "-" + group + "-pushit";
        RealTimeNotifyTask task = new RealTimeNotifyTask(dataId, group);
        task.setLastProcessTime(System.currentTimeMillis());
        task.setTaskInterval(2000L);
        this.taskManagerService.addPushitProcessor(taskType, realTimeNotifyTaskProcessor);
        this.taskManagerService.addPushitTask(taskType, task, false);
    }


    private String getRemoteIp(HttpServletRequest request) {
        String remoteIp = request.getHeader("X-Real-IP");
        if (remoteIp == null || remoteIp.isEmpty()) {
            remoteIp = request.getRemoteAddr();
        }
        return remoteIp;
    }

}
