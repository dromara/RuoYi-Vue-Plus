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
import org.dromara.warm.flow.entity.FlowUser;

import java.util.Date;
/**
 * 流程用户
 */
@Data
@Accessors(chain = true)
public class FlowUser implements RootEntity {

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
     * 人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）
     */
    private String type;

    /**
     * 权限人
     */
    private String processedBy;

    /**
     * 任务表id
     */
    private Long associated;

}
