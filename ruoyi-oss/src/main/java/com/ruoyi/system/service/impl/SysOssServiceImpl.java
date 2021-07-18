package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.oss.factory.OssFactory;
import com.ruoyi.oss.service.ICloudStorageService;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.mapper.SysOssMapper;
import com.ruoyi.system.service.ISysOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * 文件上传 服务层实现
 *
 * @author Lion Li
 */
@Slf4j
@Service
public class SysOssServiceImpl extends ServiceImpl<SysOssMapper, SysOss> implements ISysOssService {

	@Override
	public TableDataInfo<SysOss> queryPageList(SysOss sysOss) {
		LambdaQueryWrapper<SysOss> lqw = Wrappers.lambdaQuery();
		lqw.like(StrUtil.isNotBlank(sysOss.getFileName()), SysOss::getFileName, sysOss.getFileName());
		lqw.like(StrUtil.isNotBlank(sysOss.getFileSuffix()), SysOss::getFileSuffix, sysOss.getFileSuffix());
		lqw.like(StrUtil.isNotBlank(sysOss.getUrl()), SysOss::getUrl, sysOss.getUrl());
		lqw.like(StrUtil.isNotBlank(sysOss.getService()), SysOss::getService, sysOss.getService());
		return PageUtils.buildDataInfo(page(PageUtils.buildPage(), lqw));
	}

	@Override
	public SysOss upload(MultipartFile file) {
		String originalfileName = file.getOriginalFilename();
		String suffix = StrUtil.sub(originalfileName, originalfileName.lastIndexOf("."), originalfileName.length());
		try {
			ICloudStorageService storage = OssFactory.instance();
			String url = storage.uploadSuffix(file.getBytes(), suffix);
			// 保存文件信息
			SysOss oss = new SysOss()
				.setUrl(url).setFileSuffix(suffix)
				.setFileName(originalfileName)
				.setService(storage.getServiceType());
			save(oss);
			return oss;
		} catch (IOException e) {
			throw new CustomException("文件读取异常!!!", e);
		}
	}

	@Override
	public Boolean deleteByIds(Collection<Long> ids) {
		List<SysOss> list = listByIds(ids);
		for (SysOss sysOss : list) {
			ICloudStorageService storage = OssFactory.instance(sysOss.getService());
			storage.delete(sysOss.getUrl());
		}
		return removeByIds(ids);
	}

}
