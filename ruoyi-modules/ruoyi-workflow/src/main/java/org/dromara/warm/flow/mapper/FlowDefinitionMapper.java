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
import org.dromara.warm.flow.entity.FlowDefinition;

import java.util.List;

/**
 * 流程定义Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowDefinitionMapper extends WarmMapper<FlowDefinition> {

    /**
     * 根据编码批量查询
     *
     * @param flowCodeList 流程编码集
     * @return 查询结果
     */
    default List<FlowDefinition> queryByCodeList(List<String> flowCodeList) {
        LambdaQueryWrapper<FlowDefinition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FlowDefinition::getFlowCode, flowCodeList);
        return selectList(queryWrapper);
    }

    /**
     * 根据ID批量修改发布状态
     *
     * @param ids           ids
     * @param publishStatus 发布状态(9=已失效；0=未发布；1=已发布)
     * @see org.dromara.warm.flow.enums.PublishStatus
     */
    default void updatePublishStatus(List<Long> ids, Integer publishStatus) {
        LambdaQueryWrapper<FlowDefinition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FlowDefinition::getId, ids);
        update(new FlowDefinition().setIsPublish(publishStatus), queryWrapper);
    }
}
