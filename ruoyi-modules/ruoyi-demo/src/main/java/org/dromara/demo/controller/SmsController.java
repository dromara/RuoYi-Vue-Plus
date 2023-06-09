package org.dromara.demo.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.sms.config.properties.SmsProperties;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.provider.enumerate.SupplierType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

/**
 * 短信演示案例
 * 请先阅读文档 否则无法使用
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/sms")
public class SmsController {

    private final SmsProperties smsProperties;
//    private final SmsTemplate smsTemplate; // 可以使用spring注入
//    private final AliyunSmsTemplate smsTemplate; // 也可以注入某个厂家的模板工具

    /**
     * 发送短信Aliyun
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @GetMapping("/sendAliyun")
    public R<Object> sendAliyun(String phones, String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("aliyunSmsTemplate")) {
            return R.fail("阿里云依赖未引入！");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("code", "1234");
        SmsResponse smsResponse = SmsFactory.createSmsBlend(SupplierType.ALIBABA).sendMessage(phones, templateId, map);
        return R.ok(smsResponse);
    }

    /**
     * 发送短信Tencent
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @GetMapping("/sendTencent")
    public R<Object> sendTencent(String phones, String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("tencentSmsTemplate")) {
            return R.fail("腾讯云依赖未引入！");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
//        map.put("2", "测试测试");
        map.put("1", "1234");
        SmsResponse smsResponse = SmsFactory.createSmsBlend(SupplierType.TENCENT).sendMessage(phones, templateId, map);
        return R.ok(smsResponse);
    }

}
