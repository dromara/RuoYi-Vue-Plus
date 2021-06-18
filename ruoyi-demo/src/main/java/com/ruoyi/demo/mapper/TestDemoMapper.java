package com.ruoyi.demo.mapper;

import com.ruoyi.common.core.mybatisplus.cache.MybatisPlusRedisCache;
import com.ruoyi.common.core.mybatisplus.core.BaseMapperPlus;
import com.ruoyi.demo.domain.TestDemo;
import org.apache.ibatis.annotations.CacheNamespace;

/**
 * 测试单表Mapper接口
 *
 * @author Lion Li
 * @date 2021-05-30
 */
@CacheNamespace(implementation = MybatisPlusRedisCache.class, eviction = MybatisPlusRedisCache.class)
public interface TestDemoMapper extends BaseMapperPlus<TestDemo> {

}
