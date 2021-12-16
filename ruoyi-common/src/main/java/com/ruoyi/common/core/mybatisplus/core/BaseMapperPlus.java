package com.ruoyi.common.core.mybatisplus.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.BeanCopyUtils;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 自定义 Mapper 接口, 实现 自定义扩展
 *
 * @author Lion Li
 * @since 2021-05-13
 */
public interface BaseMapperPlus<T> extends BaseMapper<T> {

	/**
	 * 单sql批量插入( 全量填充 )
	 */
	int insertAll(@Param("list") Collection<T> batchList);

    /**
     * 根据 ID 查询
     */
    default <V> V selectVoById(Serializable id, Class<V> voClass){
        T obj = this.selectById(id);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        return BeanCopyUtils.copy(obj, voClass);
    }

    /**
     * 查询（根据ID 批量查询）
     */
    default <V> List<V> selectVoBatchIds(Collection<? extends Serializable> idList, Class<V> voClass){
        List<T> list = this.selectBatchIds(idList);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return BeanCopyUtils.copyList(list, voClass);
    }

    /**
     * 查询（根据 columnMap 条件）
     */
    default <V> List<V> selectVoByMap(Map<String, Object> map, Class<V> voClass){
        List<T> list = this.selectByMap(map);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return BeanCopyUtils.copyList(list, voClass);
    }

    /**
     * 根据 entity 条件，查询一条记录
     */
    default <V> V selectVoOne(Wrapper<T> wrapper, Class<V> voClass) {
        T obj = this.selectOne(wrapper);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        return BeanCopyUtils.copy(obj, voClass);
    }

    /**
     * 根据 entity 条件，查询全部记录
     */
    default <V> List<V> selectVoList(Wrapper<T> wrapper, Class<V> voClass) {
        List<T> list = this.selectList(wrapper);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return BeanCopyUtils.copyList(list, voClass);
    }

    /**
     * 分页查询VO
     */
    default <V, P extends IPage<V>> P selectVoPage(IPage<T> page, Wrapper<T> wrapper, Class<V> voClass) {
        IPage<T> pageData = this.selectPage(page, wrapper);
        IPage<V> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        if (CollUtil.isEmpty(pageData.getRecords())) {
            return (P) voPage;
        }
        voPage.setRecords(BeanCopyUtils.copyList(pageData.getRecords(), voClass));
        return (P) voPage;
    }

}
