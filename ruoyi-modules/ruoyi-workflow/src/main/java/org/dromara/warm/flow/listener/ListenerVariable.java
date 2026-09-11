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
package org.dromara.warm.flow.listener;

import org.dromara.warm.flow.dto.FlowParams;
import org.dromara.warm.flow.entity.FlowDefinition;
import org.dromara.warm.flow.entity.FlowInstance;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowTask;

import java.util.List;
import java.util.Map;

/**
 * 监听器变量
 *
 * @author warm
 */
public class ListenerVariable {

    /**
     * 流程定义
     */
    private FlowDefinition definition;

    /**
     * 流程实例
     */
    private FlowInstance instance;

    /**
     * 监听器对应的节点
     */
    private FlowNode node;

    /**
     * 当前任务
     */
    private FlowTask task;

    /**
     * 下一次执行的节点集合
     */
    private List<FlowNode> nextNodes;

    /**
     * 新创建任务集合
     */
    private List<FlowTask> nextTasks;

    /**
     * 流程变量
     */
    private Map<String, Object> variable;

    /**
     * 工作流内置参数
     */
    private FlowParams flowParams;


    public ListenerVariable() {
    }

    public ListenerVariable(FlowDefinition definition, FlowInstance instance, Map<String, Object> variable) {
        this.definition = definition;
        this.instance = instance;
        this.variable = variable;
    }

    public ListenerVariable(FlowDefinition definition, FlowInstance instance, FlowNode node, Map<String, Object> variable) {
        this.definition = definition;
        this.instance = instance;
        this.node = node;
        this.variable = variable;
    }

    public ListenerVariable(FlowDefinition definition, FlowInstance instance, Map<String, Object> variable, FlowTask task) {
        this.definition = definition;
        this.instance = instance;
        this.variable = variable;
        this.task = task;
    }

    public ListenerVariable(FlowDefinition definition, FlowInstance instance, FlowNode node, Map<String, Object> variable, FlowTask task) {
        this.definition = definition;
        this.instance = instance;
        this.node = node;
        this.variable = variable;
        this.task = task;
    }

    public ListenerVariable(FlowDefinition definition, FlowInstance instance, FlowNode node, Map<String, Object> variable, FlowTask task, List<FlowNode> nextNodes) {
        this.definition = definition;
        this.instance = instance;
        this.node = node;
        this.variable = variable;
        this.task = task;
        this.nextNodes = nextNodes;
    }

    public ListenerVariable(FlowDefinition definition, FlowInstance instance, FlowNode node, Map<String, Object> variable, FlowTask task
        , List<FlowNode> nextNodes, List<FlowTask> nextTasks) {
        this.definition = definition;
        this.instance = instance;
        this.node = node;
        this.variable = variable;
        this.task = task;
        this.nextNodes = nextNodes;
        this.nextTasks = nextTasks;
    }

    public FlowDefinition getDefinition() {
        return definition;
    }

    public ListenerVariable setDefinition(FlowDefinition definition) {
        this.definition = definition;
        return this;
    }

    public FlowInstance getInstance() {
        return instance;
    }

    public ListenerVariable setInstance(FlowInstance instance) {
        this.instance = instance;
        return this;
    }

    public FlowNode getNode() {
        return node;
    }

    public ListenerVariable setNode(FlowNode node) {
        this.node = node;
        return this;
    }

    public FlowTask getTask() {
        return task;
    }

    public ListenerVariable setTask(FlowTask task) {
        this.task = task;
        return this;
    }

    public List<FlowNode> getNextNodes() {
        return nextNodes;
    }

    public ListenerVariable setNextNodes(List<FlowNode> nextNodes) {
        this.nextNodes = nextNodes;
        return this;
    }

    public List<FlowTask> getNextTasks() {
        return nextTasks;
    }

    public ListenerVariable setNextTasks(List<FlowTask> nextTasks) {
        this.nextTasks = nextTasks;
        return this;
    }

    public Map<String, Object> getVariable() {
        return variable;
    }

    public ListenerVariable setVariable(Map<String, Object> variable) {
        this.variable = variable;
        return this;
    }

    public FlowParams getFlowParams() {
        return flowParams;
    }

    public ListenerVariable setFlowParams(FlowParams flowParams) {
        this.flowParams = flowParams;
        return this;
    }


    @Override
    public String toString() {
        return "ListenerVariable{" +
            "definition=" + definition +
            ", instance=" + instance +
            ", node=" + node +
            ", task=" + task +
            ", nextNodes=" + nextNodes +
            ", nextTasks=" + nextTasks +
            ", variable=" + variable +
            ", flowParams=" + flowParams +
            '}';
    }
}
