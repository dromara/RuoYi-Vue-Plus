package com.ruoyi.system.controller;


import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.system.bo.SysOssQueryBo;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.system.vo.SysOssVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传 控制层
 *
 * @author Lion Li
 */
@Validated
@Api(value = "OSS云存储控制器", tags = {"OSS云存储管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/system/oss")
public class SysOssController extends BaseController {

	private final ISysOssService iSysOssService;

	/**
	 * 查询OSS云存储列表
	 */
	@ApiOperation("查询OSS云存储列表")
	@PreAuthorize("@ss.hasPermi('system:oss:list')")
	@GetMapping("/list")
	public TableDataInfo<SysOssVo> list(@Validated SysOssQueryBo bo) {
		return iSysOssService.queryPageList(bo);
	}

	/**
	 * 上传OSS云存储
	 */
	@ApiOperation("上传OSS云存储")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "file", value = "文件", dataType = "java.io.File", required = true),
	})
	@PreAuthorize("@ss.hasPermi('system:oss:upload')")
	@Log(title = "OSS云存储", businessType = BusinessType.INSERT)
	@RepeatSubmit
	@PostMapping("/upload")
	public AjaxResult<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
		if (file.isEmpty()) {
			throw new CustomException("上传文件不能为空");
		}
		SysOss oss = iSysOssService.upload(file);
		Map<String, String> map = new HashMap<>(2);
		map.put("url", oss.getUrl());
		map.put("fileName", oss.getFileName());
		return AjaxResult.success(map);
	}

	/**
	 * 删除OSS云存储
	 */
	@ApiOperation("删除OSS云存储")
	@PreAuthorize("@ss.hasPermi('system:oss:remove')")
	@Log(title = "OSS云存储" , businessType = BusinessType.DELETE)
	@DeleteMapping("/{ossIds}")
	public AjaxResult<Void> remove(@NotEmpty(message = "主键不能为空")
								   @PathVariable Long[] ossIds) {
		return toAjax(iSysOssService.deleteWithValidByIds(Arrays.asList(ossIds), true) ? 1 : 0);
	}

}
