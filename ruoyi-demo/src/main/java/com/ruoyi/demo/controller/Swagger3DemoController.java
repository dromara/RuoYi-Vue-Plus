package com.ruoyi.demo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * swagger3 用法示例
 *
 * @author Lion Li
 */
@Api(value = "演示swagger3控制器", tags = {"演示swagger3接口"})
@RestController
@RequestMapping("/swagger/demo")
public class Swagger3DemoController {

	/**
	 * 上传请求
	 * 必须使用 @RequestPart 注解标注为文件
	 * dataType 必须为 "java.io.File"
	 */
	@ApiOperation(value = "通用上传请求")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "file", value = "文件", dataType = "java.io.File", required = true),
	})
	@PostMapping(value = "/upload")
	public AjaxResult<String> upload(@RequestPart("file") MultipartFile file) {
		return AjaxResult.success("操作成功", file.getOriginalFilename());
	}

}
