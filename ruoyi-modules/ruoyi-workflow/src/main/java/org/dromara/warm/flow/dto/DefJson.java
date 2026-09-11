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
import org.dromara.warm.flow.entity.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowInstance;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowSkip;



import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程定义json对象
 *
 * @author warm
 * @since 2023-03-29
 */
@Setter
@Getter
@Accessors(chain = true)
@ToString
public class DefJson {

    /**
     * 主键
     */
    private Long id;

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
     * 审批表单是否自定义（Y=是 N=否）
     */
    private String formCustom;

    /**
     * 审批表单是否自定义（Y=是 N=否）
     */
    private String formPath;

    /**
     * 监听器类型
     */
    private String listenerType;

    /**
     * 监听器路径
     */
    private String listenerPath;

    /**
     * 实例对象
     */
    private FlowInstance instance;

    /**
     * 扩展字段，预留给业务系统使用
     */
    private String ext;

    /**
     * 扩展map，保存业务自定义扩展属性
     */
    private Map<String, Object> extMap;

    /**
     * 所有节点结合
     */
    private List<NodeJson> nodeList = new ArrayList<>();

    /**
     * 流程状态对应的三原色
     */
    private List<String> chartStatusColor;

    /**
     * 顶部信息: 比如流程名称等
     */
    private String topText;

    /**
     * 顶部信息: 流程名称是否显示
     */
    private boolean topTextShow;

    private String createBy;

    private String updateBy;

    /**
     * 流程类别
     */
    private List<Tree> categoryList;

    /**
     * 自定义表单的唯一标识：如formCode+version
     */
    private List<Tree> formPathList;


    public String getModelValue() {
        if (StrUtil.isEmpty(modelValue)) {
            modelValue = "CLASSICS";
        }
        return modelValue;
    }

    public static DefJson copyDef(FlowDefinition definition) {
        DefJson defJson = new DefJson()
                .setFlowCode(definition.getFlowCode())
                .setFlowName(definition.getFlowName())
                .setModelValue(definition.getModelValue())
                .setVersion(definition.getVersion())
                .setIsPublish(definition.getIsPublish())
                .setCategory(definition.getCategory())
                .setFormCustom(definition.getFormCustom())
                .setFormPath(definition.getFormPath())
                .setListenerType(definition.getListenerType())
                .setListenerPath(definition.getListenerPath())
                .setExt(definition.getExt())
                .setCreateBy(definition.getCreateBy())
                .setUpdateBy(definition.getUpdateBy());

        List<NodeJson> nodeList = new ArrayList<>();
        defJson.setNodeList(nodeList);
        for (FlowNode node : definition.getNodeList()) {
            // 向节点中添加子节点
            NodeJson nodeJson = new NodeJson()
                    .setNodeType(node.getNodeType())
                    .setNodeCode(node.getNodeCode())
                    .setNodeName(node.getNodeName())
                    .setPermissionFlag(node.getPermissionFlag())
                    .setNodeRatio(node.getNodeRatio())
                    .setCoordinate(node.getCoordinate())
                    .setAnyNodeSkip(node.getAnyNodeSkip())
                    .setListenerType(node.getListenerType())
                    .setListenerPath(node.getListenerPath())
                    .setFormCustom(node.getFormCustom())
                    .setFormPath(node.getFormPath())
                    .setExt(node.getExt())
                    .setCreateBy(node.getCreateBy())
                    .setUpdateBy(node.getUpdateBy());
            nodeList.add(nodeJson);

            List<SkipJson> skipList = new ArrayList<>();
            nodeJson.setSkipList(skipList);
            if (CollUtil.isNotEmpty(node.getSkipList())) {
                for (FlowSkip skip : node.getSkipList()) {
                    skipList.add(new SkipJson()
                            .setCoordinate(skip.getCoordinate())
                            .setSkipType(skip.getSkipType())
                            .setSkipName(skip.getSkipName())
                            .setSkipCondition(skip.getSkipCondition())
                            .setNowNodeCode(skip.getNowNodeCode())
                            .setNextNodeCode(skip.getNextNodeCode())
                            .setCreateBy(skip.getCreateBy())
                            .setUpdateBy(skip.getUpdateBy()));
                }
            }

        }
        return defJson;
    }

    public static FlowDefinition copyDef(DefJson defJson) {
        FlowDefinition definition = new FlowDefinition()
                .setId(defJson.getId())
                .setFlowCode(defJson.getFlowCode())
                .setFlowName(defJson.getFlowName())
                .setModelValue(defJson.getModelValue())
                .setVersion(defJson.getVersion())
                .setCategory(defJson.getCategory())
                .setFormCustom(defJson.getFormCustom())
                .setFormPath(defJson.getFormPath())
                .setListenerType(defJson.getListenerType())
                .setListenerPath(defJson.getListenerPath())
                .setExt(defJson.getExt())
                .setCreateBy(defJson.getCreateBy())
                .setUpdateBy(defJson.getUpdateBy());

        List<FlowNode> nodeList = new ArrayList<>();
        definition.setNodeList(nodeList);
        for (NodeJson nodeJson : defJson.getNodeList()) {
            // 向节点中添加子节点
            FlowNode node = new FlowNode()
                    .setNodeType(nodeJson.getNodeType())
                    .setNodeCode(nodeJson.getNodeCode())
                    .setNodeName(nodeJson.getNodeName())
                    .setPermissionFlag(nodeJson.getPermissionFlag())
                    .setNodeRatio(nodeJson.getNodeRatio() != null ? nodeJson.getNodeRatio() : "0")
                    .setCoordinate(nodeJson.getCoordinate())
                    .setAnyNodeSkip(nodeJson.getAnyNodeSkip())
                    .setListenerType(nodeJson.getListenerType())
                    .setListenerPath(nodeJson.getListenerPath())
                    .setFormCustom(nodeJson.getFormCustom())
                    .setFormPath(nodeJson.getFormPath())
                    .setExt(nodeJson.getExt())
                    .setCreateBy(nodeJson.getCreateBy())
                    .setUpdateBy(nodeJson.getUpdateBy());
            nodeList.add(node);

            List<FlowSkip> skipList = new ArrayList<>();
            node.setSkipList(skipList);

            if (CollUtil.isNotEmpty(nodeJson.getSkipList())) {
                for (SkipJson skipJson : nodeJson.getSkipList()) {
                    skipList.add(new FlowSkip()
                            .setCoordinate(skipJson.getCoordinate())
                            .setSkipType(skipJson.getSkipType())
                            .setSkipName(skipJson.getSkipName())
                            .setSkipCondition(skipJson.getSkipCondition())
                            .setNowNodeCode(skipJson.getNowNodeCode())
                            .setNextNodeCode(skipJson.getNextNodeCode())
                            .setCreateBy(skipJson.getCreateBy())
                            .setUpdateBy(skipJson.getUpdateBy()));
                }
            }

        }
        return definition;
    }

    public static FlowCombine copyCombine(DefJson defJson) {
        FlowDefinition definition = copyDef(defJson);
        FlowCombine flowCombine = new FlowCombine();
        flowCombine.setDefinition(definition);
        flowCombine.setAllNodes(definition.getNodeList());
        List<FlowSkip> skipList = definition.getNodeList().stream()
            .map(FlowNode::getSkipList)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        flowCombine.setAllSkips(skipList);
        return flowCombine;
    }

}
