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
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * 流程定义
 */
@Data
@Accessors(chain = true)
public class FlowDefinition implements RootEntity {

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
     *
     * 删除标记
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;

    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）
     */
    private String modelValue;

    /**
     * 流程类别
     */
    private String category;

    /**
     * 流程版本
     */
    private String version;

    /**
     * 是否发布（0未开启 1开启）
     */
    private Integer isPublish;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Integer activityStatus;

    /**
     * 监听器类型
     */
    private String listenerType;

    /**
     * 监听器路径
     */
    private String listenerPath;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String ext;

    @TableField(exist = false)
    private List<FlowNode> nodeList = new ArrayList<>();

    @TableField(exist = false)
    private List<FlowUser> userList = new ArrayList<>();


    /**
     * 复制当前对象（不含主键、时间字段）
     */
    public FlowDefinition copy() {
        return new FlowDefinition()
            .setDelFlag(this.getDelFlag())
            .setFlowCode(this.getFlowCode())
            .setFlowName(this.getFlowName())
            .setModelValue(this.getModelValue())
            .setCategory(this.getCategory())
            .setVersion(this.getVersion())
            .setFormCustom(this.getFormCustom())
            .setFormPath(this.getFormPath())
            .setListenerType(this.getListenerType())
            .setListenerPath(this.getListenerPath())
            .setExt(this.getExt())
            .setCreateBy(this.getCreateBy())
            .setUpdateBy(this.getUpdateBy());
    }
}
