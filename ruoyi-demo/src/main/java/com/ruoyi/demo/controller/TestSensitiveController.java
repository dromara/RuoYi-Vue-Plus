package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.SensitiveStrategy;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试数据脱敏控制器
 *
 * 默认管理员不过滤
 * 需自行根据业务重写实现
 *
 * @see com.ruoyi.common.core.service.SensitiveService
 * @author Lion Li
 * @version 3.6.0
 */
@Api(value = "测试数据脱敏控制器", tags = {"测试数据脱敏管理"})
@RestController
@RequestMapping("/demo/sensitive")
public class TestSensitiveController extends BaseController {

    /**
     * 测试数据脱敏
     */
    @ApiOperation("查询测试单表列表")
    @GetMapping("/test")
    public AjaxResult<TestSensitive> test() {
        TestSensitive testSensitive = new TestSensitive()
            .setIdCard("210397198608215431")
            .setPhone("17640125371")
            .setAddress("北京市朝阳区某某四合院1203室")
            .setEmail("17640125371@163.com")
            .setBankCard("6226456952351452853");
        return AjaxResult.success(testSensitive);
    }

    @Data
    @Accessors(chain = true)
    static class TestSensitive {

        /**
         * 身份证
         */
        @Sensitive(strategy = SensitiveStrategy.ID_CARD)
        private String idCard;

        /**
         * 电话
         */
        @Sensitive(strategy = SensitiveStrategy.PHONE)
        private String phone;

        /**
         * 地址
         */
        @Sensitive(strategy = SensitiveStrategy.ADDRESS)
        private String address;

        /**
         * 邮箱
         */
        @Sensitive(strategy = SensitiveStrategy.EMAIL)
        private String email;

        /**
         * 银行卡
         */
        @Sensitive(strategy = SensitiveStrategy.BANK_CARD)
        private String bankCard;

    }

}
