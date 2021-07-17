package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.oss.config.CloudStorageConfig;
import com.ruoyi.oss.service.abstractd.AbstractCloudStorageService;
import com.ruoyi.system.domain.SysOss;
import com.ruoyi.system.factory.OSSFactory;
import com.ruoyi.system.mapper.SysOssMapper;
import com.ruoyi.system.service.ISysOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传 服务层实现
 *
 * @author chkj
 * @date 2019-07-15
 */
@Slf4j
@Service
public class SysOssServiceImpl extends ServiceImpl<SysOssMapper, SysOss> implements ISysOssService {

	@Autowired
	private CloudStorageConfig config;

	@Override
	public List<SysOss> list(SysOss sysOss) {
		LambdaQueryWrapper<SysOss> wrapper = new LambdaQueryWrapper<>();
		return baseMapper.selectList(wrapper);
	}

	@Override
	public Map<String, String> upload(MultipartFile file) {
		String originalfileName = file.getOriginalFilename();
		String suffix = originalfileName.substring(originalfileName.lastIndexOf("."));
		try {
			AbstractCloudStorageService storage = OSSFactory.build();
			String url = storage.uploadSuffix(file.getBytes(), suffix);
			// 保存文件信息
			SysOss ossEntity = new SysOss()
				.setUrl(url).setFileSuffix(suffix)
				.setFileName(originalfileName)
				.setService(storage.getServiceType());
			save(ossEntity);
			Map<String, String> map = new HashMap<>(2);
			map.put("url", ossEntity.getUrl());
			map.put("fileName", ossEntity.getFileName());
			return map;
		} catch (IOException e) {
			throw new CustomException("文件读取异常!!!", e);
		}
	}

}
