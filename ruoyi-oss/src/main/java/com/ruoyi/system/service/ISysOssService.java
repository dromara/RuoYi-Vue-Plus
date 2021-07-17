package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.system.domain.SysOss;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件上传 服务层
 *
 * @author chkj
 * @date 2019-07-15
 */
public interface ISysOssService extends IService<SysOss> {
	/**
	 * 列表查询
	 */
	List<SysOss> list(SysOss sysOss);

	Map<String, String> upload(MultipartFile file);

}
