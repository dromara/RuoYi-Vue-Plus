package com.ruoyi.common.core.page;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自定义 Service 接口, 实现 数据库实体与 vo 对象转换返回
 *
 * @author Lion Li
 * @since 2021-05-13
 */
public interface IServicePlus<T> extends IService<T> {

    /**
     * 根据 ID 查询
     *
     * @param kClass vo类型
     * @param id     主键ID
     */
    default <K> K getVoById(Serializable id, Class<K> kClass) {
        T t = getBaseMapper().selectById(id);
        return BeanUtil.toBean(t, kClass);
    }

    /**
     * 根据 ID 查询
     *
     * @param id        主键ID
     * @param convertor 转换函数
     * @param <K>       vo类型
     */
    default <K> K getVoById(Serializable id, Function<T, K> convertor) {
        T t = getBaseMapper().selectById(id);
        return convertor.apply(t);
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param kClass vo类型
     * @param idList 主键ID列表
     */
    default <K> List<K> listVoByIds(Collection<? extends Serializable> idList, Class<K> kClass) {
        List<T> list = getBaseMapper().selectBatchIds(idList);
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(any -> BeanUtil.toBean(any, kClass))
                .collect(Collectors.toList());
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param convertor 转换函数
     * @param idList    主键ID列表
     */
    default <K> List<K> listVoByIds(Collection<? extends Serializable> idList,
                                    Function<Collection<T>, List<K>> convertor) {
        List<T> list = getBaseMapper().selectBatchIds(idList);
        if (list == null) {
            return null;
        }
        return convertor.apply(list);
    }

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param kClass    vo类型
     * @param columnMap 表字段 map 对象
     */
    default <K> List<K> listVoByMap(Map<String, Object> columnMap, Class<K> kClass) {
        List<T> list = getBaseMapper().selectByMap(columnMap);
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(any -> BeanUtil.toBean(any, kClass))
                .collect(Collectors.toList());
    }

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param convertor 转换函数
     * @param columnMap 表字段 map 对象
     */
    default <K> List<K> listVoByMap(Map<String, Object> columnMap,
                                    Function<Collection<T>, List<K>> convertor) {
        List<T> list = getBaseMapper().selectByMap(columnMap);
        if (list == null) {
            return null;
        }
        return convertor.apply(list);
    }

    /**
     * 根据 Wrapper，查询一条记录 <br/>
     * <p>结果集，如果是多个会抛出异常，随机取一条加上限制条件 wrapper.last("LIMIT 1")</p>
     *
     * @param kClass       vo类型
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <K> K getVoOne(Wrapper<T> queryWrapper, Class<K> kClass) {
        return BeanUtil.toBean(getOne(queryWrapper, true), kClass);
    }

    /**
     * 根据 Wrapper，查询一条记录 <br/>
     * <p>结果集，如果是多个会抛出异常，随机取一条加上限制条件 wrapper.last("LIMIT 1")</p>
     *
     * @param convertor    转换函数
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <K> K getVoOne(Wrapper<T> queryWrapper, Function<T, K> convertor) {
        return convertor.apply(getOne(queryWrapper, true));
    }

    /**
     * 查询列表
     *
     * @param kClass       vo类型
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <K> List<K> listVo(Wrapper<T> queryWrapper, Class<K> kClass) {
        List<T> list = getBaseMapper().selectList(queryWrapper);
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(any -> BeanUtil.toBean(any, kClass))
                .collect(Collectors.toList());
    }

    /**
     * 查询列表
     *
     * @param convertor    转换函数
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    default <K> List<K> listVo(Wrapper<T> queryWrapper, Function<Collection<T>, List<K>> convertor) {
        List<T> list = getBaseMapper().selectList(queryWrapper);
        if (list == null) {
            return null;
        }
        return convertor.apply(list);
    }

    /**
     * 查询所有
     *
     * @param kClass vo类型
     * @see Wrappers#emptyWrapper()
     */
    default <K> List<K> listVo(Class<K> kClass) {
        return listVo(Wrappers.emptyWrapper(), kClass);
    }

    /**
     * 查询所有
     *
     * @param convertor 转换函数
     * @see Wrappers#emptyWrapper()
     */
    default <K> List<K> listVo(Function<Collection<T>, List<K>> convertor) {
        return listVo(Wrappers.emptyWrapper(), convertor);
    }

    /**
     * 翻页查询
     *
     * @param page         翻页对象
     * @param queryWrapper 实体对象封装操作类
     * @param kClass       vo类型
     */
    default <K> PagePlus<T, K> pageVo(PagePlus<T, K> page, Wrapper<T> queryWrapper, Class<K> kClass) {
        PagePlus<T, K> result = getBaseMapper().selectPage(page, queryWrapper);
        List<K> volist = result.getRecords().stream()
                .map(any -> BeanUtil.toBean(any, kClass))
                .collect(Collectors.toList());
        result.setRecordsVo(volist);
        return result;
    }

    /**
     * 翻页查询
     *
     * @param page         翻页对象
     * @param queryWrapper 实体对象封装操作类
     * @param convertor    转换函数
     */
    default <K> PagePlus<T, K> pageVo(PagePlus<T, K> page, Wrapper<T> queryWrapper,
                                      Function<Collection<T>, List<K>> convertor) {
        PagePlus<T, K> result = getBaseMapper().selectPage(page, queryWrapper);
        return result.setRecordsVo(convertor.apply(result.getRecords()));
    }

    /**
     * 无条件翻页查询
     *
     * @param page   翻页对象
     * @param kClass vo类型
     */
    default <K> PagePlus<T, K> pageVo(PagePlus<T, K> page, Class<K> kClass) {
        return pageVo(page, Wrappers.emptyWrapper(), kClass);
    }

    /**
     * 无条件翻页查询
     *
     * @param page      翻页对象
     * @param convertor 转换函数
     */
    default <K> PagePlus<T, K> pageVo(PagePlus<T, K> page, Function<Collection<T>, List<K>> convertor) {
        return pageVo(page, Wrappers.emptyWrapper(), convertor);
    }

}

