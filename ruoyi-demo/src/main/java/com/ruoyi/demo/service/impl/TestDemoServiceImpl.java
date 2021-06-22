package com.ruoyi.demo.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.annotation.DataSource;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.PagePlus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.DataSourceType;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.demo.bo.TestDemoAddBo;
import com.ruoyi.demo.bo.TestDemoEditBo;
import com.ruoyi.demo.bo.TestDemoQueryBo;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.mapper.TestDemoMapper;
import com.ruoyi.demo.service.ITestDemoService;
import com.ruoyi.demo.vo.TestDemoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测试单表Service业务层处理
 *
 * @author Lion Li
 * @date 2021-05-30
 */
@Service
public class TestDemoServiceImpl extends ServicePlusImpl<TestDemoMapper, TestDemo> implements ITestDemoService {


	@Autowired
	private LockTemplate lockTemplate;

	@Override
	public void testLock4jLockTemaplate(String key) {
		final LockInfo lockInfo = lockTemplate.lock(key, 30000L, 5000L, RedissonLockExecutor.class);
		if (null == lockInfo) {
			throw new RuntimeException("业务处理中,请稍后再试");
		}
		// 获取锁成功，处理业务
		try {
			try {
				Thread.sleep(8000);
			} catch (InterruptedException e) {
				//
			}
		 System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName());
		} finally {
			//释放锁
			lockTemplate.releaseLock(lockInfo);
		}
		//结束
	}

	@Override
	@Lock4j(executor = RedissonLockExecutor.class,keys = {"#key"})
	public void testLock4j(String key) {
		System.out.println("start:"+key+",time:"+LocalTime.now().toString());
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("end :"+key+",time:"+LocalTime.now().toString());
	}

	@Override
	public TestDemoVo queryById(Long id) {
		return getVoById(id, TestDemoVo.class);
	}

	@DataScope(isUser = true)
	@Override
	public TableDataInfo<TestDemoVo> queryPageList(TestDemoQueryBo bo) {
		PagePlus<TestDemo, TestDemoVo> result = pageVo(PageUtils.buildPagePlus(), buildQueryWrapper(bo), TestDemoVo.class);
		return PageUtils.buildDataInfo(result);
	}

	@DataScope(isUser = true)
	@Override
	public List<TestDemoVo> queryList(TestDemoQueryBo bo) {
		return listVo(buildQueryWrapper(bo), TestDemoVo.class);
	}

	private LambdaQueryWrapper<TestDemo> buildQueryWrapper(TestDemoQueryBo bo) {
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
	public Boolean insertByAddBo(TestDemoAddBo bo) {
		TestDemo add = BeanUtil.toBean(bo, TestDemo.class);
		validEntityBeforeSave(add);
		return save(add);
	}

	@Override
	public Boolean updateByEditBo(TestDemoEditBo bo) {
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
