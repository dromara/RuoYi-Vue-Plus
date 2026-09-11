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
import org.dromara.warm.flow.json.JsonUtil;

import org.dromara.warm.flow.entity.*;

import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.constant.ExceptionCons;
import org.dromara.warm.flow.constant.FlowCons;
import org.dromara.warm.flow.dto.FlowCombine;
import org.dromara.warm.flow.dto.PathWayData;
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;
import org.dromara.warm.flow.enums.NodeType;
import org.dromara.warm.flow.enums.PublishStatus;
import org.dromara.warm.flow.enums.SkipType;
import org.dromara.warm.flow.exception.FlowException;
import org.dromara.warm.flow.service.WarmServiceImpl;
import org.dromara.warm.flow.utils.*;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;




import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowNodeMapper;


import java.util.Collection;

import java.util.List;


/**
 * 流程节点Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
@Slf4j
@Service
public class NodeService extends WarmServiceImpl<FlowNode> {
    public List<FlowNode> getPublishByFlowCode(String flowCode) {
        FlowDefinition definition = FlowEngine.defService().getOne(new FlowDefinition()
            .setFlowCode(flowCode).setIsPublish(PublishStatus.PUBLISHED.getKey()));
        if (ObjectUtil.isNotNull(definition)) {
            return list(new FlowNode().setDefinitionId(definition.getId()));
        }
        return Collections.emptyList();
    }

    public List<FlowNode> getByNodeCodes(List<String> nodeCodes, Long definitionId) {
        return getMapper().getByNodeCodes(nodeCodes, definitionId);
    }

    public List<FlowNode> previousNodeList(Long nodeId) {
        FlowNode nowNode = getById(nodeId);
        return previousNodeList(nowNode.getDefinitionId(), nowNode.getNodeCode());
    }

    public List<FlowNode> previousNodeList(Long definitionId, String nowNodeCode) {
        return prefixOrSuffixNodes(definitionId, nowNodeCode, FlowCons.PREVIOUS);
    }

    public List<FlowNode> suffixNodeList(Long nodeId) {
        FlowNode nowNode = getById(nodeId);
        return suffixNodeList(nowNode.getDefinitionId(), nowNode.getNodeCode());
    }

    public List<FlowNode> suffixNodeList(Long definitionId, String nowNodeCode) {
        return prefixOrSuffixNodes(definitionId, nowNodeCode, FlowCons.SUFFIX);
    }

    public List<FlowNode> suffixNodeList(String nowNodeCode, FlowCombine flowCombine) {
        return prefixOrSuffixNodes(nowNodeCode, FlowCons.SUFFIX, flowCombine);
    }

    public List<FlowNode> getByDefId(Long definitionId) {
        return list(new FlowNode().setDefinitionId(definitionId));
    }

    public FlowNode getByDefIdAndNodeCode(Long definitionId, String nodeCode) {
        return getOne(new FlowNode().setDefinitionId(definitionId).setNodeCode(nodeCode));
    }

    public FlowNode getStartNode(Long definitionId) {
        return getOne(new FlowNode().setDefinitionId(definitionId).setNodeType(NodeType.START.getKey()));
    }

    public List<FlowNode> getBetweenNode(Long definitionId) {
        return list(new FlowNode().setDefinitionId(definitionId).setNodeType(NodeType.BETWEEN.getKey()));
    }


    public List<FlowNode> getFirstBetweenNode(Long definitionId, Map<String, Object> variable) {
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(definitionId);
        FlowNode startNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), t -> NodeType.isStart(t.getNodeType()));
        return getNextNodeList(startNode, null, SkipType.PASS.getKey(),
            variable, null, flowCombine);
    }

    public FlowNode getEndNode(Long definitionId) {
        return getOne(new FlowNode().setDefinitionId(definitionId).setNodeType(NodeType.END.getKey()));
    }

    public List<FlowNode> prefixOrSuffixNodes(Long definitionId, String nowNodeCode, String type) {
        FlowCombine flowCombine = new FlowCombine();
        flowCombine.setAllNodes(FlowEngine.nodeService().getByDefId(definitionId));
        flowCombine.setAllSkips(FlowEngine.skipService().getByDefId(definitionId));
        return prefixOrSuffixNodes(nowNodeCode, type, flowCombine);
    }

    public List<FlowNode> prefixOrSuffixNodes(String nowNodeCode, String type, FlowCombine flowCombine) {
        Map<String, FlowNode> nodeMap = StreamUtils.toMap(flowCombine.getAllNodes(), FlowNode::getNodeCode, node -> node);
        Map<String, List<FlowSkip>> skipMap = flowCombine.getAllSkips().stream().filter(skip -> SkipType.isPass(skip.getSkipType()))
            .collect(Collectors.groupingBy(FlowCons.PREVIOUS.equals(type) ? FlowSkip::getNextNodeCode : FlowSkip::getNowNodeCode
                , LinkedHashMap::new, Collectors.toList()));

        List<FlowNode> prefixOrSuffixNodes = new ArrayList<>();
        List<String> prefixOrSuffixCode = prefixOrSuffixCodes(skipMap, nowNodeCode
            , FlowCons.PREVIOUS.equals(type) ? FlowSkip::getNowNodeCode : FlowSkip::getNextNodeCode);
        for (String nodeCode : prefixOrSuffixCode) {
            FlowNode node = nodeMap.get(nodeCode);
            if (!NodeType.isGateWay(node.getNodeType())) {
                prefixOrSuffixNodes.add(node);
            }
        }
        Collections.reverse(prefixOrSuffixNodes);
        Set<String> sameCode = new HashSet<>();
        prefixOrSuffixNodes.removeIf(node -> {
            if (sameCode.contains(node.getNodeCode())) {
                return true;
            }
            sameCode.add(node.getNodeCode());
            return false;
        });
        Collections.reverse(prefixOrSuffixNodes);
        return prefixOrSuffixNodes;
    }

    public List<FlowNode> getNextNodeList(Long definitionId, String nowNodeCode, String anyNodeCode, String skipType,
                                      Map<String, Object> variable) {
        AssertUtil.isEmpty(nowNodeCode, ExceptionCons.LOST_NODE_CODE);
        // 查询当前节点
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(definitionId);
        FlowNode nowNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), t -> t.getNodeCode().equals(nowNodeCode));
        // 如果是网关节点，则根据条件判断
        return getNextByCheckGateway(variable, getNextNode(nowNode, anyNodeCode, skipType, null, flowCombine),
            null, flowCombine);
    }

    public FlowNode getNextNode(Long definitionId, String nowNodeCode, String anyNodeCode, String skipType) {
        // 查询当前节点
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(definitionId);
        FlowNode nowNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), t -> t.getNodeCode().equals(nowNodeCode));
        return getNextNode(nowNode, anyNodeCode, skipType, null, flowCombine);
    }

    public List<FlowNode> getNextNodeList(FlowNode nowNode, String anyNodeCode, String skipType, Map<String, Object> variable
        , PathWayData pathWayData, FlowCombine flowCombine) {
        // 如果是网关节点，则根据条件判断
        return getNextByCheckGateway(variable, getNextNode(nowNode, anyNodeCode, skipType
            , pathWayData, flowCombine), pathWayData, flowCombine);
    }

    public FlowNode getNextNode(FlowNode nowNode, String anyNodeCode, String skipType, PathWayData pathWayData, FlowCombine flowCombine) {
        // 查询当前节点
        AssertUtil.isNull(nowNode, ExceptionCons.LOST_NODE_CODE);
        AssertUtil.isNull(nowNode.getDefinitionId(), ExceptionCons.NOT_DEFINITION_ID);
        AssertUtil.isEmpty(skipType, ExceptionCons.NULL_CONDITION_VALUE);

        if (pathWayData != null) {
            pathWayData.getPathWayNodes().add(nowNode);
        }
        FlowNode nextNode = null;
        if (StrUtil.isNotEmpty(anyNodeCode)) {
            // 如果指定了跳转节点，直接获取节点
            nextNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), node -> anyNodeCode.equals(node.getNodeCode()));
        } else if (StrUtil.isNotEmpty(nowNode.getAnyNodeSkip()) && SkipType.isReject(skipType)) {
            // 如果配置了任意跳转节点，直接获取节点
            nextNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), node -> nowNode.getAnyNodeSkip().equals(node.getNodeCode()));
        }

        if (ObjectUtil.isNotNull(nextNode)) {
            AssertUtil.isTrue(NodeType.isGateWay(nextNode.getNodeType()), ExceptionCons.TAR_NOT_GATEWAY);
            return nextNode;
        }

        // 获取跳转关系
        List<FlowSkip> skips = StreamUtils.filter(flowCombine.getAllSkips(), skip -> nowNode.getNodeCode().equals(skip.getNowNodeCode()));
        AssertUtil.isNull(skips, ExceptionCons.NULL_DEST_NODE);
        FlowSkip nextSkip = getSkipByCheck(skips, skipType);

        // 根据跳转查询出跳转到的那个节点
        nextNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), node -> nextSkip != null && nextSkip.getNextNodeCode().equals(node.getNodeCode()));
        AssertUtil.isNull(nextNode, ExceptionCons.NULL_NODE_CODE);
        AssertUtil.isTrue(NodeType.isStart(nextNode.getNodeType()), ExceptionCons.FIRST_FORBID_BACK);
        if (pathWayData != null) {
            pathWayData.getPathWayNodes().add(nextNode);
            pathWayData.getPathWaySkips().add(nextSkip);
        }
        return nextNode;
    }

    public List<FlowNode> getNextByCheckGateway(Map<String, Object> variable, FlowNode nextNode, PathWayData pathWayData
        , FlowCombine flowCombine) {
        // 网关节点处理
        if (NodeType.isGateWay(nextNode.getNodeType())) {
            List<FlowSkip> skipsGateway = StreamUtils.filter(flowCombine.getAllSkips()
                , skip -> nextNode.getNodeCode().equals(skip.getNowNodeCode()));
            if (CollUtil.isEmpty(skipsGateway)) {
                return null;
            }

            //如果是互斥网关，跳转条件匹配的，则取任意第一条，否则取跳转条件为空的任意一条
            if (NodeType.isGateWaySerial(nextNode.getNodeType())) {
                FlowSkip skipOne = null;
                for (FlowSkip skip : skipsGateway) {
                    if (StrUtil.isNotEmpty(skip.getSkipCondition())) {
                        if (ExpressionUtil.evalCondition(skip.getSkipCondition(), variable)) {
                            skipOne = skip;
                            break;
                        }
                    } else {
                        skipOne = skip;
                    }
                }
                skipsGateway = skipOne == null ? null : CollUtil.toList(skipOne);
            } else if (NodeType.isGateWayInclusive(nextNode.getNodeType())) {
                //如果是包含网关，有跳转条件的分支，但是跳转条件不匹配的不执行，没跳转条件为空的分支默认执行
                skipsGateway.removeIf(skip -> StrUtil.isNotEmpty(skip.getSkipCondition())
                    && !ExpressionUtil.evalCondition(skip.getSkipCondition(), variable));
            }

            AssertUtil.isEmpty(skipsGateway, ExceptionCons.NULL_CONDITION_VALUE_NODE);
            List<String> nextNodeCodes = StreamUtils.toList(skipsGateway, FlowSkip::getNextNodeCode);
            List<FlowNode> nextNodes = StreamUtils.filter(flowCombine.getAllNodes()
                , node -> nextNodeCodes.contains(node.getNodeCode()));
            AssertUtil.isEmpty(nextNodes, ExceptionCons.NOT_NODE_DATA);
            if (pathWayData != null) {
                pathWayData.getPathWayNodes().addAll(nextNodes);
                pathWayData.getPathWaySkips().addAll(skipsGateway);
            }
            List<FlowNode> newNextNodes = new ArrayList<>();
            for (FlowNode node : nextNodes) {
                List<FlowNode> nodeList = getNextByCheckGateway(variable, node, pathWayData, flowCombine);
                newNextNodes.addAll(nodeList);
            }
            return newNextNodes;
        }
        // 非网关节点直接返回
        if (pathWayData != null) {
            pathWayData.getPathWayNodes().remove(nextNode);
        }
        AssertUtil.isTrue(NodeType.isStart(nextNode.getNodeType()), ExceptionCons.START_NODE_NOT_ALLOW_JUMP);
        return CollUtil.toList(nextNode);
    }


    public int deleteNodeByDefIds(Collection<? extends Serializable> defIds) {
        return getMapper().deleteNodeByDefIds(defIds);
    }

    public Map<String, String> getExt(FlowNode node) {
        Map<String, String> map = new HashMap<>();
        String ext = node.getExt();
        if (StrUtil.isNotEmpty(ext)) {
            List<Map<String, Object>> extList = JsonUtil.strToList(ext);
            if (CollUtil.isNotEmpty(extList)) {
                for (Map<String, Object> extMap : extList) {
                    String code = ObjectUtil.defaultIfNull(extMap.get("code"), "").toString();
                    String value = ObjectUtil.defaultIfNull(extMap.get("value"), "").toString();
                    if (StrUtil.isAllNotEmpty(code, value)) {
                        map.put(code, value);
                    }
                }
            }
        }

        return map;
    }

    private List<String> prefixOrSuffixCodes(Map<String, List<FlowSkip>> skipMap, String nodeCode,
                                             Function<FlowSkip, String> supplier) {
        // 记录已访问节点，防止循环
        Set<String> visited = new HashSet<>();
        List<String> result = new ArrayList<>();
        prefixOrSuffixCodesRecursive(skipMap, nodeCode, supplier, visited, result);
        return result;
    }

    private void prefixOrSuffixCodesRecursive(Map<String, List<FlowSkip>> skipMap, String nodeCode,
                                              Function<FlowSkip, String> supplier, Set<String> visited, List<String> result) {
        if (visited.contains(nodeCode)) {
            // 防止循环访问
            return;
        }

        visited.add(nodeCode);
        List<FlowSkip> skipList = skipMap.get(nodeCode);

        if (CollUtil.isNotEmpty(skipList)) {
            for (FlowSkip skip : skipList) {
                if (SkipType.isPass(skip.getSkipType())) {
                    String nextNodeCode = supplier.apply(skip);
                    // 避免重复添加
                    if (!result.contains(nextNodeCode)) {
                        result.add(nextNodeCode);
                    }
                    prefixOrSuffixCodesRecursive(skipMap, nextNodeCode, supplier, visited, result);
                }
            }
        }
    }


    /**
     * 通过校验跳转类型获取跳转集合
     *
     * @param skips    跳转集合
     * @param skipType 跳转类型
     * @return List<FlowSkip>
     * @author xiarg
     * @since 2024/8/21 11:32
     */
    private FlowSkip getSkipByCheck(List<FlowSkip> skips, String skipType) {
        return Optional.ofNullable(skips)
            .orElse(Collections.emptyList())
            .stream()
            .filter(t -> StrUtil.isEmpty(t.getSkipType()) || skipType.equals(t.getSkipType()))
            .findFirst()
            .orElseThrow(() -> new FlowException(ExceptionCons.NULL_SKIP_TYPE));
    }

    @Override
    public FlowNodeMapper getMapper() {
        return SpringUtils.getBean(FlowNodeMapper.class);
    }
}