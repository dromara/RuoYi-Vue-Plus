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
package org.dromara.warm.flow.utils;

import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;

import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.constant.ExceptionCons;
import org.dromara.warm.flow.dto.FlowCombine;
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;
import org.dromara.warm.flow.enums.NodeType;
import org.dromara.warm.flow.enums.SkipType;

import java.util.*;

/**
 * 流程配置帮助类
 *
 * @author zhoukai
 */
public class FlowConfigUtil {
    /**
     * 无参构造
     */
    private FlowConfigUtil() {

    }

    public static FlowCombine structureFlow(FlowDefinition definition) {
        // 获取流程
        FlowCombine combine = new FlowCombine();
        // 流程定义
        combine.setDefinition(definition);
        // 所有的流程节点
        List<FlowNode> allNodes = combine.getAllNodes();
        // 所有的流程连线
        List<FlowSkip> allSkips = combine.getAllSkips();

        String flowName = definition.getFlowName();
        AssertUtil.isEmpty(definition.getFlowCode(), "【" + flowName + "】流程flowCode为空!");
        // 发布
        definition.setIsPublish(0);
        definition.setUpdateTime(new Date());
        FlowEngine.dataFillHandler().idFill(definition);

        List<FlowNode> nodeList = definition.getNodeList();
        // 每一个流程的开始节点个数
        int startNum = 0;
        Set<String> nodeCodeSet = new HashSet<>();
        // 遍历一个流程中的各个节点
        for (FlowNode node : nodeList) {
            initNodeAndCondition(node, definition.getId(), definition.getVersion());
            startNum = checkStartAndSame(node, startNum, flowName, nodeCodeSet);
            allNodes.add(node);
            allSkips.addAll(node.getSkipList());
        }
        Map<String, Integer> skipMap = StreamUtils.toMap(allNodes, FlowNode::getNodeCode, FlowNode::getNodeType);
        allSkips.forEach(allSkip -> allSkip.setNextNodeType(skipMap.get(allSkip.getNextNodeCode())));
        AssertUtil.isTrue(startNum == 0, "[" + flowName + "]" + ExceptionCons.LOST_START_NODE);
        // 校验跳转节点的合法性
        checkSkipNode(allSkips);
        // 校验所有目标节点是否都存在
        validaIsExistDestNode(allSkips, nodeCodeSet);
        return combine;
    }

    public static int checkStartAndSame(FlowNode node, int startNum, String flowName, Set<String> nodeCodeSet) {
        if (NodeType.isStart(node.getNodeType())) {
            startNum++;
            AssertUtil.isTrue(startNum > 1, "[" + flowName + "]" + ExceptionCons.MUL_START_NODE);
        }
        // 保证不存在重复的nodeCode
        AssertUtil.contains(nodeCodeSet, node.getNodeCode(),
            "【" + flowName + "】" + ExceptionCons.SAME_NODE_CODE);
        nodeCodeSet.add(node.getNodeCode());
        return startNum;
    }

    /**
     * 校验跳转节点的合法性
     *
     * @param allSkips
     */
    public static void checkSkipNode(List<FlowSkip> allSkips) {
        Map<String, List<FlowSkip>> allSkipMap = StreamUtils.groupByKey(allSkips, FlowSkip::getNowNodeCode);
        // 不可同时通过或者退回到多个中间节点，必须先流转到网关节点
        for (List<FlowSkip> values : allSkipMap.values()) {
            int passNum = 0;
            int rejectNum = 0;
            for (FlowSkip value : values) {
                if (NodeType.isBetween(value.getNowNodeType()) && NodeType.isBetween(value.getNextNodeType())) {
                    if (SkipType.isPass(value.getSkipType())) {
                        passNum++;
                    } else {
                        rejectNum++;
                    }
                }
            }
            AssertUtil.isTrue(passNum > 1 || rejectNum > 1, ExceptionCons.MUL_SKIP_BETWEEN);
        }
    }

    /**
     * 校验所有的目标节点是否存在
     *
     * @param allSkips
     * @param nodeCodeSet
     */
    public static void validaIsExistDestNode(List<FlowSkip> allSkips, Set<String> nodeCodeSet) {
        for (FlowSkip allSkip : allSkips) {
            String nextNodeCode = allSkip.getNextNodeCode();
            AssertUtil.isTrue(!nodeCodeSet.contains(nextNodeCode), "【" + nextNodeCode + "】" + ExceptionCons.NULL_NODE_CODE);
        }
    }


    /**
     * 读取工作节点和跳转条件
     *
     * @param node         node
     * @param definitionId definitionId
     * @param version      version
     */
    public static void initNodeAndCondition(FlowNode node, Long definitionId, String version) {
        String nodeName = node.getNodeName();
        String nodeCode = node.getNodeCode();
        List<FlowSkip> skipList = node.getSkipList();
        if (!NodeType.isEnd(node.getNodeType())) {
            AssertUtil.isEmpty(skipList, ExceptionCons.MUST_SKIP);
        }
        AssertUtil.isEmpty(nodeCode, "[" + nodeName + "]" + ExceptionCons.LOST_NODE_CODE);

        node.setVersion(version);
        node.setDefinitionId(definitionId);

        // 中间节点的集合， 跳转类型和目标节点不能重复
        Set<String> betweenSet = new HashSet<>();
        // 网关的集合 跳转条件和下目标节点不能重复
        Set<String> gateWaySet = new HashSet<>();
        int skipNum = 0;
        // 遍历节点下的跳转条件
        if (CollUtil.isEmpty(skipList)) {
            return;
        }
        for (FlowSkip skip : skipList) {
            if (NodeType.isStart(node.getNodeType())) {
                skipNum++;
                AssertUtil.isTrue(skipNum > 1, "[" + node.getNodeName() + "]" + ExceptionCons.MUL_START_SKIP);
            }
            AssertUtil.isEmpty(skip.getNextNodeCode(), "【" + nodeName + "】" + ExceptionCons.LOST_DEST_NODE);
            // 流程id
            skip.setDefinitionId(definitionId);
            skip.setNowNodeType(node.getNodeType());
            if (NodeType.isGateWaySerial(node.getNodeType())) {
                String target = skip.getSkipCondition() + ":" + skip.getNextNodeCode();
                AssertUtil.contains(gateWaySet, target, "[" + nodeName + "]" + ExceptionCons.SAME_CONDITION_NODE);
                gateWaySet.add(target);
            } else if (NodeType.isGateWayParallel(node.getNodeType())) {
                String target = skip.getNextNodeCode();
                AssertUtil.contains(gateWaySet, target, "[" + nodeName + "]" + ExceptionCons.SAME_DEST_NODE);
                gateWaySet.add(target);
            } else {
                String value = skip.getSkipType() + ":" + skip.getNextNodeCode();
                AssertUtil.contains(betweenSet, value, "[" + nodeName + "]" + ExceptionCons.SAME_CONDITION_VALUE);
                betweenSet.add(value);
            }
        }
    }

}
