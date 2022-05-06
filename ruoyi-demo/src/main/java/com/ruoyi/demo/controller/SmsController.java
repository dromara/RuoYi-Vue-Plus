package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.R;
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

    private final SmsTemplate smsTemplate;

    @ApiOperation("发送短信Aliyun")
    @GetMapping("/sendAliyun")
    public R<Object> sendSimpleMessage(@ApiParam("电话号") String phones,
                                     @ApiParam("模板ID") String templateId) {
        Map<String, String> map = new HashMap<>(1);
        map.put("code", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

    @ApiOperation("发送短信Tencent")
    @GetMapping("/sendTencent")
    public R<Object> sendMessageWithAttachment(@ApiParam("电话号") String phones,
                                             @ApiParam("模板ID") String templateId) {
        Map<String, String> map = new HashMap<>(1);
//        map.put("2", "测试测试");
        map.put("1", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

}
