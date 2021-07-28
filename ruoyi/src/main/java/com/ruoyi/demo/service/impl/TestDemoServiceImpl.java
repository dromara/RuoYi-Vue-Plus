package com.ruoyi.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.domain.bo.TestDemoBo;
import com.ruoyi.demo.domain.vo.TestDemoVo;
import com.ruoyi.demo.mapper.TestDemoMapper;
import com.ruoyi.demo.service.ITestDemoService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测试单表Service业务层处理
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Service
public class TestDemoServiceImpl extends ServicePlusImpl<TestDemoMapper, TestDemo, TestDemoVo> implements ITestDemoService {

	@Override
	public TestDemoVo queryById(Long id) {
		return getVoById(id);
	}

	@DataScope(isUser = true)
	@Override
	public TableDataInfo<TestDemoVo> queryPageList(TestDemoBo bo) {
		PagePlus<TestDemo, TestDemoVo> result = pageVo(PageUtils.buildPagePlus(), buildQueryWrapper(bo));
		return PageUtils.buildDataInfo(result);
	}

	/**
	 * 自定义分页查询
	 */
	@DataScope(isUser = true)
	@Override
	public TableDataInfo<TestDemoVo> customPageList(TestDemoBo bo) {
		Page<TestDemoVo> result = baseMapper.customPageList(PageUtils.buildPage(), buildQueryWrapper(bo));
		return PageUtils.buildDataInfo(result);
	}

	@DataScope(isUser = true)
	@Override
	public List<TestDemoVo> queryList(TestDemoBo bo) {
		return listVo(buildQueryWrapper(bo));
	}

	private LambdaQueryWrapper<TestDemo> buildQueryWrapper(TestDemoBo bo) {
		Map<String, Object> params = bo.getParams();
		Object dataScope = params.get("dataScope");
		LambdaQueryWrapper<TestDemo> lqw = Wrappers.lambdaQuery();
		lqw.like(StrUtil.isNotBlank(bo.getTestKey()), TestDemo::getTestKey, bo.getTestKey());
		lqw.eq(StrUtil.isNotBlank(bo.getValue()), TestDemo::getValue, bo.getValue());
		lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
			TestDemo::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
		lqw.apply(dataScope != null && StrUtil.isNotBlank(dataScope.toString()),
			dataScope != null ? dataScope.toString() : null);
		return lqw;
	}

	@Override
	public Boolean insertByBo(TestDemoBo bo) {
		TestDemo add = BeanUtil.toBean(bo, TestDemo.class);
		validEntityBeforeSave(add);
		return save(add);
	}

	@Override
	public Boolean updateByBo(TestDemoBo bo) {
		TestDemo update = BeanUtil.toBean(bo, TestDemo.class);
		validEntityBeforeSave(update);
		return updateById(update);
	}

	/**
	 * 保存前的数据校验
	 *
	 * @param entity 实体类数据
	 */
	private void validEntityBeforeSave(TestDemo entity) {
		//TODO 做一些数据校验,如唯一约束
	}

	@Override
	public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
		if (isValid) {
			//TODO 做一些业务上的校验,判断是否需要校验
		}
		return removeByIds(ids);
	}
}
