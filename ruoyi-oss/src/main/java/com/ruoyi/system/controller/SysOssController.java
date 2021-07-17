package com.ruoyi.system.controller;


import com.aliyun.oss.ServiceException;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.service.ISysOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件上传 控制层
 *
 * @author chkj
 * @date 2019-07-15
 */
@Slf4j
@RestController
@RequestMapping("/system/oss")
public class SysOssController extends BaseController {

	@Autowired
	private ISysOssService iSysOssService;

	/**
	 * 查询文件上传列表
	 */
	@GetMapping("/list")
	public TableDataInfo list(SysOss sysOss) {
		List<SysOss> list = iSysOssService.list(sysOss);
		return PageUtils.buildDataInfo(list);
	}

	/**
	 * 上传图片
	 */
	@PostMapping("/upload")
	public AjaxResult upload(@RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			throw new CustomException("上传文件不能为空");
		}
		Map<String, String> json = iSysOssService.upload(file);
		return AjaxResult.success(json);
	}

}
