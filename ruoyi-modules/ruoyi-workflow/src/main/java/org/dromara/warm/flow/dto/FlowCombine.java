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
package org.dromara.warm.flow.dto;
import org.dromara.warm.flow.entity.*;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;

import java.util.ArrayList;
import java.util.List;


/**
 * 流程数据集合
 *
 * @author warm
 * @since 2023/3/30 14:27
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlowCombine {
    /**
     * 所有的流程定义
     */
    private FlowDefinition definition = new FlowDefinition();

    /**
     * 所有的流程节点
     */
    private List<FlowNode> allNodes = new ArrayList<>();

    /**
     * 所有的流程节点跳转关联
     */
    private List<FlowSkip> allSkips = new ArrayList<>();

}
