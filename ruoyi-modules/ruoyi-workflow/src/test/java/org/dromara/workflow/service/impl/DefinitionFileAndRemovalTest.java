package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefinitionFileAndRemovalTest {
    private final DefService definitions = mock(DefService.class);
    private final FlowDefinitionMapper mapper = mock(FlowDefinitionMapper.class);
    private final FlowHisTaskMapper history = mock(FlowHisTaskMapper.class);
    private final FlwDefinitionServiceImpl service = new FlwDefinitionServiceImpl(definitions, mapper, history, null, null, null);

    @org.junit.jupiter.api.BeforeAll
    static void initializeMapperMetadata() {
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(
            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "workflow-history");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, FlowHisTask.class);
    }

    @Test
    void acceptsCollectionOfUnusedDefinitions() {
        assertTrue(service.removeDef(new LinkedHashSet<>(List.of(4L, 7L))));
        verify(definitions).removeDef(List.of(4L, 7L));
    }

    @Test
    void rejectsWholeBatchContainingUsedDefinition() {
        FlowHisTask task = new FlowHisTask();
        task.setDefinitionId(7L);
        FlowDefinition used = new FlowDefinition();
        used.setFlowCode("leave");
        when(history.selectList(any())).thenReturn(List.of(task));
        when(mapper.selectByIds(List.of(7L))).thenReturn(List.of(used));
        assertTrue(assertThrows(ServiceException.class, () -> service.removeDef(List.of(4L, 7L)))
            .getMessage().contains("leave"));
        verifyNoInteractions(definitions);
    }

    @Test
    void publishReportsEveryUnassignedApprovalBeforeCallingEngine() {
        var nodes = mock(org.dromara.warm.flow.orm.mapper.FlowNodeMapper.class);
        var common = mock(org.dromara.workflow.service.IFlwCommonService.class);
        var publisher = new FlwDefinitionServiceImpl(definitions, mapper, history, nodes, null, common);
        var approvals = List.of("财务", "部门").stream().map(name -> new org.dromara.warm.flow.orm.entity.FlowNode()
            .setNodeCode(name).setNodeName(name).setNodeType(org.dromara.warm.flow.core.enums.NodeType.BETWEEN.getKey())).toList();
        when(common.applyNodeCode(9L)).thenReturn("apply");
        when(nodes.selectList(any())).thenReturn(approvals);
        var error = assertThrows(ServiceException.class, () -> publisher.publish(9L));
        assertTrue(error.getMessage().contains("财务"));
        assertTrue(error.getMessage().contains("部门"));
        verifyNoInteractions(definitions);
    }

    @Test
    void exportedChineseDefinitionCanBeImportedIntoAnotherCategory() throws IOException {
        String json = """
            {"flowCode":"leave","category":"old","nodeList":[{"nodeCode":"review","nodeName":"部门审批"}]}
            """;
        when(definitions.exportJson(9L)).thenReturn(json);
        var response = new MockHttpServletResponse();
        service.exportDef(9L, response);
        var file = new org.springframework.mock.web.MockMultipartFile("file", response.getContentAsByteArray());
        try (var spring = mockStatic(cn.hutool.extra.spring.SpringUtil.class)) {
            spring.when(() -> cn.hutool.extra.spring.SpringUtil.getBean(tools.jackson.databind.json.JsonMapper.class))
                .thenReturn(tools.jackson.databind.json.JsonMapper.builder().build());
            assertTrue(service.importJson(file, "new"));
        }
        var definition = org.mockito.ArgumentCaptor.forClass(org.dromara.warm.flow.core.dto.DefJson.class);
        verify(definitions).importDef(definition.capture());
        assertEquals("new", definition.getValue().getCategory());
        assertEquals("leave", definition.getValue().getFlowCode());
        assertEquals("部门审批", definition.getValue().getNodeList().getFirst().getNodeName());
    }

    @Test
    void unreadableFileStopsBeforeImport() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("read failed"));
        assertThrows(ServiceException.class, () -> service.importJson(file, "12"));
        verifyNoInteractions(definitions);
    }

    @Test
    void exportPreservesUtf8AndByteLength() throws IOException {
        String json = "{\"nodeName\":\"部门审批\"}";
        when(definitions.exportJson(7L)).thenReturn(json);
        var response = new MockHttpServletResponse();
        service.exportDef(7L, response);
        byte[] expected = json.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, response.getContentAsByteArray());
        assertEquals(Integer.toString(expected.length), response.getHeader("Content-Length"));
    }
}
