package com.ruoyi.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.mybatisplus.cache.MybatisPlusRedisCache;
import com.ruoyi.common.core.mybatisplus.core.BaseMapperPlus;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.domain.vo.TestDemoVo;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;

/**
 * 测试单表Mapper接口
 *
 * @author Lion Li
 * @date 2021-07-26
 */
// 如使需切换数据源 请勿使用缓存 会造成数据不一致现象
@CacheNamespace(implementation = MybatisPlusRedisCache.class, eviction = MybatisPlusRedisCache.class)
public interface TestDemoMapper extends BaseMapperPlus<TestDemo> {

    Page<TestDemoVo> customPageList(@Param("page") Page<TestDemo> page, @Param("ew") Wrapper<TestDemo> wrapper);

}
