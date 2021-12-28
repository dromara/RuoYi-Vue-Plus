package com.ruoyi.web.controller.system;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.QueryGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.oss.constant.OssConstant;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.domain.bo.SysOssBo;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysOssService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传 控制层
 *
 * @author Lion Li
 */
@Validated
@Api(value = "OSS对象存储控制器", tags = {"OSS对象存储管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/system/oss")
public class SysOssController extends BaseController {

    private final ISysOssService iSysOssService;
    private final ISysConfigService iSysConfigService;

    /**
     * 查询OSS对象存储列表
     */
    @ApiOperation("查询OSS对象存储列表")
    @PreAuthorize("@ss.hasPermi('system:oss:list')")
    @GetMapping("/list")
    public TableDataInfo<SysOssVo> list(@Validated(QueryGroup.class) SysOssBo bo, PageQuery pageQuery) {
        return iSysOssService.queryPageList(bo, pageQuery);
    }

    /**
     * 上传OSS对象存储
     */
    @ApiOperation("上传OSS对象存储")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "文件", dataTypeClass = File.class, required = true),
    })
    @PreAuthorize("@ss.hasPermi('system:oss:upload')")
    @Log(title = "OSS对象存储", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/upload")
    public AjaxResult<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        if (ObjectUtil.isNull(file)) {
            throw new ServiceException("上传文件不能为空");
        }
        SysOss oss = iSysOssService.upload(file);
        Map<String, String> map = new HashMap<>(2);
        map.put("url", oss.getUrl());
        map.put("fileName", oss.getFileName());
        return AjaxResult.success(map);
    }

    @ApiOperation("下载OSS对象存储")
    @PreAuthorize("@ss.hasPermi('system:oss:download')")
    @GetMapping("/download/{ossId}")
    public void download(@ApiParam("OSS对象ID") @PathVariable Long ossId, HttpServletResponse response) throws IOException {
        SysOss sysOss = iSysOssService.getById(ossId);
        if (ObjectUtil.isNull(sysOss)) {
            throw new ServiceException("文件数据不存在!");
        }
        response.reset();
        FileUtils.setAttachmentResponseHeader(response, sysOss.getOriginalName());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
        long data;
        try {
            data = HttpUtil.download(sysOss.getUrl(), response.getOutputStream(), false);
        } catch (HttpException e) {
            if (e.getMessage().contains("403")) {
                throw new ServiceException("无读取权限, 请在对应的OSS开启'公有读'权限!");
            } else {
                throw new ServiceException(e.getMessage());
            }
        }
        response.setContentLength(Convert.toInt(data));
    }

    /**
     * 删除OSS对象存储
     */
    @ApiOperation("删除OSS对象存储")
    @PreAuthorize("@ss.hasPermi('system:oss:remove')")
    @Log(title = "OSS对象存储", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ossIds}")
    public AjaxResult<Void> remove(@ApiParam("OSS对象ID串")
                                   @NotEmpty(message = "主键不能为空")
                                   @PathVariable Long[] ossIds) {
        return toAjax(iSysOssService.deleteWithValidByIds(Arrays.asList(ossIds), true) ? 1 : 0);
    }

    /**
     * 变更图片列表预览状态
     */
    @ApiOperation("变更图片列表预览状态")
    @PreAuthorize("@ss.hasPermi('system:oss:edit')")
    @Log(title = "OSS对象存储", businessType = BusinessType.UPDATE)
    @PutMapping("/changePreviewListResource")
    public AjaxResult<Void> changePreviewListResource(@RequestBody String body) {
        Map<String, Boolean> map = JsonUtils.parseMap(body);
        SysConfig config = iSysConfigService.getOne(new LambdaQueryWrapper<SysConfig>()
            .eq(SysConfig::getConfigKey, OssConstant.PEREVIEW_LIST_RESOURCE_KEY));
        config.setConfigValue(map.get("previewListResource").toString());
        return toAjax(iSysConfigService.updateConfig(config));
    }

}
