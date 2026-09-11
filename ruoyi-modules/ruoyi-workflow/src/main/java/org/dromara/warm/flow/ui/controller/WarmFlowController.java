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
package org.dromara.warm.flow.ui.controller;

import org.dromara.warm.flow.dto.ApiResult;
import org.dromara.warm.flow.dto.DefJson;
import org.dromara.warm.flow.dto.FlowDto;
import org.dromara.warm.flow.entity.FlowInstance;
import org.dromara.warm.flow.ui.dto.HandlerFeedBackDto;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.service.WarmFlowService;
import org.dromara.warm.flow.ui.vo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设计器Controller 可选择是否放行，放行可与业务系统共享权限，主要是用来访问业务系统数据
 *
 * @author warm
 */
@RestController
@RequestMapping("/warm-flow")
public class WarmFlowController {

    /**
     * 保存流程json字符串
     *
     * @param defJson 流程数据集合
     * @return ApiResult<Void>
     * @throws Exception 异常
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    @PostMapping("/save-json")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Void> saveJson(@RequestBody DefJson defJson, @RequestHeader("onlyNodeSkip") boolean onlyNodeSkip) throws Exception {
        return WarmFlowService.saveJson(defJson, onlyNodeSkip);
    }

    /**
     * 获取流程定义数据(包含节点和跳转)
     *
     * @param id 流程定义id
     * @return ApiResult<DefVo>
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    @GetMapping(value = {"/query-def", "/query-def/{id}"})
    public ApiResult<DefJson> queryDef(@PathVariable(value = "id", required = false) Long id) {
        return WarmFlowService.queryDef(id);
    }

    /**
     * 获取流程图
     *
     * @param id 流程实例id
     * @return ApiResult<DefJson>
     */
    @GetMapping("/query-flow-chart/{id}")
    public ApiResult<DefJson> queryFlowChart(@PathVariable("id") Long id) {
        return WarmFlowService.queryFlowChart(id);
    }

    /**
     * 办理人权限设置列表tabs页签
     *
     * @return List<String>
     */
    @GetMapping("/handler-type")
    public ApiResult<List<String>> handlerType() {
        return WarmFlowService.handlerType();
    }

    /**
     * 办理人权限设置列表结果
     *
     * @return HandlerSelectVo
     */
    @GetMapping("/handler-result")
    public ApiResult<HandlerSelectVo> handlerResult(HandlerQuery query) {
        return WarmFlowService.handlerResult(query);
    }

    /**
     * 办理人权限名称回显
     *
     * @return HandlerSelectVo
     */
    @GetMapping("/handler-feedback")
    public ApiResult<List<HandlerFeedBackVo>> handlerFeedback(HandlerFeedBackDto handlerFeedBackDto) {
        return WarmFlowService.handlerFeedback(handlerFeedBackDto);
    }

    /**
     * 办理人选择项
     *
     * @return List<Dict>
     */
    @GetMapping("/handler-dict")
    public ApiResult<List<Dict>> handlerDict() {
        return WarmFlowService.handlerDict();
    }

    /**
     * 根据任务id获取待办任务表单及数据
     *
     * @param taskId 当前任务id
     * @return {@link ApiResult< FlowDto >}
     * @author liangli
     * @date 2024/8/21 17:08
     **/
    @GetMapping(value = "/execute/load/{taskId}")
    public ApiResult<FlowDto> load(@PathVariable("taskId") Long taskId) {
        return WarmFlowService.load(taskId);
    }

    /**
     * 根据任务id获取已办任务表单及数据
     *
     * @param hisTaskId
     * @return
     */
    @GetMapping(value = "/execute/hisLoad/{taskId}")
    public ApiResult<FlowDto> hisLoad(@PathVariable("taskId") Long hisTaskId) {
        return WarmFlowService.hisLoad(hisTaskId);
    }

    /**
     * 通用表单流程审批接口
     *
     * @param formData
     * @param taskId
     * @param skipType
     * @param message
     * @param nodeCode
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @PostMapping(value = "/execute/handle")
    public ApiResult<FlowInstance> handle(@RequestBody Map<String, Object> formData, @RequestParam("taskId") Long taskId
        , @RequestParam("skipType") String skipType, @RequestParam("message") String message
        , @RequestParam(value = "nodeCode", required = false) String nodeCode) {
        return WarmFlowService.handle(formData, taskId, skipType, message, nodeCode);
    }

    /**
     * 获取节点扩展属性
     *
     * @return List<NodeExt>
     */
    @GetMapping("/node-ext")
    public ApiResult<List<NodeExt>> nodeExt() {
        return WarmFlowService.nodeExt();
    }

    /**
     * 获取节点扩展属性
     *
     * @return List<NodeExt>
     */
    @GetMapping("/listener-list")
    public ApiResult<List<ListenerVo>> listenerList() {
        return WarmFlowService.listenerList();
    }

}
