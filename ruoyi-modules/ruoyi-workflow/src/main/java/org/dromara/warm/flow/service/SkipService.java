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
import org.dromara.warm.flow.entity.*;

import org.dromara.warm.flow.entity.FlowSkip;
import org.dromara.warm.flow.service.WarmServiceImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;



import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowSkipMapper;




/**
 * 节点跳转关联Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
@Service
public class SkipService extends WarmServiceImpl<FlowSkip> {
    public int deleteSkipByDefIds(Collection<? extends Serializable> defIds) {
        return getMapper().deleteSkipByDefIds(defIds);
    }

    public List<FlowSkip> getByDefId(Long definitionId) {
        return list(new FlowSkip().setDefinitionId(definitionId));
    }

    public List<FlowSkip> getByDefIdAndNowNodeCode(Long definitionId, String nodeCode) {
        return list(new FlowSkip().setDefinitionId(definitionId).setNowNodeCode(nodeCode));
    }

    @Override
    public FlowSkipMapper getMapper() {
        return SpringUtils.getBean(FlowSkipMapper.class);
    }
}