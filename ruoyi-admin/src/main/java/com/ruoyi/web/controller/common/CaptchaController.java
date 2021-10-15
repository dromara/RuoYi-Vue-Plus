package com.ruoyi.web.controller.common;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.CaptchaType;
import com.ruoyi.common.utils.RedisUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.reflect.ReflectUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.config.properties.CaptchaProperties;
import com.ruoyi.system.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码操作处理
 *
 * @author Lion Li
 */
@Api(value = "验证码操作处理", tags = {"验证码管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
public class CaptchaController {

    private final CaptchaProperties captchaProperties;
    private final ISysConfigService configService;

    /**
     * 生成验证码
     */
    @ApiOperation("生成验证码")
    @GetMapping("/captchaImage")
    public AjaxResult<Map<String, Object>> getCode() {
        Map<String, Object> ajax = new HashMap<>();
        boolean captchaOnOff = configService.selectCaptchaOnOff();
        ajax.put("captchaOnOff", captchaOnOff);
        if (!captchaOnOff) {
            return AjaxResult.success(ajax);
        }
        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        // 生成验证码
        CaptchaType captchaType = captchaProperties.getType();
        boolean isMath = CaptchaType.MATH == captchaType;
        Integer length = isMath ? captchaProperties.getNumberLength() : captchaProperties.getCharLength();
        CodeGenerator codeGenerator = ReflectUtils.newInstance(captchaType.getClazz(), length);
        AbstractCaptcha captcha = SpringUtils.getBean(captchaProperties.getCategory().getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        String code = isMath ? getCodeResult(captcha.getCode()) : captcha.getCode();
        RedisUtils.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        ajax.put("uuid", uuid);
        ajax.put("img", captcha.getImageBase64());
        return AjaxResult.success(ajax);
    }

    private String getCodeResult(String capStr) {
        int numberLength = captchaProperties.getNumberLength();
        int a = Convert.toInt(StringUtils.substring(capStr, 0, numberLength).trim());
        char operator = capStr.charAt(numberLength);
        int b = Convert.toInt(StringUtils.substring(capStr, numberLength + 1, numberLength + 1 + numberLength).trim());
        switch (operator) {
            case '*':
                return Convert.toStr(a * b);
            case '+':
                return Convert.toStr(a + b);
            case '-':
                return Convert.toStr(a - b);
            default:
                return StringUtils.EMPTY;
        }
    }

}
