package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.oss.entity.UploadResult;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.service.ICloudStorageService;
import com.ruoyi.system.domain.bo.SysOssBo;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.mapper.SysOssMapper;
import com.ruoyi.system.service.ISysOssService;
import com.ruoyi.system.domain.vo.SysOssVo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文件上传 服务层实现
 *
 * @author Lion Li
 */
@Service
public class SysOssServiceImpl extends ServicePlusImpl<SysOssMapper, SysOss, SysOssVo> implements ISysOssService {

	@Override
	public TableDataInfo<SysOssVo> queryPageList(SysOssBo bo) {
		PagePlus<SysOss, SysOssVo> result = pageVo(PageUtils.buildPagePlus(), buildQueryWrapper(bo));
		return PageUtils.buildDataInfo(result);
	}

	private LambdaQueryWrapper<SysOss> buildQueryWrapper(SysOssBo bo) {
		Map<String, Object> params = bo.getParams();
		LambdaQueryWrapper<SysOss> lqw = Wrappers.lambdaQuery();
		lqw.like(StrUtil.isNotBlank(bo.getFileName()), SysOss::getFileName, bo.getFileName());
		lqw.like(StrUtil.isNotBlank(bo.getOriginalName()), SysOss::getOriginalName, bo.getOriginalName());
		lqw.eq(StrUtil.isNotBlank(bo.getFileSuffix()), SysOss::getFileSuffix, bo.getFileSuffix());
		lqw.eq(StrUtil.isNotBlank(bo.getUrl()), SysOss::getUrl, bo.getUrl());
		lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
			SysOss::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
		lqw.eq(StrUtil.isNotBlank(bo.getCreateBy()), SysOss::getCreateBy, bo.getCreateBy());
		lqw.eq(StrUtil.isNotBlank(bo.getService()), SysOss::getService, bo.getService());
		return lqw;
	}

	@Override
	public SysOss upload(MultipartFile file) {
		String originalfileName = file.getOriginalFilename();
		String suffix = StrUtil.sub(originalfileName, originalfileName.lastIndexOf("."), originalfileName.length());
		ICloudStorageService storage = OssFactory.instance();
		UploadResult uploadResult;
		try {
			uploadResult = storage.uploadSuffix(file.getBytes(), suffix, file.getContentType());
		} catch (IOException e) {
			throw new CustomException("文件读取异常!!!", e);
		}
		// 保存文件信息
		SysOss oss = new SysOss()
			.setUrl(uploadResult.getUrl())
			.setFileSuffix(suffix)
			.setFileName(uploadResult.getFilename())
			.setOriginalName(originalfileName)
			.setService(storage.getServiceType());
		save(oss);
		return oss;
	}

	@Override
	public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
		if (isValid) {
			// 做一些业务上的校验,判断是否需要校验
		}
		List<SysOss> list = listByIds(ids);
		for (SysOss sysOss : list) {
			ICloudStorageService storage = OssFactory.instance(sysOss.getService());
			storage.delete(sysOss.getUrl());
		}
		return removeByIds(ids);
	}

}
