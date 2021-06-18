package com.ruoyi.common.core.mybatisplus.core;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 自定义 Mapper 接口, 实现 自定义扩展
 *
 * @author Lion Li
 * @since 2021-05-13
 */
public interface BaseMapperPlus<T> extends BaseMapper<T> {

	int insertAll(@Param("list") Collection<T> batchList);

}
