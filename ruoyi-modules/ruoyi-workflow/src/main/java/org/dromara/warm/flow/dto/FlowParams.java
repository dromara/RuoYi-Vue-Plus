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
import java.io.Serial;

import org.dromara.common.json.utils.JsonUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import lombok.Getter;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.handler.PermissionHandler;



import java.io.Serializable;
import java.util.*;

/**
 * 工作流内置参数
 *
 * @author warm
 * @since 2023/3/31 17:18
 */
@Getter
public class FlowParams implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 当前办理人唯一标识：就是确定唯一用的，如用户id，通常用来入库，记录流程实例创建人，办理人
     */
    private String handler;

    /**
     * 节点编码（如果要指定跳转节点，传入）
     */
    private String nodeCode;

    /**
     * 用户权限标识：和办理权限有关，是否有办理权限，通俗来说，就是设计器里面预设的办理人，和这个标识是否有交集，有交集就可以办理，审批的时候，就不会提示报错
     */
    private List<String> permissionFlag;

    /**
     * 跳转类型（PASS审批通过 REJECT退回）
     */
    private String skipType;

    /**
     * 审批意见
     */
    private String message;

    /**
     * 流程变量
     */
    private Map<String, Object> variable = new HashMap<>();

    /**
     * 流程实例状态
     */
    private String flowStatus;

    /**
     * 历史任务表状态
     */
    private String hisStatus;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Integer activityStatus;

    /**
     * 协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)
     */
    private Integer cooperateType;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String ext;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String hisTaskExt;

    /**
     * 增加办理人：加签，转办，委托
     */
    private List<String> addHandlers;

    /**
     * 减少办理人：减签，委托
     */
    private List<String> reductionHandlers;

    /**
     * 忽略-办理权限校验（true：忽略，false：不忽略）
     */
    private boolean ignore;

    /**
     * 忽略-委派处理（true：忽略，false：不忽略）
     */
    private boolean ignoreDepute;

    /**
     * 忽略-会签票签处理（true：忽略，false：不忽略）
     */
    private boolean ignoreCooperate;

    /**
     * 执行的下个任务的办理人
     */
    private String[] nextHandler;

    /**
     * 下个任务处理人配置类型（true-追加，false-覆盖，默认false）
     */
    private boolean nextHandlerAppend;

    public FlowParams() {
    }

    public FlowParams(String skipType, String message, Map<String, Object> variable) {
        this.skipType = skipType;
        this.message = message;
        this.variable = variable;
    }

    public FlowParams(String nodeCode, String skipType, String message, Map<String, Object> variable) {
        this.nodeCode = nodeCode;
        this.skipType = skipType;
        this.message = message;
        this.variable = variable;
    }

    public FlowParams(String skipType, String message, Map<String, Object> variable
        , String flowStatus, String hisStatus) {
        this.skipType = skipType;
        this.message = message;
        this.variable = variable;
        this.flowStatus = flowStatus;
        this.hisStatus = hisStatus;
    }

    public FlowParams(String nodeCode, String skipType, String message, Map<String, Object> variable
        , String flowStatus, String hisStatus) {
        this.nodeCode = nodeCode;
        this.skipType = skipType;
        this.message = message;
        this.variable = variable;
        this.flowStatus = flowStatus;
        this.hisStatus = hisStatus;
    }

    public static FlowParams build() {
        return new FlowParams();
    }

    public FlowParams flowCode(String flowCode) {
        this.flowCode = flowCode;
        return this;
    }

    public FlowParams handler(String handler) {
        this.handler = handler;
        return this;
    }

    public FlowParams nodeCode(String nodeCode) {
        this.nodeCode = nodeCode;
        return this;
    }

    public FlowParams permissionFlag(List<String> permissionFlag) {
        this.permissionFlag = permissionFlag;
        return this;
    }

    public FlowParams message(String message) {
        this.message = message;
        return this;
    }

    public FlowParams variable(Map<String, Object> variable) {
        this.variable = variable;
        return this;
    }

    public FlowParams flowStatus(String flowStatus) {
        this.flowStatus = flowStatus;
        return this;
    }

    public FlowParams hisStatus(String hisStatus) {
        this.hisStatus = hisStatus;
        return this;
    }

    public FlowParams activityStatus(Integer activityStatus) {
        this.activityStatus = activityStatus;
        return this;
    }

    public FlowParams cooperateType(Integer cooperateType) {
        this.cooperateType = cooperateType;
        return this;
    }

    public FlowParams ext(String ext) {
        this.ext = ext;
        return this;
    }

    public FlowParams hisTaskExt(String hisTaskExt) {
        this.hisTaskExt = hisTaskExt;
        return this;
    }

    public FlowParams nextHandler(String... nextHandler) {
        // 如果外部传递 null , 就忽略 , 维持节点本身审批,防止 null 数据变成 [null]
        if (nextHandler == null) {
            return this;
        }
        this.nextHandler = Arrays.stream(nextHandler).filter(Objects::nonNull).toArray(String[]::new);
        return this;
    }

    public String getVariableStr() {
        return JsonUtils.toJsonString(variable);
    }

    public String getHandler() {
        if (StrUtil.isEmpty(handler)) {
            PermissionHandler permissionHandler = FlowEngine.permissionHandler();
            if (permissionHandler != null) {
                try {
                    handler = permissionHandler.getHandler();
                } catch (Exception ignored) {
                }
            }
        }
        return handler;
    }

    public List<String> getPermissionFlag() {
        if (CollUtil.isEmpty(permissionFlag)) {
            PermissionHandler permissionHandler = FlowEngine.permissionHandler();
            if (permissionHandler != null) {
                try {
                    permissionFlag = permissionHandler.permissions();
                } catch (Exception ignored) {
                }
            }
        }
        return permissionFlag;
    }

    public FlowParams skipType(String skipType) {
        this.skipType = skipType;
        return this;
    }

    public FlowParams addHandlers(List<String> addHandlers) {
        this.addHandlers = addHandlers;
        return this;
    }

    public FlowParams reductionHandlers(List<String> reductionHandlers) {
        this.reductionHandlers = reductionHandlers;
        return this;
    }

    public FlowParams ignore(boolean ignore) {
        this.ignore = ignore;
        return this;
    }

    public FlowParams ignoreDepute(boolean ignoreDepute) {
        this.ignoreDepute = ignoreDepute;
        return this;
    }

    public FlowParams ignoreCooperate(boolean ignoreCooperate) {
        this.ignoreCooperate = ignoreCooperate;
        return this;
    }

    public FlowParams nextHandlerAppend(boolean nextHandlerAppend) {
        this.nextHandlerAppend = nextHandlerAppend;
        return this;
    }

    public FlowParams formData(Map<String, Object> formData) {
        if (this.variable == null) {
            this.variable = new HashMap<>();
        }
        this.variable.put("formData", formData);
        return this;
    }

}
