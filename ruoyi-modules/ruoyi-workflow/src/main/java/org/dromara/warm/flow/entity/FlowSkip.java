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
package org.dromara.warm.flow.entity;
import org.dromara.warm.flow.entity.RootEntity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dromara.warm.flow.entity.FlowSkip;

import java.util.Date;
/**
 * 节点跳转关联
 */
@Data
@Accessors(chain = true)
public class FlowSkip implements RootEntity {

    /**
     * 主键
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 删除标记
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;

    /**
     * 流程id
     */
    private Long definitionId;

    /**
     * 节点id
     */
    @TableField(exist = false)
    private Long nodeId;

    /**
     * 当前流程节点的编码
     */
    private String nowNodeCode;

    /**
     * 当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nowNodeType;

    /**
     * 下一个流程节点的编码
     */
    private String nextNodeCode;

    /**
     * 下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nextNodeType;

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
     * 复制当前对象（不含主键、时间字段）
     */
    public FlowSkip copy() {
        return new FlowSkip()
            .setDelFlag(getDelFlag())
            .setDefinitionId(getDefinitionId())
            .setNowNodeCode(getNowNodeCode())
            .setNowNodeType(getNowNodeType())
            .setNextNodeCode(getNextNodeCode())
            .setNextNodeType(getNextNodeType())
            .setSkipName(getSkipName())
            .setSkipType(getSkipType())
            .setSkipCondition(getSkipCondition())
            .setCoordinate(getCoordinate());
    }
}
