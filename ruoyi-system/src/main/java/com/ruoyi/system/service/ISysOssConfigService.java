package com.ruoyi.system.service;

import com.ruoyi.system.domain.SysOssConfig;
import com.ruoyi.system.domain.vo.SysOssConfigVo;
import com.ruoyi.system.domain.bo.SysOssConfigBo;
import com.ruoyi.common.core.mybatisplus.core.IServicePlus;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 云存储配置Service接口
 *
 * @author ruoyi
 * @date 2021-08-11
 */
public interface ISysOssConfigService extends IServicePlus<SysOssConfig, SysOssConfigVo> {
	/**
	 * 查询单个
	 * @return
	 */
	SysOssConfigVo queryById(Integer ossConfigId);

	/**
	 * 查询列表
	 */
    TableDataInfo<SysOssConfigVo> queryPageList(SysOssConfigBo bo);


	/**
	 * 根据新增业务对象插入云存储配置
	 * @param bo 云存储配置新增业务对象
	 * @return
	 */
	Boolean insertByBo(SysOssConfigBo bo);

	/**
	 * 根据编辑业务对象修改云存储配置
	 * @param bo 云存储配置编辑业务对象
	 * @return
	 */
	Boolean updateByBo(SysOssConfigBo bo);

	/**
	 * 校验并删除数据
	 * @param ids 主键集合
	 * @param isValid 是否校验,true-删除前校验,false-不校验
	 * @return
	 */
	Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid);

	/**
	 * 启用停用状态
	 * @param sysOssConfig
	 * @return
	 */
	int updateOssConfigStatus(SysOssConfig sysOssConfig);

	/**
	 * 判断configkey是否唯一
	 * @param bo
	 * @return
	 */
	String checkConfigKeyUnique(SysOssConfigBo bo);
}
