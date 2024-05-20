package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.workflow.domain.vo.MultiInstanceVo;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.ProcessEngine;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.ServerException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelUtils {

    private static final ProcessEngine PROCESS_ENGINE = SpringUtils.getBean(ProcessEngine.class);

    public static BpmnModel xmlToBpmnModel(String xml) throws IOException {
        if (xml == null) {
            throw new ServerException("xml不能为空");
        }
        try {
            InputStream inputStream = new ByteArrayInputStream(StrUtil.utf8Bytes(xml));
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            return new BpmnXMLConverter().convertToBpmnModel(reader);
        } catch (XMLStreamException e) {
            throw new ServerException(e.getMessage());
        }
    }

    /**
     * bpmnModel转为xml
     *
     * @param jsonBytes json
     */
    public static byte[] bpmnJsonToXmlBytes(byte[] jsonBytes) throws IOException {
        if (jsonBytes == null) {
            return new byte[0];
        }
        // 1. json字节码转成 BpmnModel 对象
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonBytes);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);

        if (bpmnModel.getProcesses().isEmpty()) {
            return new byte[0];
        }
        // 2.将bpmnModel转为xml
        return new BpmnXMLConverter().convertToXML(bpmnModel);
    }

    /**
     * xml转为bpmnModel
     *
     * @param xmlBytes xml
     */
    public static BpmnModel xmlToBpmnModel(byte[] xmlBytes) throws XMLStreamException {
        ByteArrayInputStream byteArrayInputStream = IoUtil.toStream(xmlBytes);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xtr = xif.createXMLStreamReader(byteArrayInputStream);
        return new BpmnXMLConverter().convertToBpmnModel(xtr);
    }

    /**
     * 校验模型
     *
     * @param bpmnModel bpmn模型
     */
    public static void checkBpmnModel(BpmnModel bpmnModel) throws ServerException {
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();

        checkBpmnNode(flowElements, false);

        List<SubProcess> subProcessList = flowElements.stream().filter(SubProcess.class::isInstance).map(SubProcess.class::cast).collect(Collectors.toList());
        if (!CollUtil.isEmpty(subProcessList)) {
            for (SubProcess subProcess : subProcessList) {
                Collection<FlowElement> subProcessFlowElements = subProcess.getFlowElements();
                checkBpmnNode(subProcessFlowElements, true);
            }
        }
        List<MultiInstanceVo> multiInstanceVoList = new ArrayList<>();
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask && ObjectUtil.isNotEmpty(((UserTask) flowElement).getLoopCharacteristics()) && StringUtils.isNotBlank(((UserTask) flowElement).getLoopCharacteristics().getInputDataItem())) {
                MultiInstanceVo multiInstanceVo = new MultiInstanceVo();
                multiInstanceVo.setAssigneeList(((UserTask) flowElement).getLoopCharacteristics().getInputDataItem());
                multiInstanceVoList.add(multiInstanceVo);
            }
        }

        if (CollectionUtil.isNotEmpty(multiInstanceVoList) && multiInstanceVoList.size() > 1) {
            Map<String, List<MultiInstanceVo>> assigneeListGroup = StreamUtils.groupByKey(multiInstanceVoList, MultiInstanceVo::getAssigneeList);
            for (Map.Entry<String, List<MultiInstanceVo>> entry : assigneeListGroup.entrySet()) {
                List<MultiInstanceVo> value = entry.getValue();
                if (CollectionUtil.isNotEmpty(value) && value.size() > 1) {
                    String key = entry.getKey();
                    throw new ServerException("会签人员集合【" + key + "】重复,请重新设置集合KEY");
                }
            }
        }
    }

    /**
     * 校验bpmn节点是否合法
     *
     * @param flowElements 节点集合
     * @param subtask      是否子流程
     */
    private static void checkBpmnNode(Collection<FlowElement> flowElements, boolean subtask) throws ServerException {

        if (CollUtil.isEmpty(flowElements)) {
            throw new ServerException(subtask ? "子流程必须存在节点" : "必须存在节点！");
        }

        List<StartEvent> startEventList = flowElements.stream().filter(StartEvent.class::isInstance).map(StartEvent.class::cast).collect(Collectors.toList());
        if (CollUtil.isEmpty(startEventList)) {
            throw new ServerException(subtask ? "子流程必须存在开始节点" : "必须存在开始节点！");
        }

        if (startEventList.size() > 1) {
            throw new ServerException(subtask ? "子流程只能存在一个开始节点" : "只能存在一个开始节点！");
        }

        StartEvent startEvent = startEventList.get(0);
        List<SequenceFlow> outgoingFlows = startEvent.getOutgoingFlows();
        if (CollUtil.isEmpty(outgoingFlows)) {
            throw new ServerException(subtask ? "子流程流程节点为空，请至少设计一条主线流程！" : "流程节点为空，请至少设计一条主线流程！");
        }

        FlowElement targetFlowElement = outgoingFlows.get(0).getTargetFlowElement();
        if (!(targetFlowElement instanceof UserTask) && !subtask) {
            throw new ServerException("开始节点后第一个节点必须是用户任务！");
        }
        //开始节点后第一个节点申请人节点
        if ((targetFlowElement instanceof UserTask) && !subtask) {
            UserTask userTask = (UserTask) targetFlowElement;
            if (StringUtils.isBlank(userTask.getFormKey())) {
                throw new ServerException("申请人节点必须选择表单！");
            }
        }
        List<EndEvent> endEventList = flowElements.stream().filter(EndEvent.class::isInstance).map(EndEvent.class::cast).collect(Collectors.toList());
        if (CollUtil.isEmpty(endEventList)) {
            throw new ServerException(subtask ? "子流程必须存在结束节点！" : "必须存在结束节点！");
        }
    }

    /**
     * 获取流程全部用户节点
     *
     * @param processDefinitionId 流程定义id
     */
    public static List<UserTask> getUserTaskFlowElements(String processDefinitionId) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        List<UserTask> list = new ArrayList<>();
        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> flowElements = processes.get(0).getFlowElements();
        buildUserTaskFlowElements(flowElements, list);
        return list;
    }

    /**
     * 递归获取所有节点
     *
     * @param flowElements 节点信息
     * @param list         集合
     */
    private static void buildUserTaskFlowElements(Collection<FlowElement> flowElements, List<UserTask> list) {
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof SubProcess) {
                Collection<FlowElement> subFlowElements = ((SubProcess) flowElement).getFlowElements();
                buildUserTaskFlowElements(subFlowElements, list);
            } else if (flowElement instanceof UserTask) {
                list.add((UserTask) flowElement);
            }
        }
    }

    /**
     * 获取流程全部节点
     *
     * @param processDefinitionId 流程定义id
     */
    public static List<FlowElement> getFlowElements(String processDefinitionId) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        List<FlowElement> list = new ArrayList<>();
        List<Process> processes = bpmnModel.getProcesses();
        Collection<FlowElement> flowElements = processes.get(0).getFlowElements();
        buildFlowElements(flowElements, list);
        return list;
    }

    /**
     * 递归获取所有节点
     *
     * @param flowElements 节点信息
     * @param list         集合
     */
    private static void buildFlowElements(Collection<FlowElement> flowElements, List<FlowElement> list) {
        for (FlowElement flowElement : flowElements) {
            list.add(flowElement);
            if (flowElement instanceof SubProcess) {
                Collection<FlowElement> subFlowElements = ((SubProcess) flowElement).getFlowElements();
                buildFlowElements(subFlowElements, list);
            }
        }
    }

    /**
     * 获取全部扩展信息
     *
     * @param processDefinitionId 流程定义id
     */
    public static Map<String, List<ExtensionElement>> getExtensionElements(String processDefinitionId) {
        Map<String, List<ExtensionElement>> map = new HashMap<>();
        List<FlowElement> flowElements = getFlowElements(processDefinitionId);
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask && CollUtil.isNotEmpty(flowElement.getExtensionElements())) {
                map.putAll(flowElement.getExtensionElements());
            }
        }
        return map;
    }

    /**
     * 获取某个节点的扩展信息
     *
     * @param processDefinitionId 流程定义id
     * @param flowElementId       节点id
     */
    public static Map<String, List<ExtensionElement>> getExtensionElement(String processDefinitionId, String flowElementId) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        Process process = bpmnModel.getMainProcess();
        FlowElement flowElement = process.getFlowElement(flowElementId);
        return flowElement.getExtensionElements();
    }

    /**
     * 判断当前节点是否为用户任务
     *
     * @param processDefinitionId 流程定义id
     * @param taskDefinitionKey   流程定义id
     */
    public static boolean isUserTask(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(taskDefinitionKey);
        return flowNode instanceof UserTask;
    }

    /**
     * 获取申请人节点
     *
     * @param processDefinitionId 流程定义id
     * @return 结果
     */
    public static UserTask getApplyUserTask(String processDefinitionId) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
        List<StartEvent> startEventList = flowElements.stream().filter(StartEvent.class::isInstance).map(StartEvent.class::cast).collect(Collectors.toList());
        StartEvent startEvent = startEventList.get(0);
        List<SequenceFlow> outgoingFlows = startEvent.getOutgoingFlows();
        FlowElement targetFlowElement = outgoingFlows.get(0).getTargetFlowElement();
        return (UserTask) targetFlowElement;
    }
}
