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

import java.util.List;
import java.util.Map;

/**
 * 节点跳转关联对象Vo
 *
 * @author warm
 * @since 2023-03-29
 */
@Setter
@Getter
@Accessors(chain = true)
public class SkipJson {

    /**
     * 当前流程节点的编码
     */
    private String nowNodeCode;

    /**
     * 下一个流程节点的编码
     */
    private String nextNodeCode;

    /**
     * 跳转名称
     */
    private String skipName;

    /**
     * 跳转类型（PASS审批通过 REJECT退回）
     */
    private String skipType;

    /**
     * 跳转条件
     */
    private String skipCondition;

    /**
     * 流程跳转坐标
     */
    private String coordinate;

    /**
     * 办理状态: 0未办理 1待办理 2已办理
     */
    private Integer status;

    /**
     * 扩展map，保存业务自定义扩展属性
     */
    private Map<String, Object> extMap;

    /**
     * 流程图节点提示内容
     */
    private List<String> promptContent;

    private String createBy;

    private String updateBy;

}
