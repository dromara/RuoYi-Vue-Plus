package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.MessageUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 测试国际化
 *
 * @author Lion Li
 */
@RestController
@RequestMapping("/demo/i18n")
public class TestI18nController {

	/**
	 * 通过code获取国际化内容
	 * code为 messages.properties 中的 key
	 *
	 * 测试使用 user.register.success
	 */
	@GetMapping()
	public AjaxResult<Void> get(String code) {
		return AjaxResult.success(MessageUtils.message(code));
	}
}
