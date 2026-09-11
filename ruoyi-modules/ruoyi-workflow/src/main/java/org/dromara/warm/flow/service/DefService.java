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
package org.dromara.warm.flow.service;
import org.dromara.common.json.utils.JsonUtils;

import org.dromara.warm.flow.entity.*;

import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.constant.ExceptionCons;
import org.dromara.warm.flow.dto.DefJson;
import org.dromara.warm.flow.dto.FlowCombine;
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowInstance;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;
import org.dromara.warm.flow.enums.ActivityStatus;
import org.dromara.warm.flow.enums.PublishStatus;
import org.dromara.warm.flow.exception.FlowException;
import org.dromara.warm.flow.service.WarmServiceImpl;
import org.dromara.warm.flow.utils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;



import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowDefinitionMapper;

import java.util.List;


/**
 * 流程定义Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
@Slf4j
@Service
public class DefService extends WarmServiceImpl<FlowDefinition> {
    public FlowDefinition importIs(InputStream is) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new FlowException(ExceptionCons.READ_IS_ERROR);
        }
        return importJson(stringBuilder.toString());
    }

    public FlowDefinition importJson(String defJson) {
        return importDef(JsonUtils.parseObject(defJson, DefJson.class));
    }

    public FlowDefinition importDef(DefJson defJson) {
        FlowDefinition definition = DefJson.copyDef(defJson);
        FlowCombine flowCombine = FlowConfigUtil.structureFlow(definition);
        return insertFlow(flowCombine.getDefinition(), flowCombine.getAllNodes(), flowCombine.getAllSkips());
    }

    public FlowDefinition insertFlow(FlowDefinition definition, List<FlowNode> nodeList, List<FlowSkip> skipList) {
        definition.setVersion(getNewVersion(definition));
        for (FlowNode node : nodeList) {
            node.setVersion(definition.getVersion());
        }
        FlowEngine.defService().save(definition);
        FlowEngine.nodeService().saveBatch(nodeList);
        FlowEngine.skipService().saveBatch(skipList);
        return definition;
    }

    public boolean checkAndSave(FlowDefinition definition) {
        return save(definition.setVersion(getNewVersion(definition)));
    }

    public void saveDef(DefJson defJson, boolean onlyNodeSkip) {
        if (ObjectUtil.isNull(defJson)) {
            return;
        }
        FlowCombine flowCombine = DefJson.copyCombine(defJson);
        FlowDefinition definition = flowCombine.getDefinition();
        Long id = definition.getId();
        // 如果是新增的流程定义
        if (ObjectUtil.isNull(id)) {
            definition.setVersion(getNewVersion(definition));
            FlowEngine.dataFillHandler().idFill(definition);
        }

        // 校验流程定义合法性
        checkFlowLegal(flowCombine);

        // 如果是新增的流程定义
        if (ObjectUtil.isNull(id)) {
            FlowEngine.defService().save(definition);
        } else {
            if (!onlyNodeSkip) {
                FlowEngine.defService().updateById(definition);
            }
            // 删除所有节点和连线
            FlowEngine.nodeService().remove(new FlowNode().setDefinitionId(id));
            FlowEngine.skipService().remove(new FlowSkip().setDefinitionId(id));
        }

        // 保存流程节点和跳转
        List<FlowNode> allNodes = flowCombine.getAllNodes();
        allNodes.forEach(node -> {
            if(StrUtil.isEmpty(node.getNodeRatio())) {
                node.setNodeRatio("0");
            }
        });
        // 所有的流程连线
        List<FlowSkip> allSkips = flowCombine.getAllSkips();

        // 保存节点，流程连线，权利人
        FlowEngine.nodeService().saveBatch(allNodes);
        FlowEngine.skipService().saveBatch(allSkips);
    }

    public String exportJson(Long id) {
        return JsonUtils.toJsonString(queryDesign(id).setIsPublish(null));
    }

    public FlowDefinition getAllDataDefinition(Long id) {
        FlowDefinition definition = getMapper().selectById(id);
        List<FlowNode> nodeList = FlowEngine.nodeService().getByDefId(id);
        definition.setNodeList(nodeList);
        List<FlowSkip> skips = FlowEngine.skipService().getByDefId(id);
        Map<String, List<FlowSkip>> flowSkipMap = StreamUtils.groupByKey(skips, FlowSkip::getNowNodeCode);
        nodeList.forEach(flowNode -> flowNode.setSkipList(flowSkipMap.get(flowNode.getNodeCode())));
        return definition;
    }

    public FlowCombine getFlowCombine(Long id) {
        return getFlowCombine(getMapper().selectById(id));
    }

    public FlowCombine getFlowCombineNoDef(Long id) {
        FlowCombine flowCombine = new FlowCombine();
        flowCombine.setAllNodes(FlowEngine.nodeService().getByDefId(id));
        flowCombine.setAllSkips(FlowEngine.skipService().getByDefId(id));
        return flowCombine;
    }

    public FlowCombine getFlowCombine(FlowDefinition definition) {
        FlowCombine flowCombine = getFlowCombineNoDef(definition.getId());
        flowCombine.setDefinition(definition);
        return flowCombine;
    }

    public DefJson queryDesign(Long id) {
        return DefJson.copyDef(getAllDataDefinition(id));
    }

    public List<FlowDefinition> queryByCodeList(List<String> flowCodeList) {
        return getMapper().queryByCodeList(flowCodeList);
    }

    public void updatePublishStatus(List<Long> ids, Integer publishStatus) {
        getMapper().updatePublishStatus(ids, publishStatus);
    }

    /**
     * 删除流程定义
     *
     * @param ids 流程定义id
     */
    public boolean removeDef(List<Long> ids) {
        ids.forEach(id -> {
            List<FlowInstance> instances = FlowEngine.insService().getByDefId(id);
            AssertUtil.isNotEmpty(instances, ExceptionCons.EXIST_START_TASK);
        });
        FlowEngine.nodeService().deleteNodeByDefIds(ids);
        FlowEngine.skipService().deleteSkipByDefIds(ids);
        return removeByIds(ids);
    }

    public boolean publish(Long id) {
        List<FlowNode> nodeList = FlowEngine.nodeService().getByDefId(id);
        AssertUtil.isEmpty(nodeList, ExceptionCons.NOT_DRAW_FLOW_ERROR);
        FlowDefinition definition = getById(id);
        List<FlowDefinition> definitions = getByFlowCode(definition.getFlowCode());
        // 已发布流程定义，改为已失效或者未发布状态
        List<Long> otherDefIds = definitions.stream()
            .filter(item -> !Objects.equals(definition.getId(), item.getId())
                && PublishStatus.PUBLISHED.getKey().equals(item.getIsPublish()))
            .map(FlowDefinition::getId)
            .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(otherDefIds)) {
            List<FlowInstance> instanceList = FlowEngine.insService().listByDefIds(otherDefIds);
            if (CollUtil.isNotEmpty(instanceList)) {
                // 已发布已使用过的流程定义
                Set<Long> useDefIds = StreamUtils.toSet(instanceList, FlowInstance::getDefinitionId);
                if (CollUtil.isNotEmpty(useDefIds)) {
                    // 已发布已使用过的流程定义，改为已失效
                    getMapper().updatePublishStatus(new ArrayList<>(useDefIds), PublishStatus.EXPIRED.getKey());
                    // 过滤掉已发布已使用-->已发布未使用
                    otherDefIds.removeIf(useDefIds::contains);
                }
            }
            if (CollUtil.isNotEmpty(otherDefIds)) {
                // 已发布未使用过的流程定义，改为未发布
                getMapper().updatePublishStatus(otherDefIds, PublishStatus.UNPUBLISHED.getKey());
            }
        }

        FlowDefinition flowDefinition = new FlowDefinition();
        flowDefinition.setId(id);
        flowDefinition.setIsPublish(PublishStatus.PUBLISHED.getKey());
        return updateById(flowDefinition);
    }

    public boolean unPublish(Long id) {
        List<FlowInstance> instances = FlowEngine.insService().getByDefId(id);
        AssertUtil.isNotEmpty(instances, ExceptionCons.EXIST_START_TASK);
        FlowDefinition definition = new FlowDefinition().setId(id);
        definition.setIsPublish(PublishStatus.UNPUBLISHED.getKey());
        return updateById(definition);
    }

    public boolean copyDef(Long id) {
        FlowDefinition sourceDef = getById(id);
        AssertUtil.isNull(sourceDef, ExceptionCons.NOT_FOUNT_DEF);
        FlowDefinition definition = sourceDef.copy();
        definition.setVersion(getNewVersion(definition));

        List<FlowNode> nodeList = StreamUtils.toList(FlowEngine.nodeService().getByDefId(id), FlowNode::copy);
        List<FlowSkip> skipList = StreamUtils.toList(FlowEngine.skipService().getByDefId(id), FlowSkip::copy);
        FlowEngine.dataFillHandler().idFill(definition);

        nodeList.forEach(node -> node.setDefinitionId(definition.getId()).setVersion(definition.getVersion()));
        FlowEngine.nodeService().saveBatch(nodeList);

        skipList.forEach(skip -> skip.setDefinitionId(definition.getId()));
        FlowEngine.skipService().saveBatch(skipList);
        return save(definition);
    }

    public boolean active(Long id) {
        FlowDefinition definition = getById(id);
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);
        AssertUtil.isTrue(ActivityStatus.isActivity(definition.getActivityStatus()), ExceptionCons.DEFINITION_ALREADY_ACTIVITY);
        definition.setActivityStatus(ActivityStatus.ACTIVITY.getKey());
        return updateById(definition);
    }

    public boolean unActive(Long id) {
        FlowDefinition definition = getById(id);
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);
        AssertUtil.isTrue(ActivityStatus.isSuspended(definition.getActivityStatus()), ExceptionCons.DEFINITION_ALREADY_SUSPENDED);
        definition.setActivityStatus(ActivityStatus.SUSPENDED.getKey());
        return updateById(definition);
    }

    public List<FlowDefinition> getByFlowCode(String flowCode) {
        return list(new FlowDefinition().setFlowCode(flowCode));
    }

    public FlowDefinition getPublishByFlowCode(String flowCode) {
        return FlowEngine.defService().getOne(new FlowDefinition()
            .setFlowCode(flowCode).setIsPublish(PublishStatus.PUBLISHED.getKey()));
    }

    private String getNewVersion(FlowDefinition definition) {
        List<String> flowCodeList = Collections.singletonList(definition.getFlowCode());
        List<FlowDefinition> definitions = getMapper().queryByCodeList(flowCodeList);
        int highestVersion = 0;
        String latestNonPositiveVersion = null;
        long latestTimestamp = Long.MIN_VALUE;

        for (FlowDefinition otherDef : definitions) {
            if (definition.getFlowCode().equals(otherDef.getFlowCode())) {
                try {
                    int version = Integer.parseInt(otherDef.getVersion());
                    if (version > highestVersion) {
                        highestVersion = version;
                    }
                } catch (NumberFormatException e) {
                    long timestamp = otherDef.getCreateTime().getTime();
                    if (timestamp > latestTimestamp) {
                        latestTimestamp = timestamp;
                        latestNonPositiveVersion = otherDef.getVersion();
                    }
                }
            }
        }

        String version = "1";
        if (highestVersion > 0) {
            version = String.valueOf(highestVersion + 1);
        } else if (latestNonPositiveVersion != null) {
            version = latestNonPositiveVersion + "_1";
        }

        return version;
    }

    private void checkFlowLegal(FlowCombine flowCombine) {
        FlowDefinition definition = flowCombine.getDefinition();
        String flowName = definition.getFlowName();
        AssertUtil.isEmpty(definition.getFlowCode(), "【" + flowName + "】流程flowCode为空!");
        // 节点校验
        List<FlowNode> allNodes = flowCombine.getAllNodes();
        List<FlowSkip> allSkips = flowCombine.getAllSkips();
        Map<String, List<FlowSkip>> skipMap = StreamUtils.groupByKey(allSkips, FlowSkip::getNowNodeCode);
        allNodes.forEach(node -> {
            node.setSkipList(skipMap.get(node.getNodeCode()));
            skipMap.remove(node.getNodeCode());
        });
        AssertUtil.isNotEmpty(skipMap, "[" + flowName + "]" + ExceptionCons.FLOW_HAVE_USELESS_SKIP);
        // 每一个流程的开始节点个数
        Set<String> nodeCodeSet = new HashSet<>();
        // 便利一个流程中的各个节点
        int startNum = 0;
        for (FlowNode node : allNodes) {
            FlowConfigUtil.initNodeAndCondition(node, definition.getId(), definition.getVersion());
            startNum = FlowConfigUtil.checkStartAndSame(node, startNum, flowName, nodeCodeSet);
        }
        AssertUtil.isTrue(startNum == 0, "[" + flowName + "]" + ExceptionCons.LOST_START_NODE);
        // 校验跳转节点的合法性
        FlowConfigUtil.checkSkipNode(allSkips);
        // 校验所有目标节点是否都存在
        FlowConfigUtil.validaIsExistDestNode(allSkips, nodeCodeSet);
    }


    @Override
    public FlowDefinitionMapper getMapper() {
        return SpringUtils.getBean(FlowDefinitionMapper.class);
    }
}
