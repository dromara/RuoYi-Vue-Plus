package com.ruoyi.web.controller.common;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.captcha.UnsignedMathGenerator;
import com.ruoyi.framework.config.properties.CaptchaProperties;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码操作处理
 *
 * @author ruoyi
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

	@Autowired
	private CaptchaProperties captchaProperties;

	@Autowired
	private ISysConfigService configService;

	/**
	 * 生成验证码
	 */
	@GetMapping("/captchaImage")
	public AjaxResult getCode() {
		Map<String, Object> ajax = new HashMap<>();
		boolean captchaOnOff = configService.selectCaptchaOnOff();
		ajax.put("captchaOnOff", captchaOnOff);
		if (!captchaOnOff) {
			return AjaxResult.success(ajax);
		}
		// 保存验证码信息
		String uuid = IdUtil.simpleUUID();
		String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
		String code = null;
		// 生成验证码
		CodeGenerator codeGenerator;
		AbstractCaptcha captcha;
		switch (captchaProperties.getType()) {
			case "math":
				codeGenerator = new UnsignedMathGenerator(captchaProperties.getNumberLength());
				break;
			case "char":
				codeGenerator = new RandomGenerator(captchaProperties.getCharLength());
				break;
			default:
				throw new IllegalArgumentException("验证码类型异常");
		}
		switch (captchaProperties.getCategory()) {
			case "line":
				captcha = lineCaptcha;
				break;
			case "circle":
				captcha = circleCaptcha;
				break;
			case "shear":
				captcha = shearCaptcha;
				break;
			default:
				throw new IllegalArgumentException("验证码类别异常");
		}
		captcha.setGenerator(codeGenerator);
		captcha.createCode();
		if ("math".equals(captchaProperties.getType())) {
			code = getCodeResult(captcha.getCode());
		} else if ("char".equals(captchaProperties.getType())) {
			code = captcha.getCode();
		}
		redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
		ajax.put("uuid", uuid);
		ajax.put("img", captcha.getImageBase64());
		return AjaxResult.success(ajax);
	}

	private String getCodeResult(String capStr) {
		int numberLength = captchaProperties.getNumberLength();
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
