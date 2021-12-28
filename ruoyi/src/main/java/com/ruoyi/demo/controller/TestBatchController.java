package com.ruoyi.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.service.ITestDemoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试批量方法
 *
 * @author Lion Li
 * @date 2021-05-30
 */
@Api(value = "测试批量方法", tags = {"测试批量方法"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/batch")
public class TestBatchController extends BaseController {

    private final ITestDemoService iTestDemoService;

    /**
     * 新增批量方法 可完美替代 saveBatch 秒级插入上万数据 (对mysql负荷较大)
     *
     * 3.5.0 版本 增加 rewriteBatchedStatements=true 批处理参数 使 MP 原生批处理可以达到同样的速度
     */
    @ApiOperation(value = "新增批量方法")
    @PostMapping("/add")
//	@DS("slave")
    public AjaxResult<Void> add() {
        List<TestDemo> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new TestDemo().setOrderNum(-1L).setTestKey("批量新增").setValue("测试新增"));
        }
        return toAjax(iTestDemoService.saveAll(list) ? 1 : 0);
    }

    /**
     * 新增或更新 可完美替代 saveOrUpdateBatch 高性能
     *
     * 3.5.0 版本 增加 rewriteBatchedStatements=true 批处理参数 使 MP 原生批处理可以达到同样的速度
     */
    @ApiOperation(value = "新增或更新批量方法")
    @PostMapping("/addOrUpdate")
//	@DS("slave")
    public AjaxResult<Void> addOrUpdate() {
        List<TestDemo> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new TestDemo().setOrderNum(-1L).setTestKey("批量新增").setValue("测试新增"));
        }
        iTestDemoService.saveAll(list);
        for (int i = 0; i < list.size(); i++) {
            TestDemo testDemo = list.get(i);
            testDemo.setTestKey("批量新增或修改").setValue("批量新增或修改");
            if (i % 2 == 0) {
                testDemo.setId(null);
            }
        }
        return toAjax(iTestDemoService.saveOrUpdateAll(list) ? 1 : 0);
    }

    /**
     * 删除批量方法
     */
    @ApiOperation(value = "删除批量方法")
    @DeleteMapping()
//	@DS("slave")
    public AjaxResult<Void> remove() {
        return toAjax(iTestDemoService.remove(new LambdaQueryWrapper<TestDemo>()
            .eq(TestDemo::getOrderNum, -1L)) ? 1 : 0);
    }

}
