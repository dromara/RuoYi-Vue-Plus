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
import org.dromara.warm.flow.entity.FlowInstance;

import java.util.List;

/**
 * 流程实例Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowInstanceMapper extends WarmMapper<FlowInstance> {

    /**
     * 根据流程定义ID,查询流程实例集合
     *
     * @param defIds 流程定义ID集合
     * @return 流程实例集合
     */
    default List<FlowInstance> getByDefIds(List<Long> defIds) {
        LambdaQueryWrapper<FlowInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FlowInstance::getDefinitionId, defIds);
        return selectList(queryWrapper);
    }
}
