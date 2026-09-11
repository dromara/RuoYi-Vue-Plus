/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.dromara.warm.flow.entity.RootEntity;

import java.util.List;

/**
 * BaseMapper接口（含原 Dao 层通用查询，default 方法由 MyBatis 直接调用）
 *
 * @author warm
 * @since 2023-03-17
 */
public interface WarmMapper<T extends RootEntity> extends BaseMapper<T> {

    /**
     * 实体条件查询
     *
     * @param entity 条件实体
     */
    default List<T> selectList(T entity) {
        return selectList(new QueryWrapper<>(entity));
    }

    /**
     * 实体条件计数（接口类型 lambda 需显式设置实体类）
     */
    @SuppressWarnings("unchecked")
    default long selectCount(T entity) {
        LambdaQueryWrapper<T> queryWrapper = new LambdaQueryWrapper<>(entity);
        queryWrapper.setEntityClass((Class<T>) entity.getClass());
        return selectCount(queryWrapper);
    }

    /**
     * 按主键更新（接口类型 lambda 需显式设置实体类）
     */
    @SuppressWarnings("unchecked")
    default int update(T entity) {
        LambdaQueryWrapper<T> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.setEntityClass((Class<T>) entity.getClass());
        queryWrapper.eq(RootEntity::getId, entity.getId());
        return update(entity, queryWrapper);
    }

    /**
     * 实体条件删除
     */
    default int delete(T entity) {
        return delete(new LambdaQueryWrapper<>(entity));
    }
}
