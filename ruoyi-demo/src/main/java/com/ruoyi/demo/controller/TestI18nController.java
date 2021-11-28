package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.MessageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


/**
 * 测试国际化
 *
 * @author Lion Li
 */
@Validated
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

    /**
     * Validator 校验国际化
     * 不传值 分别查看异常返回
     *
     * 测试使用 not.null
     */
    @ApiOperation("Validator 校验国际化")
    @GetMapping("/test1")
    public AjaxResult<Void> test1(@NotBlank(message = "{not.null}") String str) {
        return AjaxResult.success(str);
    }

    /**
     * Bean 校验国际化
     * 不传值 分别查看异常返回
     *
     * 测试使用 not.null
     */
    @ApiOperation("Bean 校验国际化")
    @GetMapping("/test2")
    public AjaxResult<TestI18nBo> test2(@Validated TestI18nBo bo) {
        return AjaxResult.success(bo);
    }

    @Data
    public static class TestI18nBo {

        @NotBlank(message = "{not.null}")
        private String name;

        @NotNull(message = "{not.null}")
        @Range(min = 0, max = 100, message = "{length.not.valid}")
        private Integer age;
    }
}
