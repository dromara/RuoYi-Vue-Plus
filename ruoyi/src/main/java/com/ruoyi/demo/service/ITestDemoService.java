package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.domain.vo.TestDemoVo;
import com.ruoyi.demo.domain.bo.TestDemoBo;
import com.ruoyi.common.core.mybatisplus.core.IServicePlus;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 测试单表Service接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
public interface ITestDemoService extends IServicePlus<TestDemo, TestDemoVo> {

	/**
	 * 查询单个
	 * @return
	 */
	TestDemoVo queryById(Long id);

	/**
	 * 查询列表
	 */
    TableDataInfo<TestDemoVo> queryPageList(TestDemoBo bo);

	/**
	 * 自定义分页查询
	 */
	TableDataInfo<TestDemoVo> customPageList(TestDemoBo bo);

    /**
	 * 查询列表
	 */
	List<TestDemoVo> queryList(TestDemoBo bo);

	/**
	 * 根据新增业务对象插入测试单表
	 * @param bo 测试单表新增业务对象
	 * @return
	 */
	Boolean insertByBo(TestDemoBo bo);

	/**
	 * 根据编辑业务对象修改测试单表
	 * @param bo 测试单表编辑业务对象
	 * @return
	 */
	Boolean updateByBo(TestDemoBo bo);

	/**
	 * 校验并删除数据
	 * @param ids 主键集合
	 * @param isValid 是否校验,true-删除前校验,false-不校验
	 * @return
	 */
	Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
