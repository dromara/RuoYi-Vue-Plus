package com.ruoyi.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.demo.domain.TestTree;
import com.ruoyi.demo.domain.bo.TestTreeBo;
import com.ruoyi.demo.domain.vo.TestTreeVo;
import com.ruoyi.demo.mapper.TestTreeMapper;
import com.ruoyi.demo.service.ITestTreeService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测试树表Service业务层处理
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Service
public class TestTreeServiceImpl extends ServicePlusImpl<TestTreeMapper, TestTree, TestTreeVo> implements ITestTreeService {

	@Override
	public TestTreeVo queryById(Long id) {
		return getVoById(id);
	}

//	@DataSource(DataSourceType.SLAVE) // 切换从库查询
	@DataScope(isUser = true)
	@Override
	public List<TestTreeVo> queryList(TestTreeBo bo) {
		return listVo(buildQueryWrapper(bo));
	}

	private LambdaQueryWrapper<TestTree> buildQueryWrapper(TestTreeBo bo) {
		Map<String, Object> params = bo.getParams();
		Object dataScope = params.get("dataScope");
		LambdaQueryWrapper<TestTree> lqw = Wrappers.lambdaQuery();
		lqw.like(StrUtil.isNotBlank(bo.getTreeName()), TestTree::getTreeName, bo.getTreeName());
		lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
			TestTree::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
		lqw.apply(dataScope != null && StrUtil.isNotBlank(dataScope.toString()),
			dataScope != null ? dataScope.toString() : null);
		return lqw;
	}

	@Override
	public Boolean insertByBo(TestTreeBo bo) {
		TestTree add = BeanUtil.toBean(bo, TestTree.class);
		validEntityBeforeSave(add);
		return save(add);
	}

	@Override
	public Boolean updateByBo(TestTreeBo bo) {
		TestTree update = BeanUtil.toBean(bo, TestTree.class);
		validEntityBeforeSave(update);
		return updateById(update);
	}

	/**
	 * 保存前的数据校验
	 *
	 * @param entity 实体类数据
	 */
	private void validEntityBeforeSave(TestTree entity) {
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
