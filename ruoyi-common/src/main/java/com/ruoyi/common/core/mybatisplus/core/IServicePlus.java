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
 * @author Lion Li
 * @since 2021-05-13
 */
public interface IServicePlus<T, K> extends IService<T> {

	K getVoById(Serializable id, CopyOptions copyOptions);

	default K getVoById(Serializable id) {
		return getVoById(id, new CopyOptions());
	}

	default K getVoById(Serializable id, Function<T, K> convertor) {
		return convertor.apply(getById(id));
	}

	List<K> listVoByIds(Collection<? extends Serializable> idList, CopyOptions copyOptions);

	default List<K> listVoByIds(Collection<? extends Serializable> idList) {
		return listVoByIds(idList, new CopyOptions());
	}

	default List<K> listVoByIds(Collection<? extends Serializable> idList,
								Function<Collection<T>, List<K>> convertor) {
		List<T> list = getBaseMapper().selectBatchIds(idList);
		if (list == null) {
			return null;
		}
		return convertor.apply(list);
	}

	List<K> listVoByMap(Map<String, Object> columnMap, CopyOptions copyOptions);

	default List<K> listVoByMap(Map<String, Object> columnMap) {
		return listVoByMap(columnMap, new CopyOptions());
	}


	default List<K> listVoByMap(Map<String, Object> columnMap,
								Function<Collection<T>, List<K>> convertor) {
		List<T> list = getBaseMapper().selectByMap(columnMap);
		if (list == null) {
			return null;
		}
		return convertor.apply(list);
	}

	K getVoOne(Wrapper<T> queryWrapper, CopyOptions copyOptions);

	default K getVoOne(Wrapper<T> queryWrapper) {
		return getVoOne(queryWrapper, new CopyOptions());
	}

	default K getVoOne(Wrapper<T> queryWrapper, Function<T, K> convertor) {
		return convertor.apply(getOne(queryWrapper, true));
	}

	List<K> listVo(Wrapper<T> queryWrapper, CopyOptions copyOptions);

	default List<K> listVo(Wrapper<T> queryWrapper) {
		return listVo(queryWrapper, new CopyOptions());
	}

	default List<K> listVo(Wrapper<T> queryWrapper, Function<Collection<T>, List<K>> convertor) {
		List<T> list = getBaseMapper().selectList(queryWrapper);
		if (list == null) {
			return null;
		}
		return convertor.apply(list);
	}

	default List<K> listVo() {
		return listVo(Wrappers.emptyWrapper());
	}

	default List<K> listVo(Function<Collection<T>, List<K>> convertor) {
		return listVo(Wrappers.emptyWrapper(), convertor);
	}

	PagePlus<T, K> pageVo(PagePlus<T, K> page, Wrapper<T> queryWrapper, CopyOptions copyOptions);

	default PagePlus<T, K> pageVo(PagePlus<T, K> page, Wrapper<T> queryWrapper) {
		return pageVo(page, queryWrapper, new CopyOptions());
	}

	default PagePlus<T, K> pageVo(PagePlus<T, K> page, Wrapper<T> queryWrapper,
								  Function<Collection<T>, List<K>> convertor) {
		PagePlus<T, K> result = getBaseMapper().selectPage(page, queryWrapper);
		return result.setRecordsVo(convertor.apply(result.getRecords()));
	}

	default PagePlus<T, K> pageVo(PagePlus<T, K> page) {
		return pageVo(page, Wrappers.emptyWrapper());
	}

	default PagePlus<T, K> pageVo(PagePlus<T, K> page, Function<Collection<T>, List<K>> convertor) {
		return pageVo(page, Wrappers.emptyWrapper(), convertor);
	}

	boolean saveAll(Collection<T> entityList);
}

