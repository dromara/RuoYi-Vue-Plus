package com.ruoyi.web.controller.common;

import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
		String code = null;
		// 生成验证码
		CodeGenerator codeGenerator;
		AbstractCaptcha captcha;
		switch (captchaType) {
			case "math":
				codeGenerator = new MathGenerator(numberLength);
				break;
			case "char":
				codeGenerator = new RandomGenerator(charLength);
				break;
			default:
				throw new IllegalArgumentException("验证码类型异常");
		}
		switch (captchaCategory) {
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
		if ("math".equals(captchaType)) {
			code = getCodeResult(captcha.getCode());
		} else if ("char".equals(captchaType)) {
			code = captcha.getCode();
		}
		redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
		AjaxResult ajax = AjaxResult.success();
		ajax.put("uuid", uuid);
		ajax.put("img", captcha.getImageBase64());
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
