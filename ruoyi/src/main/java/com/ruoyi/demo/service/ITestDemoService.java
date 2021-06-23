package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.vo.TestDemoVo;
import com.ruoyi.demo.bo.TestDemoQueryBo;
import com.ruoyi.demo.bo.TestDemoAddBo;
import com.ruoyi.demo.bo.TestDemoEditBo;
import com.ruoyi.common.core.mybatisplus.core.IServicePlus;
import com.ruoyi.common.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 测试单表Service接口
 *
 * @author Lion Li
 * @date 2021-05-30
 */
public interface ITestDemoService extends IServicePlus<TestDemo> {

	/**
	 * 查询单个
	 * @return
	 */
	TestDemoVo queryById(Long id);

	/**
	 * 查询列表
	 */
    TableDataInfo<TestDemoVo> queryPageList(TestDemoQueryBo bo);
	/**
	 * 查询列表
	 */
	List<TestDemoVo> queryList(TestDemoQueryBo bo);

	/**
	 * 根据新增业务对象插入测试单表
	 * @param bo 测试单表新增业务对象
	 * @return
	 */
	Boolean insertByAddBo(TestDemoAddBo bo);

	/**
	 * 根据编辑业务对象修改测试单表
	 * @param bo 测试单表编辑业务对象
	 * @return
	 */
	Boolean updateByEditBo(TestDemoEditBo bo);

	/**
	 * 校验并删除数据
	 * @param ids 主键集合
	 * @param isValid 是否校验,true-删除前校验,false-不校验
	 * @return
	 */
	Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
