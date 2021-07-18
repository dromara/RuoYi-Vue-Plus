package com.ruoyi.system.controller;


import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.service.ISysOssService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/system/oss")
public class SysOssController extends BaseController {

	private final ISysOssService iSysOssService;

	/**
	 * 查询文件上传列表
	 */
	@PreAuthorize("@ss.hasPermi('system:oss:list')")
	@GetMapping("/list")
	public TableDataInfo<SysOss> list(SysOss sysOss) {
		return iSysOssService.queryPageList(sysOss);
	}

	/**
	 * 上传图片
	 */
	@PreAuthorize("@ss.hasPermi('system:oss:upload')")
	@Log(title = "OSS云存储", businessType = BusinessType.INSERT)
	@RepeatSubmit
	@PostMapping("/upload")
	public AjaxResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
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
	@PreAuthorize("@ss.hasPermi('system:oss:remove')")
	@Log(title = "OSS云存储" , businessType = BusinessType.DELETE)
	@DeleteMapping("/{ossIds}")
	public AjaxResult<Void> remove(@NotEmpty(message = "主键不能为空")
								   @PathVariable Long[] ossIds) {
		return toAjax(iSysOssService.deleteByIds(Arrays.asList(ossIds)) ? 1 : 0);
	}

}
