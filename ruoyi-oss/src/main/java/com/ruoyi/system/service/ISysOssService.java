package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysOss;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

/**
 * 文件上传 服务层
 *
 * @author Lion Li
 */
public interface ISysOssService extends IService<SysOss> {

	TableDataInfo<SysOss> queryPageList(SysOss sysOss);

	SysOss upload(MultipartFile file);

	Boolean deleteByIds(Collection<Long> ids);
}
