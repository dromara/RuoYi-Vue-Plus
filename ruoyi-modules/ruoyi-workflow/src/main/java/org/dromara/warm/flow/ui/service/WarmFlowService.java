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
package org.dromara.warm.flow.ui.service;

import cn.hutool.core.util.StrUtil;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.config.WarmFlowProperties;
import org.dromara.warm.flow.dto.*;
import org.dromara.warm.flow.entity.FlowInstance;
import org.dromara.warm.flow.enums.FormCustomEnum;
import org.dromara.warm.flow.enums.ModelEnum;
import org.dromara.warm.flow.exception.FlowException;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;

import org.dromara.warm.flow.ui.dto.HandlerFeedBackDto;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.utils.TreeUtil;
import org.dromara.warm.flow.ui.vo.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设计器Controller 可选择是否放行，放行可与业务系统共享权限，主要是用来访问业务系统数据
 *
 * @author warm
 */
@Slf4j
public class WarmFlowService {

    /**
     * 返回流程定义的配置
     *
     * @return R<WarmFlowVo>
     */
    public static R<WarmFlowVo> config() {
        WarmFlowVo warmFlowVo = new WarmFlowVo();
        WarmFlowProperties warmFlow = FlowEngine.getFlowConfig();
        warmFlowVo.setFramework(warmFlow.getFramework().name());
        // 获取tokenName
        String tokenName = warmFlow.getTokenName();
        if (StrUtil.isEmpty(tokenName)) {
            return R.fail("未配置tokenName");
        }
        String[] tokenNames = tokenName.split(",");
        List<String> tokenNameList = Arrays.stream(tokenNames).filter(StrUtil::isNotEmpty)
            .map(String::trim).toList();
        warmFlowVo.setTokenNameList(tokenNameList);

        return R.ok(warmFlowVo);
    }

    /**
     * 保存流程json字符串
     *
     * @param defJson      流程数据集合
     * @param onlyNodeSkip 是否只保存节点和跳转
     * @return R<Void>
     * @throws Exception 异常
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    public static R<Void> saveJson(DefJson defJson, boolean onlyNodeSkip) throws Exception {
        FlowEngine.defService().saveDef(defJson, onlyNodeSkip);
        return R.ok();
    }

    /**
     * 获取流程定义数据(包含节点和跳转)
     *
     * @param id 流程定义id
     * @return R<DefVo>
     * @author xiarg
     * @since 2024/10/29 16:31
     */
    public static R<DefJson> queryDef(Long id) {
        try {
            DefJson defJson;
            if (id == null) {
                defJson = new DefJson()
                    .setModelValue(ModelEnum.CLASSICS.name())
                    .setFormCustom(FormCustomEnum.N.name());
            } else {
                defJson = FlowEngine.defService().queryDesign(id);
            }
            CategoryService categoryService = SpringUtils.getBeanOrNull(CategoryService.class);
            if (categoryService != null) {
                List<Tree> treeList = categoryService.queryCategory();
                defJson.setCategoryList(TreeUtil.buildTree(treeList));
            }
            return R.ok(defJson);
        } catch (Exception e) {
            log.error("获取流程json字符串", e);
            throw new FlowException("获取流程json字符串失败", e);
        }
    }

    /**
     * 获取流程图
     *
     * @param id 流程实例id
     * @return R<DefJson>
     */
    public static R<DefJson> queryFlowChart(Long id) {
        try {
            FlowInstance instance = FlowEngine.insService().getById(id);
            String defJsonStr = instance.getDefJson();
            DefJson defJson = JsonUtils.parseObject(defJsonStr, DefJson.class);
            defJson.setInstance(instance);

            // 获取流程图三原色
            defJson.setChartStatusColor(FlowEngine.chartService().getChartRgb(defJson.getModelValue()));
            // 是否显示流程图顶部文字
            defJson.setTopTextShow(FlowEngine.getFlowConfig().isTopTextShow());
            // 需要业务系统实现该接口
            ChartExtService chartExtService = SpringUtils.getBeanOrNull(ChartExtService.class);
            if (chartExtService != null) {
                chartExtService.initPromptContent(defJson);
                chartExtService.execute(defJson);
            }

            return R.ok(defJson);
        } catch (Exception e) {
            log.error("获取流程图", e);
            throw new FlowException("获取流程图失败", e);
        }
    }

    /**
     * 办理人权限设置列表tabs页签
     *
     * @return List<String>
     */
    public static R<List<String>> handlerType() {
        try {
            // 需要业务系统实现该接口
            HandlerSelectService handlerSelectService = SpringUtils.getBeanOrNull(HandlerSelectService.class);
            if (handlerSelectService == null) {
                return R.ok(Collections.emptyList());
            }
            List<String> handlerType = handlerSelectService.getHandlerType();
            return R.ok(handlerType);
        } catch (Exception e) {
            log.error("办理人权限设置列表tabs页签异常", e);
            throw new FlowException("办理人权限设置列表tabs页签失败", e);
        }
    }

    /**
     * 办理人权限设置列表结果
     *
     * @return HandlerSelectVo
     */
    public static R<HandlerSelectVo> handlerResult(HandlerQuery query) {
        try {
            // 需要业务系统实现该接口
            HandlerSelectService handlerSelectService = SpringUtils.getBeanOrNull(HandlerSelectService.class);
            if (handlerSelectService == null) {
                return R.ok(new HandlerSelectVo());
            }
            HandlerSelectVo handlerSelectVo = handlerSelectService.getHandlerSelect(query);
            return R.ok(handlerSelectVo);
        } catch (Exception e) {
            log.error("办理人权限设置列表结果异常", e);
            throw new FlowException("办理人权限设置列表结果失败", e);
        }
    }

    /**
     * 办理人权限名称回显
     *
     * @return HandlerSelectVo
     */
    public static R<List<HandlerFeedBackVo>> handlerFeedback(HandlerFeedBackDto handlerFeedBackDto) {
        try {
            // 需要业务系统实现该接口
            HandlerSelectService handlerSelectService = SpringUtils.getBeanOrNull(HandlerSelectService.class);
            if (handlerSelectService == null) {
                List<HandlerFeedBackVo> handlerFeedBackVos = StreamUtils.toList(handlerFeedBackDto.getStorageIds(),
                    storageId -> new HandlerFeedBackVo(storageId, null));
                return R.ok(handlerFeedBackVos);
            }
            List<HandlerFeedBackVo> handlerFeedBackVos = handlerSelectService.handlerFeedback(handlerFeedBackDto.getStorageIds());
            return R.ok(handlerFeedBackVos);
        } catch (Exception e) {
            log.error("办理人权限名称回显", e);
            throw new FlowException("办理人权限名称回显", e);
        }
    }

    /**
     * 办理人选择项
     *
     * @return List<Dict>
     */
    public static R<List<Dict>> handlerDict() {
        try {
            // 需要业务系统实现该接口
            HandlerDictService handlerDictService = SpringUtils.getBeanOrNull(HandlerDictService.class);
            if (handlerDictService == null) {
                List<Dict> dictList = new ArrayList<>();
                Dict dict = new Dict();
                dict.setLabel("默认表达式");
                dict.setValue("${handler}");
                Dict dict1 = new Dict();
                dict1.setLabel("spel表达式");
                dict1.setValue("#{@user.evalVar(#handler)}");
                Dict dict2 = new Dict();
                dict2.setLabel("其他");
                dict2.setValue("");
                dictList.add(dict);
                dictList.add(dict1);
                dictList.add(dict2);

                return R.ok(dictList);
            }
            return R.ok(handlerDictService.getHandlerDict());
        } catch (Exception e) {
            log.error("办理人权限设置列表结果异常", e);
            throw new FlowException("办理人权限设置列表结果失败", e);
        }
    }

    /**
     * 根据任务id获取待办任务表单及数据
     *
     * @param taskId 当前任务id
     * @return {@link R<FlowDto>}
     * @author liangli
     * @date 2024/8/21 17:08
     **/
    public static R<FlowDto> load(Long taskId) {
        FlowParams flowParams = FlowParams.build();

        return R.ok(FlowEngine.taskService().load(taskId, flowParams));
    }

    /**
     * 根据任务id获取已办任务表单及数据
     *
     * @param hisTaskId
     * @return
     */
    public static R<FlowDto> hisLoad(Long hisTaskId) {
        FlowParams flowParams = FlowParams.build();

        return R.ok(FlowEngine.taskService().hisLoad(hisTaskId, flowParams));
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
    public static R<FlowInstance> handle(Map<String, Object> formData, Long taskId, String skipType
        , String message, String nodeCode) {
        FlowParams flowParams = FlowParams.build()
            .skipType(skipType)
            .nodeCode(nodeCode)
            .message(message);

        flowParams.formData(formData);

        return R.ok(FlowEngine.taskService().skip(taskId, flowParams));
    }

    /**
     * 获取节点扩展属性
     *
     * @return List<NodeExt>
     */
    public static R<List<NodeExt>> nodeExt() {
        try {
            // 需要业务系统实现该接口
            NodeExtService nodeExtService = SpringUtils.getBeanOrNull(NodeExtService.class);
            if (nodeExtService == null) {
                return R.ok(Collections.emptyList());
            }
            List<NodeExt> nodeExts = nodeExtService.getNodeExt();
            return R.ok(nodeExts);
        } catch (Exception e) {
            log.error("获取节点扩展属性", e);
            throw new FlowException("获取节点扩展属性失败", e);
        }
    }

    /**
     * 获取监听器列表
     *
     * @return List<NodeExt>
     */
    public static R<List<ListenerVo>> listenerList() {
        try {
            // 需要业务系统实现该接口
            ListenerListService listenerListService = SpringUtils.getBeanOrNull(ListenerListService.class);
            if (listenerListService == null) {
                return R.ok(Collections.emptyList());
            }
            List<ListenerVo> listenerList = listenerListService.listenerList();
            return R.ok(listenerList);
        } catch (Exception e) {
            log.error("获取监听器列表", e);
            throw new FlowException("获取监听器列表失败", e);
        }
    }

}
