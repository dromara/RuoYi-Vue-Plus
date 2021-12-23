package com.ruoyi.demo.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.core.validate.QueryGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.excel.ExcelResult;
import com.ruoyi.common.utils.ValidatorUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.domain.TestSensitive;
import com.ruoyi.demo.domain.bo.TestDemoBo;
import com.ruoyi.demo.domain.bo.TestDemoImportVo;
import com.ruoyi.demo.domain.vo.TestDemoVo;
import com.ruoyi.demo.service.ITestDemoService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 测试单表Controller
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Validated
@Api(value = "测试数据脱敏控制器", tags = {"测试数据脱敏管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/sensitive")
public class TestSensitiveController extends BaseController {

    //默认为admin用户及拥有Sensitive权限字符用户不做脱敏
    //1.配置菜单加入权限字符为Sensitive的按钮
    //2.配置需要免除数据脱敏的角色加入Sensitive权限
    //3.实体类上加上数据脱敏注解
    /**
     * 测试数据脱敏
     */
    @ApiOperation("查询测试单表列表")
    @GetMapping()
    public AjaxResult<TestSensitive> get() {
        TestSensitive testSensitive = new TestSensitive()
            .setIdCard("3333199910101212")
            .setPhone("18888888888")
            .setAddress("北京市朝阳区某某四合院1203室");
        return AjaxResult.success(testSensitive);
    }

}
