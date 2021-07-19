package com.ruoyi.common.core.mybatisplus.core;

import com.baomidou.mybatisplus.core.toolkit.ClassUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;

import java.util.Collection;

/**
 * IServicePlus 实现类
 *
 * @author Lion Li
 */
@Slf4j
@SuppressWarnings("unchecked")
public class ServicePlusImpl<M extends BaseMapperPlus<T>, T> extends ServiceImpl<M, T> implements IServicePlus<T> {

	@Autowired
	protected M baseMapper;

	@Override
	public M getBaseMapper() {
		return baseMapper;
	}


	protected Class<T> entityClass = currentModelClass();

	@Override
	public Class<T> getEntityClass() {
		return entityClass;
	}

	protected Class<T> mapperClass = currentMapperClass();

	@Override
	protected Class<T> currentMapperClass() {
		return (Class<T>) this.getResolvableType().as(ServicePlusImpl.class).getGeneric(0).getType();
	}

	@Override
	protected Class<T> currentModelClass() {
		return (Class<T>) this.getResolvableType().as(ServicePlusImpl.class).getGeneric(1).getType();
	}

	@Override
	protected ResolvableType getResolvableType() {
		return ResolvableType.forClass(ClassUtils.getUserClass(getClass()));
	}

	/**
	 * 单条执行性能差 适用于列表对象内容不确定
	 */
	@Override
	public boolean saveBatch(Collection<T> entityList, int batchSize) {
		return super.saveBatch(entityList, batchSize);
	}

	@Override
	public boolean saveOrUpdate(T entity) {
		return super.saveOrUpdate(entity);
	}

	/**
	 * 单条执行性能差 适用于列表对象内容不确定
	 */
	@Override
	public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
		return super.saveOrUpdateBatch(entityList, batchSize);
	}

	@Override
	public boolean updateBatchById(Collection<T> entityList, int batchSize) {
		return super.updateBatchById(entityList, batchSize);
	}

	/**
	 * 单sql批量插入( 全量填充 无视数据库默认值 )
	 * 适用于无脑插入
	 */
	@Override
	public boolean saveAll(Collection<T> entityList) {
		return baseMapper.insertAll(entityList) == entityList.size();
	}

}
