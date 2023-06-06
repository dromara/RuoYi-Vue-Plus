package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.json.utils.JsonUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.ServerException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流工具
 *
 * @author may
 */
public class WorkflowUtils {

    public WorkflowUtils() {
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
        //1. json字节码转成 BpmnModel 对象
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonBytes);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);

        if (bpmnModel.getProcesses().isEmpty()) {
            return new byte[0];
        }
        //2.将bpmnModel转为xml
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
    }

    /**
     * 校验bpmn节点是否合法
     *
     * @param flowElements 节点集合
     * @param subtask      是否子流程
     */
    private static void checkBpmnNode(Collection<FlowElement> flowElements, boolean subtask) throws ServerException {

        if (CollUtil.isEmpty(flowElements)) {
            throw new ServerException(subtask ? "子流程必须存在节点" : "" + "必须存在节点！");
        }

        List<StartEvent> startEventList = flowElements.stream().filter(StartEvent.class::isInstance).map(StartEvent.class::cast).collect(Collectors.toList());
        if (CollUtil.isEmpty(startEventList)) {
            throw new ServerException(subtask ? "子流程必须存在开始节点" : "" + "必须存在开始节点！");
        }

        if (startEventList.size() > 1) {
            throw new ServerException(subtask ? "子流程只能存在一个开始节点" : "" + "只能存在一个开始节点！");
        }

        StartEvent startEvent = startEventList.get(0);
        List<SequenceFlow> outgoingFlows = startEvent.getOutgoingFlows();
        if (CollUtil.isEmpty(outgoingFlows)) {
            throw new ServerException(subtask ? "子流程流程节点为空，请至少设计一条主线流程！" : "" + "流程节点为空，请至少设计一条主线流程！");
        }

        FlowElement targetFlowElement = outgoingFlows.get(0).getTargetFlowElement();
        if (!(targetFlowElement instanceof UserTask)) {
            throw new ServerException(subtask ? "子流程开始节点后第一个节点必须是用户任务！" : "" + "开始节点后第一个节点必须是用户任务！");
        }

        List<EndEvent> endEventList = flowElements.stream().filter(EndEvent.class::isInstance).map(EndEvent.class::cast).collect(Collectors.toList());
        if (CollUtil.isEmpty(endEventList)) {
            throw new ServerException(subtask ? "子流程必须存在结束节点！" : "" + "必须存在结束节点！");
        }
    }
}
