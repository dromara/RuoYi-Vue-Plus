package org.dromara.workflow.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.domain.bo.FlowVariableBo;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstanceVariableRulesTest {
    private final FlowInstanceMapper mapper = mock(FlowInstanceMapper.class);
    private final FlwInstanceServiceImpl service = new FlwInstanceServiceImpl(null, null, null, null, mapper, null, null, null);

    @Test
    void noVariablesReturnsEmptyListForExistingInstance() {
        FlowInstance instance = mock(FlowInstance.class);
        when(mapper.selectById(6L)).thenReturn(instance);
        assertTrue(((java.util.List<?>) service.instanceVariable(6L).get("variableList")).isEmpty());
    }

    @Test
    void readsBothRawJsonAndVariableEntries() {
        var instance = mock(FlowInstance.class);
        when(mapper.selectById(6L)).thenReturn(instance);
        when(instance.getVariable()).thenReturn("{\"days\":2}");
        when(instance.getVariableMap()).thenReturn(Map.of("days", 2));
        var result = service.instanceVariable(6L);
        assertEquals("{\"days\":2}", result.get("variable"));
        assertEquals(java.util.List.of(Map.of("key", "days", "value", 2)), result.get("variableList"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1})
    void updatePreservesOtherVariablesAndReflectsAffectedRows(int rows) {
        var instance = mock(FlowInstance.class);
        when(mapper.selectById(6L)).thenReturn(instance);
        when(instance.getVariableMap()).thenReturn(Map.of("days", "1", "department", 42));
        when(mapper.updateById(instance)).thenReturn(rows);
        var original = org.dromara.warm.flow.core.FlowEngine.jsonConvert;
        var codec = mock(org.dromara.warm.flow.core.json.JsonConvert.class);
        when(codec.objToStr(Map.of("days", "3", "department", 42))).thenReturn("{\"days\":\"3\",\"department\":42}");
        try {
            org.dromara.warm.flow.core.FlowEngine.jsonConvert = codec;
            assertEquals(rows == 1, service.updateVariable(new FlowVariableBo(6L, "days", "3")));
            verify(instance).setVariable("{\"days\":\"3\",\"department\":42}");
            verify(mapper).updateById(instance);
        } finally {
            org.dromara.warm.flow.core.FlowEngine.jsonConvert = original;
        }
    }

    @Test
    void unknownVariableDoesNotWrite() {
        FlowInstance instance = mock(FlowInstance.class);
        when(mapper.selectById(6L)).thenReturn(instance);
        when(instance.getVariableMap()).thenReturn(Map.of("days", 1));
        assertFalse(service.updateVariable(new FlowVariableBo(6L, "amount", "10")));
        verify(mapper, never()).updateById(any(FlowInstance.class));
    }

    @Test
    void missingInstanceRejectsReadsAndUpdates() {
        assertThrows(ServiceException.class, () -> service.instanceVariable(6L));
        assertThrows(ServiceException.class, () -> service.updateVariable(new FlowVariableBo(6L, "days", "2")));
    }

    @Test
    void historyLookupRequiresAnIdentifier() {
        assertThrows(ServiceException.class, () -> service.flowHisTaskList(null, " "));
        verifyNoInteractions(mapper);
    }
}
