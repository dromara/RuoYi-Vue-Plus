package org.dromara.workflow.liteflow.instance;

import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.domain.context.InstanceDeleteContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstanceDeleteLoadTest {
    private final FlowInstanceMapper mapper = mock(FlowInstanceMapper.class);
    private final InstanceDeleteLoadComponent load = spy(new InstanceDeleteLoadComponent(mapper));
    private final InstanceDeleteExistsComponent exists = spy(new InstanceDeleteExistsComponent());

    @ParameterizedTest
    @ValueSource(strings = {"business", "running", "history"})
    void emptySelectionSkipsLookupAndDeletion(String mode) {
        var context = switch (mode) {
            case "business" -> InstanceDeleteContext.byBusinessIds(List.of());
            case "running" -> InstanceDeleteContext.byInstanceIds(List.of());
            default -> InstanceDeleteContext.byHistoryInstanceIds(List.of());
        };
        load(context);
        assertFalse(exists.processBoolean());
        assertFalse(context.isResult());
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"business", "running", "history"})
    void deletionUsesOnlyInstancesLoadedForOwnershipCheck(String mode) {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
            new com.baomidou.mybatisplus.core.MybatisConfiguration(), "delete-load"), FlowInstance.class);
        var matching = List.of(new FlowInstance().setBusinessId("A").setId(5L));
        var ids = List.of(5L, 99L);
        var context = switch (mode) {
            case "business" -> InstanceDeleteContext.byBusinessIds(List.of("A", "missing"));
            case "running" -> InstanceDeleteContext.byInstanceIds(ids);
            default -> InstanceDeleteContext.byHistoryInstanceIds(ids);
        };
        when(mapper.selectList(any())).thenReturn(matching);
        when(mapper.selectByIds(ids)).thenReturn(matching);
        load(context);
        assertTrue(exists.processBoolean());
        assertEquals(matching, context.getFlowInstances());
        assertEquals(List.of(5L), context.getDeleteInstanceIds());
    }

    private void load(InstanceDeleteContext context) {
        doReturn(context).when(load).getContextBean(InstanceDeleteContext.class);
        doReturn(context).when(exists).getContextBean(InstanceDeleteContext.class);
        load.process();
    }
}
