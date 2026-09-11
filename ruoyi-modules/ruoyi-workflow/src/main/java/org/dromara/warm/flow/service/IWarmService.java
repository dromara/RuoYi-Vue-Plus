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
package org.dromara.warm.flow.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Service接口
 *
 * @author warm
 * @since 2023-03-17
 */
public interface IWarmService<T> {
    /**
     * 根据id查询
     *
     * @param id 主键
     * @return 实体
     */
    T getById(Serializable id);

    /**
     * 根据ids查询
     *
     * @param ids 主键
     * @return 实体
     */
    List<T> getByIds(Collection<? extends Serializable> ids);

    /**
     * 查询列表
     *
     * @param entity 查询实体
     * @return 集合
     */
    List<T> list(T entity);

    /**
     * 查询一条记录
     *
     * @param entity 查询实体
     * @return 结果
     */
    T getOne(T entity);

    /**
     * 判断是否存在
     *
     * @param entity 查询实体
     * @return 结果
     */
    Boolean exists(T entity);

    /**
     * 新增
     *
     * @param entity 实体
     * @return 结果
     */
    boolean save(T entity);

    /**
     * 根据id修改
     *
     * @param entity 实体
     * @return 结果
     */
    boolean updateById(T entity);

    /**
     * 根据id删除
     *
     * @param id 主键
     * @return 结果
     */
    boolean removeById(Serializable id);

    /**
     * 根据entity删除
     *
     * @param entity 实体
     * @return 结果
     */
    boolean remove(T entity);

    /**
     * 根据ids批量删除
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    boolean removeByIds(Collection<? extends Serializable> ids);

    /**
     * 批量新增
     *
     * @param list 实体集合
     */
    void saveBatch(List<T> list);
}
