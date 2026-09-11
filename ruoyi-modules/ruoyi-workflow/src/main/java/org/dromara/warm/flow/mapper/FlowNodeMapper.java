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

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.warm.flow.entity.FlowNode;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 流程节点Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowNodeMapper extends WarmMapper<FlowNode> {

    default List<FlowNode> getByNodeCodes(List<String> nodeCodes, Long definitionId) {
        LambdaQueryWrapper<FlowNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CollUtil.isNotEmpty(nodeCodes), FlowNode::getNodeCode, nodeCodes)
            .eq(FlowNode::getDefinitionId, definitionId);
        return selectList(queryWrapper);
    }

    /**
     * 批量删除流程节点
     *
     * @param defIds 需要删除的数据主键集合
     * @return 结果
     */
    default int deleteNodeByDefIds(Collection<? extends Serializable> defIds) {
        return delete(new LambdaQueryWrapper<FlowNode>().in(FlowNode::getDefinitionId, defIds));
    }
}
