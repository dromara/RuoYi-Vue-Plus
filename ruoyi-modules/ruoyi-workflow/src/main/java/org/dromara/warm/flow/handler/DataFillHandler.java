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
package org.dromara.warm.flow.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.entity.RootEntity;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

/**
 * 数据填充handler，以下三个接口按照实际情况实现
 *
 * @author warm
 * @since 2023/4/1 15:37
 */
public interface DataFillHandler {

    Logger logger = LoggerFactory.getLogger(DataFillHandler.class);

    /**
     * id填充。引擎在入库前就需要主键做行间互引（如历史任务的 instanceId/taskId），
     * 必须此处生成而非留给 MyBatis-Plus 在 insert 时补；与全局 IdentifierGenerator 同源，
     * insert 时对非空 id 不会重复生成
     *
     * @param object object
     */
    default void idFill(Object object) {
        if (object instanceof RootEntity entity && ObjectUtil.isNull(entity.getId())) {
            IdentifierGenerator generator = SpringUtils.getBeanOrNull(IdentifierGenerator.class);
            entity.setId(generator != null ? generator.nextId(entity).longValue() : IdWorker.getId());
        }
    }

    /**
     * 新增填充
     *
     * @param object object
     */
    default void insertFill(Object object) {
        RootEntity entity = (RootEntity) object;
        if (ObjectUtil.isNull(entity)) {
            logger.warn("Insert operation failed - Reason: Entity is null after casting");
            return;
        }
        entity.setCreateTime(ObjectUtil.isNotNull(entity.getCreateTime()) ? entity.getCreateTime() : new Date());
        entity.setUpdateTime(ObjectUtil.isNotNull(entity.getUpdateTime()) ? entity.getUpdateTime() : new Date());

        PermissionHandler permissionHandler = FlowEngine.permissionHandler();
        String handler = null;
        if (permissionHandler != null) {
            try {
                handler = permissionHandler.getHandler();
            } catch (Exception ignored) {
            }
        }
        entity.setCreateBy(StrUtil.isNotEmpty(handler) ? handler : entity.getCreateBy());
        entity.setUpdateBy(StrUtil.isNotEmpty(handler) ? handler : entity.getUpdateBy());
    }

    /**
     * 设置更新常用参数
     *
     * @param object object
     */
    default void updateFill(Object object) {
        RootEntity entity = (RootEntity) object;
        if (ObjectUtil.isNull(entity)) {
            logger.warn("Insert operation failed - Reason: Entity is null after casting");
            return;
        }
        entity.setUpdateTime(ObjectUtil.isNotNull(entity.getUpdateTime()) ? entity.getUpdateTime() : new Date());
        PermissionHandler permissionHandler = FlowEngine.permissionHandler();
        String handler = null;
        if (permissionHandler != null) {
            try {
                handler = permissionHandler.getHandler();
            } catch (Exception ignored) {
            }
        }
        entity.setUpdateBy(StrUtil.isNotEmpty(handler) ? handler : entity.getUpdateBy());
    }
}
