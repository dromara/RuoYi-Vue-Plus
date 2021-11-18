package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.MessageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 测试国际化
 *
 * @author Lion Li
 */
@Api(value = "测试国际化控制器", tags = {"测试国际化管理"})
@RestController
@RequestMapping("/demo/i18n")
public class TestI18nController {

	/**
	 * 通过code获取国际化内容
	 * code为 messages.properties 中的 key
	 *
	 * 测试使用 user.register.success
	 */
	@ApiOperation("通过code获取国际化内容")
	@GetMapping()
	public AjaxResult<Void> get(@ApiParam("国际化code") String code) {
		return AjaxResult.success(MessageUtils.message(code));
	}
}
