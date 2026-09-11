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


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;

import java.util.ArrayList;
import java.util.List;


/**
 * 办理过程中途径数据，用于渲染流程图
 *
 * @author warm
 * @since 2025/1/4
 */
@Getter
@Setter
@Accessors(chain = true)
public class PathWayData {
    /**
     * 流程定义id
     */
    private Long defId;

    /**
     * 流程实例id
     */
    private Long insId;

    /**
     * 跳转类型（PASS审批通过 REJECT退回）
     */
    private String skipType;

    /**
     * 目标结点集合
     */
    private List<FlowNode> targetNodes = new ArrayList<>();

    /**
     * 途径结点集合
     */
    private List<FlowNode> pathWayNodes = new ArrayList<>();

    /**
     * 途径流程跳转线
     */
    private List<FlowSkip> pathWaySkips = new ArrayList<>();

}
