package com.ruoyi.common.core.mybatisplus.core;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.page.PagePlus;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 自定义 Service 接口, 实现 数据库实体与 vo 对象转换返回
 *
 * @param <T> 数据实体类
 * @param <V> vo类
 * @author Lion Li
 */
public interface IServicePlus<T, V> extends IService<T> {

	/**
	 * @param id          主键id
	 * @param copyOptions copy条件
	 * @return V对象
	 */
	V getVoById(Serializable id, CopyOptions copyOptions);

	default V getVoById(Serializable id) {
		return getVoById(id, new CopyOptions());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default V getVoById(Serializable id, Function<T, V> convertor) {
		return convertor.apply(getById(id));
	}

	/**
	 * @param idList      id列表
	 * @param copyOptions copy条件
	 * @return V对象
	 */
	List<V> listVoByIds(Collection<? extends Serializable> idList, CopyOptions copyOptions);

	default List<V> listVoByIds(Collection<? extends Serializable> idList) {
		return listVoByIds(idList, new CopyOptions());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default List<V> listVoByIds(Collection<? extends Serializable> idList,
								Function<Collection<T>, List<V>> convertor) {
		List<T> list = getBaseMapper().selectBatchIds(idList);
		if (list == null) {
			return null;
		}
		return convertor.apply(list);
	}

	/**
	 * @param columnMap   表字段 map 对象
	 * @param copyOptions copy条件
	 * @return V对象
	 */
	List<V> listVoByMap(Map<String, Object> columnMap, CopyOptions copyOptions);

	default List<V> listVoByMap(Map<String, Object> columnMap) {
		return listVoByMap(columnMap, new CopyOptions());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default List<V> listVoByMap(Map<String, Object> columnMap,
								Function<Collection<T>, List<V>> convertor) {
		List<T> list = getBaseMapper().selectByMap(columnMap);
		if (list == null) {
			return null;
		}
		return convertor.apply(list);
	}

	/**
	 * @param queryWrapper 查询条件
	 * @param copyOptions  copy条件
	 * @return V对象
	 */
	V getVoOne(Wrapper<T> queryWrapper, CopyOptions copyOptions);

	default V getVoOne(Wrapper<T> queryWrapper) {
		return getVoOne(queryWrapper, new CopyOptions());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default V getVoOne(Wrapper<T> queryWrapper, Function<T, V> convertor) {
		return convertor.apply(getOne(queryWrapper, true));
	}

	/**
	 * @param queryWrapper 查询条件
	 * @param copyOptions  copy条件
	 * @return V对象
	 */
	List<V> listVo(Wrapper<T> queryWrapper, CopyOptions copyOptions);

	default List<V> listVo(Wrapper<T> queryWrapper) {
		return listVo(queryWrapper, new CopyOptions());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default List<V> listVo(Wrapper<T> queryWrapper, Function<Collection<T>, List<V>> convertor) {
		List<T> list = getBaseMapper().selectList(queryWrapper);
		if (list == null) {
			return null;
		}
		return convertor.apply(list);
	}

	default List<V> listVo() {
		return listVo(Wrappers.emptyWrapper());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default List<V> listVo(Function<Collection<T>, List<V>> convertor) {
		return listVo(Wrappers.emptyWrapper(), convertor);
	}

	/**
	 * @param page         分页对象
	 * @param queryWrapper 查询条件
	 * @param copyOptions  copy条件
	 * @return V对象
	 */
	PagePlus<T, V> pageVo(PagePlus<T, V> page, Wrapper<T> queryWrapper, CopyOptions copyOptions);

	default PagePlus<T, V> pageVo(PagePlus<T, V> page, Wrapper<T> queryWrapper) {
		return pageVo(page, queryWrapper, new CopyOptions());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default PagePlus<T, V> pageVo(PagePlus<T, V> page, Wrapper<T> queryWrapper,
								  Function<Collection<T>, List<V>> convertor) {
		PagePlus<T, V> result = getBaseMapper().selectPage(page, queryWrapper);
		return result.setRecordsVo(convertor.apply(result.getRecords()));
	}

	default PagePlus<T, V> pageVo(PagePlus<T, V> page) {
		return pageVo(page, Wrappers.emptyWrapper());
	}

	/**
	 * @param convertor 自定义转换器
	 */
	default PagePlus<T, V> pageVo(PagePlus<T, V> page, Function<Collection<T>, List<V>> convertor) {
		return pageVo(page, Wrappers.emptyWrapper(), convertor);
	}

	boolean saveAll(Collection<T> entityList);

	boolean saveOrUpdateAll(Collection<T> entityList);
}

