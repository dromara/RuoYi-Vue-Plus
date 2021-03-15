package com.ruoyi.web.controller.common;

import java.util.concurrent.TimeUnit;

import cn.hutool.captcha.*;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;

import javax.annotation.Resource;

/**
 * 验证码操作处理
 *
 * @author Lion Li
 */
@RestController
public class CaptchaController {

    // 圆圈干扰验证码
    @Resource(name = "CircleCaptcha")
    private CircleCaptcha circleCaptcha;
    // 线段干扰的验证码
    @Resource(name = "LineCaptcha")
    private LineCaptcha lineCaptcha;
    // 扭曲干扰验证码
    @Resource(name = "ShearCaptcha")
    private ShearCaptcha shearCaptcha;

    @Autowired
    private RedisCache redisCache;

    // 验证码类型
    @Value("${captcha.captchaType}")
    private String captchaType;
    // 验证码类别
    @Value("${captcha.captchaCategory}")
    private String captchaCategory;
    // 数字验证码位数
    @Value("${captcha.captchaNumberLength}")
    private int numberLength;
    // 字符验证码长度
    @Value("${captcha.captchaCharLength}")
    private int charLength;

    /**
     * 生成验证码
     */
    @GetMapping("/captchaImage")
    public AjaxResult getCode() {
        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String capStr = null, code = null;
        // 生成验证码
        CodeGenerator codeGenerator;
        if ("math".equals(captchaType)) {
            codeGenerator = new MathGenerator(numberLength);
        } else if ("char".equals(captchaType)) {
            codeGenerator = new RandomGenerator(charLength);
        } else {
            throw new IllegalArgumentException("验证码类型异常");
        }
        if ("line".equals(captchaCategory)) {
            lineCaptcha.setGenerator(codeGenerator);
            lineCaptcha.createCode();
            capStr = lineCaptcha.getCode();
        } else if ("circle".equals(captchaCategory)) {
            circleCaptcha.setGenerator(codeGenerator);
            circleCaptcha.createCode();
            capStr = circleCaptcha.getCode();
        } else if ("shear".equals(captchaCategory)) {
            shearCaptcha.setGenerator(codeGenerator);
            shearCaptcha.createCode();
            capStr = shearCaptcha.getCode();
        }  else {
            throw new IllegalArgumentException("验证码类别异常");
        }
        if ("math".equals(captchaType)) {
            code = getCodeResult(capStr);
        } else if ("char".equals(captchaType)) {
            code = capStr;
        }

        redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);

        circleCaptcha.createImage(capStr);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("uuid", uuid);
        ajax.put("img", circleCaptcha.getImageBase64());
        return ajax;
    }

    private String getCodeResult(String capStr) {
        int a = Convert.toInt(StrUtil.sub(capStr, 0, numberLength).trim());
        char operator = capStr.charAt(numberLength);
        int b = Convert.toInt(StrUtil.sub(capStr, numberLength + 1, numberLength + 1 + numberLength).trim());
        switch (operator) {
            case '*':
                return a * b + "";
            case '+':
                return a + b + "";
            case '-':
                return a - b + "";
            default:
                return "";
        }
    }

}
