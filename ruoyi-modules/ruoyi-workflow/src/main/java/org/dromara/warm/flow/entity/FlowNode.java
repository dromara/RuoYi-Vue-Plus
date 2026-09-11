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
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * 流程节点
 */
@Data
@Accessors(chain = true)
public class FlowNode implements RootEntity {

    /**
     * 节点跳转列表（非表字段）
     */
    @TableField(exist = false)
    List<FlowSkip> skipList;

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
    @TableLogic
    private String delFlag;

    /**
     * 节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）
     */
    private Integer nodeType;
    /**
     * 流程id
     */
    private Long definitionId;
    /**
     * 流程节点编码   每个流程的nodeCode是唯一的,即definitionId+nodeCode唯一,在数据库层面做了控制
     */
    private String nodeCode;
    /**
     * 流程节点名称
     */
    private String nodeName;
    /**
     * 权限标识（权限类型:权限标识，可以多个，用@@隔开)
     */
    private String permissionFlag;
    /**
     * 流程签署比例值
     */
    private String nodeRatio;
    /**
     * 流程节点坐标
     */
    private String coordinate;
    /**
     * 任意结点跳转
     */
    private String anyNodeSkip;
    /**
     * 监听器类型
     */
    private String listenerType;
    /**
     * 监听器路径
     */
    private String listenerPath;
    /**
     * 审批表单是否自定义（Y=是 N=否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 节点扩展属性
     */
    private String ext;


    /**
     * 复制当前对象（不含主键、时间字段）
     */
    public FlowNode copy() {
        return new FlowNode()
            .setDelFlag(this.getDelFlag())
            .setNodeType(this.getNodeType())
            .setDefinitionId(this.getDefinitionId())
            .setNodeCode(this.getNodeCode())
            .setNodeName(this.getNodeName())
            .setNodeRatio(this.getNodeRatio())
            .setPermissionFlag(this.getPermissionFlag())
            .setCoordinate(this.getCoordinate())
            .setAnyNodeSkip(this.getAnyNodeSkip())
            .setListenerType(this.getListenerType())
            .setListenerPath(this.getListenerPath())
            .setFormCustom(this.getFormCustom())
            .setFormPath(this.getFormPath())
            .setExt(this.getExt());
    }
}
