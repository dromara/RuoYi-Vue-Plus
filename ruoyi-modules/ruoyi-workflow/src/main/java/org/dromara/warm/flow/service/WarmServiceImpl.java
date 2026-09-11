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

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.entity.RootEntity;
import org.dromara.warm.flow.handler.DataFillHandler;
import org.dromara.warm.flow.mapper.WarmMapper;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * BaseService层处理，直接持有 Mapper，无独立 Dao 层
 *
 * @author warm
 * @since 2023-03-17
 */
public abstract class WarmServiceImpl<T extends RootEntity> implements IWarmService<T> {

    /**
     * 获取Mapper（由子类提供，Mapper 由 MyBatis 扫描 mapper 包注册）
     */
    public abstract WarmMapper<T> getMapper();

    @Override
    public T getById(Serializable id) {
        return getMapper().selectById(id);
    }

    @Override
    public List<T> getByIds(Collection<? extends Serializable> ids) {
        return getMapper().selectByIds(ids);
    }

    @Override
    public List<T> list(T entity) {
        return getMapper().selectList(entity);
    }

    @Override
    public T getOne(T entity) {
        List<T> list = getMapper().selectList(entity);
        return CollUtil.getFirst(list);
    }

    @Override
    public Boolean exists(T entity) {
        return getMapper().selectCount(entity) > 0;
    }

    @Override
    public boolean save(T entity) {
        insertFill(entity);
        return SqlHelper.retBool(getMapper().insert(entity));
    }

    @Override
    public boolean updateById(T entity) {
        updateFill(entity);
        return SqlHelper.retBool(getMapper().update(entity));
    }

    @Override
    public boolean removeById(Serializable id) {
        return SqlHelper.retBool(getMapper().deleteById(id));
    }

    @Override
    public boolean remove(T entity) {
        return SqlHelper.retBool(getMapper().delete(entity));
    }

    @Override
    public boolean removeByIds(Collection<? extends Serializable> ids) {
        return SqlHelper.retBool(getMapper().deleteByIds(ids));
    }

    @Override
    public void saveBatch(List<T> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(this::save);
    }

    public void insertFill(T entity) {
        DataFillHandler dataFillHandler = FlowEngine.dataFillHandler();
        if (dataFillHandler == null) {
            return;
        }
        dataFillHandler.idFill(entity);
        dataFillHandler.insertFill(entity);
    }

    public void updateFill(T entity) {
        DataFillHandler dataFillHandler = FlowEngine.dataFillHandler();
        if (dataFillHandler == null) {
            return;
        }
        dataFillHandler.updateFill(entity);
    }
}
