package com.taobao.diamond.sdkapi.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.BatchContextResult;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.ContextResult;
import com.taobao.diamond.domain.DiamondConf;
import com.taobao.diamond.domain.DiamondSDKConf;
import com.taobao.diamond.domain.PageContextResult;
import com.taobao.diamond.sdkapi.DiamondSDKManager;


public class DiamondSDKManagerTest {

    private static final String GROUP = "sdk-test";

    private DiamondSDKManager diamondSDKManager;


    @Before
    public void setUp() throws Exception {
        List<DiamondConf> testDiamondConfs = new ArrayList<DiamondConf>();
        // diamond server address, port, username, password
        DiamondConf diamondConf = new DiamondConf("", "", "", "");
        testDiamondConfs.add(diamondConf);

        // 无效的地址
        List<DiamondConf> invalidDiamondConfs = new ArrayList<DiamondConf>();
        //diamond server address, port, username, password
        DiamondConf invalidConf = new DiamondConf("", "", "", "");
        invalidDiamondConfs.add(invalidConf);

        DiamondSDKConf testDiamondConf = new DiamondSDKConf(testDiamondConfs);
        testDiamondConf.setServerId("test");

        DiamondSDKConf invalidDiamondConf = new DiamondSDKConf(invalidDiamondConfs);
        invalidDiamondConf.setServerId("xtest");

        // TreeMap按照key的自然顺序排序, test排前xtest之前, 使得默认的server id为test
        Map<String, DiamondSDKConf> diamondSDKConfMaps = new TreeMap<String, DiamondSDKConf>();
        diamondSDKConfMaps.put("test", testDiamondConf);
        diamondSDKConfMaps.put("xtest", invalidDiamondConf);

        DiamondSDKManagerImpl diamondSDKManagerImpl = new DiamondSDKManagerImpl(2000, 2000);
        diamondSDKManagerImpl.setDiamondSDKConfMaps(diamondSDKConfMaps);

        diamondSDKManager = diamondSDKManagerImpl;
    }


    @Test
    public void test_精确查询_指定server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk query, use specific server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_精确查询_默认server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk query, use default server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryFromDefaultServerByDataIdAndGroupName(dataId, GROUP);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_精确查询_数据不存在() throws Exception {
        String dataId = UUID.randomUUID().toString();

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
        Assert.assertEquals(null, result.getReceiveResult());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());
    }


    @Test
    public void test_精确查询_失败() throws Exception {
        String dataId = UUID.randomUUID().toString();

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "xtest");
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
        Assert.assertEquals(null, result.getReceiveResult());
        Assert.assertEquals("登录失败,造成错误的原因可能是指定的serverId为空或不存在", result.getStatusMsg());
        Assert.assertEquals(0, result.getStatusCode());
    }


    @Test
    public void test_模糊查询_指定server() throws Exception {
        String dataId = "test" + UUID.randomUUID().toString();
        String content = "test sdk vague query, use specific server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        // 按dataId模糊查询
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy("test*", GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        // 按group模糊查询
        result = diamondSDKManager.queryBy(dataId, "sdk*", "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        // 按内容模糊查询
        result = diamondSDKManager.queryBy("*", GROUP, "vague", "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getDiamondData().get(0).getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_模糊查询_默认server() throws Exception {
        String dataId = "asdf" + UUID.randomUUID().toString();
        String content = "test sdk mohu query, use default server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        PageContextResult<ConfigInfo> result =
                diamondSDKManager.queryFromDefaultServerBy("asdf*", GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getDiamondData().get(0).getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_模糊查询_数据不存在() throws Exception {
        // 按dataId模糊查询
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy("abcd*", GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(0, result.getDiamondData().size());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(0, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(0, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());
    }


    @Test
    public void test_模糊查询_失败() throws Exception {
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy("abcd*", GROUP, "xtest", 1, 10);
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(null, result.getDiamondData());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(0, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("登录失败,造成错误的原因可能是指定的serverId为空或不存在", result.getStatusMsg());
        Assert.assertEquals(0, result.getStatusCode());
    }


    @Test
    public void test_模糊查询转换为精确查询() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk query, mohu to jingque";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        // 模糊查询, 但没有*
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy(dataId, GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("指定diamond的查询完成", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getDiamondData().get(0).getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_发布数据_指定server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk publish, use specific server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_发布数据_默认server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk publish, use default server";

        diamondSDKManager.pulishFromDefaultServer(dataId, GROUP, content);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublishFromDefaultServer(id);
    }


    @Test
    public void test_更新数据_指定server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk update, use specific server";
        String newContent = "test sdk update, use specific server: new";

        // 发布
        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // 更新
        diamondSDKManager.pulishAfterModified(dataId, GROUP, newContent, "test");

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_更新数据_默认server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk update, use default server";
        String newContent = "test sdk update, use default server: new";

        // 发布
        diamondSDKManager.pulishFromDefaultServer(dataId, GROUP, content);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // 更新
        diamondSDKManager.pulishFromDefaultServerAfterModified(dataId, GROUP, newContent);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublishFromDefaultServer(id);
    }


    @Test
    public void test_连续发布两次_第二次为更新() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk twice publish";
        String newContent = "test sdk twice publish: new";

        // 第一次发布
        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // 第二次发布
        diamondSDKManager.pulish(dataId, GROUP, newContent, "test");

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_删除数据_指定server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk unpublish, use specific server";

        // 发布
        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // 删除
        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
    }


    @Test
    public void test_删除数据_默认server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk unpublish, use default server";

        // 发布
        diamondSDKManager.pulishFromDefaultServer(dataId, GROUP, content);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // 删除
        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublishFromDefaultServer(id);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
    }


    @Test
    public void test_数据是否存在() throws Exception {
        // 存在
        String dataId = UUID.randomUUID().toString();
        diamondSDKManager.pulish(dataId, GROUP, "aaa", "test");
        Assert.assertTrue(diamondSDKManager.exists(dataId, GROUP, "test"));

        // 不存在
        dataId = UUID.randomUUID().toString();
        Assert.assertFalse(diamondSDKManager.exists(dataId, GROUP, "test"));
    }


    // ==================== 批量接口测试 =================== //

    @Test
    public void test_批量写_全部是新增() {
        // 构造dataId和content的map
        String baseDataId = UUID.randomUUID().toString() + "-sdkBatchWrite-";
        String baseContent = UUID.randomUUID().toString() + "-allAdd-";
        Map<String, String> dataId2ContentMap = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            String dataId = baseDataId + i;
            String content = baseContent + i;
            dataId2ContentMap.put(dataId, content);
        }

        // 批量写
        BatchContextResult<ConfigInfoEx> response =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", dataId2ContentMap);

        // 验证结果
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(200, response.getStatusCode());
        List<ConfigInfoEx> resultList = response.getResult();
        Assert.assertEquals(5, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(dataId2ContentMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(dataId2ContentMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断状态码是否为新增成功
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }
    }


    @Test
    public void test_批量写_全部是更新() {
        // 构造dataId和content的map
        String baseDataId = UUID.randomUUID().toString() + "-sdkBatchWrite-";
        String baseContent = UUID.randomUUID().toString() + "-allUpdate-";
        Map<String, String> dataId2ContentMap = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            String dataId = baseDataId + i;
            String content = baseContent + i;
            dataId2ContentMap.put(dataId, content);
        }

        // 先批量写一次, 本次写为新增
        BatchContextResult<ConfigInfoEx> response =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", dataId2ContentMap);

        // 验证结果
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(200, response.getStatusCode());
        List<ConfigInfoEx> resultList = response.getResult();
        Assert.assertEquals(5, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(dataId2ContentMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(dataId2ContentMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增是否成功
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // 再批量写一次, 本次写为更新
        response = this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", dataId2ContentMap);

        // 验证结果
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(200, response.getStatusCode());
        resultList = response.getResult();
        Assert.assertEquals(5, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(dataId2ContentMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(dataId2ContentMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增是否成功
            Assert.assertEquals(Constants.BATCH_UPDATE_SUCCESS, configInfoEx.getStatus());
        }
    }


    @Test
    public void test_批量写_部分新增部分更新() {
        // 构造dataId和content的map, map1是第一次新增的数据, map2是第二次新增的数据
        String baseDataId1 = UUID.randomUUID().toString() + "-batchWriteFirst-";
        String baseContent1 = UUID.randomUUID().toString() + "-batchWriteFirstContent-";
        String baseDataId2 = UUID.randomUUID().toString() + "-batchWriteSecond-";
        String baseContent2 = UUID.randomUUID().toString() + "-batchWriteSecondContent-";

        Map<String, String> firstMap = new HashMap<String, String>();
        for (int i = 0; i < 3; i++) {
            firstMap.put(baseDataId1 + i, baseContent1 + i);
        }

        Map<String, String> secondMap = new HashMap<String, String>();
        for (int i = 0; i < 2; i++) {
            secondMap.put(baseDataId2 + i, baseContent2 + i);
        }
        secondMap.putAll(firstMap);

        // 第一次批量写, 新增3条数据
        BatchContextResult<ConfigInfoEx> firstResponse =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", firstMap);
        // 验证第一次批量写的结果
        Assert.assertTrue(firstResponse.isSuccess());
        Assert.assertEquals(200, firstResponse.getStatusCode());
        List<ConfigInfoEx> firstResultList = firstResponse.getResult();
        Assert.assertEquals(3, firstResultList.size());

        for (ConfigInfoEx configInfoEx : firstResultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(firstMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(firstMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增是否成功
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // 第二次批量写, 更新3条数据, 新增2条数据
        BatchContextResult<ConfigInfoEx> secondResponse =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", secondMap);
        // 验证第二次批量写的结果
        Assert.assertTrue(secondResponse.isSuccess());
        Assert.assertEquals(200, secondResponse.getStatusCode());
        List<ConfigInfoEx> secondResultList = secondResponse.getResult();
        Assert.assertEquals(5, secondResultList.size());

        for (ConfigInfoEx configInfoEx : secondResultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(secondMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(secondMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增或是更新
            if (recvDataId.startsWith(baseDataId1)) {
                Assert.assertEquals(Constants.BATCH_UPDATE_SUCCESS, configInfoEx.getStatus());
            }
            else if (recvDataId.startsWith(baseDataId2)) {
                Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
            }
            else {
                Assert.fail("出现了未知的dataId");
            }
        }
    }


    /**
     * 批量查询, 不再分全部成功, 全部失败场景
     */
    @Test
    public void test_批量查询() {
        String baseDataId1 = UUID.randomUUID().toString() + "-batchUpdateFirst-";
        String baseContent1 = UUID.randomUUID().toString() + "-batchUpdateFirstContent-";
        String baseDataId2 = UUID.randomUUID().toString() + "-batchUpdateSecond-";
        String baseContent2 = UUID.randomUUID().toString() + "-batchUpdateSecondContent-";

        Map<String, String> firstMap = new HashMap<String, String>();
        List<String> firstList = new LinkedList<String>();
        for (int i = 0; i < 3; i++) {
            firstMap.put(baseDataId1 + i, baseContent1 + i);
            firstList.add(baseDataId1 + i);
        }

        Map<String, String> secondMap = new HashMap<String, String>();
        List<String> secondList = new LinkedList<String>();
        for (int i = 0; i < 2; i++) {
            secondMap.put(baseDataId2 + i, baseContent2 + i);
            secondList.add(baseDataId2 + i);
        }

        // 批量新增
        BatchContextResult<ConfigInfoEx> addResult =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", firstMap);
        // 验证结果
        Assert.assertTrue(addResult.isSuccess());
        Assert.assertEquals(200, addResult.getStatusCode());
        List<ConfigInfoEx> resultList = addResult.getResult();
        Assert.assertEquals(3, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(firstMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(firstMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增是否成功
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }

        // 批量查询, 成功
        BatchContextResult<ConfigInfoEx> queryResult = this.diamondSDKManager.batchQuery("test", GROUP, firstList);
        // 验证结果
        Assert.assertTrue(queryResult.isSuccess());
        Assert.assertEquals(200, queryResult.getStatusCode());
        List<ConfigInfoEx> queryResultList = queryResult.getResult();
        Assert.assertEquals(3, queryResultList.size());

        for (ConfigInfoEx configInfoEx : queryResultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(firstMap.containsKey(recvDataId));
            // 判断收到的内容是否是该dataId对应的内容
            Assert.assertEquals(firstMap.get(recvDataId), configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增是否成功
            Assert.assertEquals(Constants.BATCH_QUERY_EXISTS, configInfoEx.getStatus());
        }

        // 批量查询, 失败
        queryResult = this.diamondSDKManager.batchQuery("test", GROUP, secondList);
        // 验证结果
        Assert.assertTrue(queryResult.isSuccess());
        Assert.assertEquals(200, queryResult.getStatusCode());
        queryResultList = queryResult.getResult();
        Assert.assertEquals(2, queryResultList.size());

        for (ConfigInfoEx configInfoEx : queryResultList) {
            String recvDataId = configInfoEx.getDataId();
            // 判断收到的dataId是否在dataId2ContentMap中
            Assert.assertTrue(secondMap.containsKey(recvDataId));
            // 查询失败内容为空
            Assert.assertNull(configInfoEx.getContent());
            // 判断group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // 判断新增是否成功
            Assert.assertEquals(Constants.BATCH_QUERY_NONEXISTS, configInfoEx.getStatus());
        }

    }


    @Test
    public void test_增加源IP和源用户信息() {
        String dataId = UUID.randomUUID().toString();
        String content = "test add src ip and src user";
        String serverId = "test";
        String srcIp = "xxx";
        String srcUser = "xxx";

        diamondSDKManager.publish(dataId, GROUP, content, serverId, srcIp, srcUser);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, serverId);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_更新源IP和源用户信息() {
        String dataId = UUID.randomUUID().toString();
        String content = "test update src ip and src user";
        String serverId = "test";
        String srcIp = "xxx";
        String srcUser = "xxx";

        // 新增
        diamondSDKManager.publish(dataId, GROUP, content, serverId, srcIp, srcUser);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, serverId);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals(200, result.getStatusCode());

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // 更新
        String newContent = "test update src ip and src user =====";
        String newSrcUser = "xxx";
        diamondSDKManager.publishAfterModified(dataId, GROUP, newContent, serverId, srcIp, newSrcUser);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, serverId);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());
        Assert.assertEquals(newContent, result.getReceiveResult());
        Assert.assertEquals(200, result.getStatusCode());
    }

}
