package com.ruoyi.system.controller;


import cn.hutool.core.convert.Convert;
import cn.hutool.http.HttpUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.system.domain.bo.SysOssBo;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.system.domain.vo.SysOssVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
	public TableDataInfo<SysOssVo> list(@Validated SysOssBo bo) {
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

	@ApiOperation("下载OSS云存储")
	@PreAuthorize("@ss.hasPermi('system:oss:download')")
	@GetMapping("/download/{ossId}")
	public void download(@PathVariable Long ossId, HttpServletResponse response) throws IOException {
		SysOss sysOss = iSysOssService.getById(ossId);
		if (sysOss == null) {
			throw new CustomException("文件数据不存在!");
		}
		response.reset();
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
		FileUtils.setAttachmentResponseHeader(response, URLEncoder.encode(sysOss.getOriginalName(), StandardCharsets.UTF_8.toString()));
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
		long data = HttpUtil.download(sysOss.getUrl(), response.getOutputStream(), false);
		response.setContentLength(Convert.toInt(data));
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
