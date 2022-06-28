package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.sms.config.properties.SmsProperties;
import com.ruoyi.sms.core.SmsTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 短信演示案例
 * 请先阅读文档 否则无法使用
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Validated
@Api(value = "短信演示案例", tags = {"短信演示案例"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/sms")
public class SmsController {

    private final SmsProperties smsProperties;
//    private final SmsTemplate smsTemplate; // 可以使用spring注入
//    private final AliyunSmsTemplate smsTemplate; // 也可以注入某个厂家的模板工具

    @ApiOperation("发送短信Aliyun")
    @GetMapping("/sendAliyun")
    public R<Object> sendAliyun(@ApiParam("电话号") String phones,
                                     @ApiParam("模板ID") String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("aliyunSmsTemplate")) {
            return R.fail("阿里云依赖未引入！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        Map<String, String> map = new HashMap<>(1);
        map.put("code", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

    @ApiOperation("发送短信Tencent")
    @GetMapping("/sendTencent")
    public R<Object> sendTencent(@ApiParam("电话号") String phones,
                                             @ApiParam("模板ID") String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("tencentSmsTemplate")) {
            return R.fail("腾讯云依赖未引入！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        Map<String, String> map = new HashMap<>(1);
//        map.put("2", "测试测试");
        map.put("1", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

}
