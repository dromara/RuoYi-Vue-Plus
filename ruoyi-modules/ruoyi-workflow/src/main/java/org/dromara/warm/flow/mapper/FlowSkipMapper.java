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
import org.dromara.warm.flow.entity.FlowSkip;

import java.io.Serializable;
import java.util.Collection;

/**
 * 节点跳转关联Mapper接口
 *
 * @author warm
 * @since 2023-03-29
 */
public interface FlowSkipMapper extends WarmMapper<FlowSkip> {

    /**
     * 批量删除节点跳转关联
     *
     * @param defIds 需要删除的数据主键集合
     * @return 结果
     */
    default int deleteSkipByDefIds(Collection<? extends Serializable> defIds) {
        return delete(new LambdaQueryWrapper<FlowSkip>().in(FlowSkip::getDefinitionId, defIds));
    }
}
