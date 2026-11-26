package org.dromara.workflow.service.impl;

import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.workflow.api.event.ProcessEvent;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.TestLeave;
import org.dromara.workflow.mapper.TestLeaveMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveProcessEventTest {
    @org.junit.jupiter.api.Test
    void completionAndDeletionAffectOnlyMatchingRecord() {
        var mapper = mock(TestLeaveMapper.class);
        var service = new TestLeaveServiceImpl(mapper, null, null);
        var record = new TestLeave();
        record.setId(42L);
        record.setApplyCode("leave-42");
        when(mapper.selectById(42L)).thenReturn(record);
        var state = new ProcessEvent();
        state.setBusinessId("42");
        state.setStatus(BusinessStatusEnum.FINISH.getStatus());
        state.setSubmit(false);
        service.processHandler(state);
        assertEquals(BusinessStatusEnum.FINISH.getStatus(), record.getStatus());
        assertEquals("leave-42", record.getApplyCode());
        verify(mapper).updateById(record);
        var deletion = new org.dromara.workflow.api.event.ProcessDeleteEvent();
        deletion.setBusinessId("42");
        service.processDeleteHandler(deletion);
        verify(mapper).deleteById(42L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"other-flow", "404"})
    void unmatchedEventsDoNotWrite(String businessId) {
        var mapper = mock(TestLeaveMapper.class);
        var service = new TestLeaveServiceImpl(mapper, null, null);
        var state = new ProcessEvent();
        state.setBusinessId(businessId);
        service.processHandler(state);
        var deletion = new org.dromara.workflow.api.event.ProcessDeleteEvent();
        deletion.setBusinessId(businessId);
        service.processDeleteHandler(deletion);
        verify(mapper, never()).updateById(any(TestLeave.class));
        verify(mapper, never()).deleteById(anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "original"})
    void submittingAssignsCodeOnlyOnceAndSetsWaiting(String existingCode) {
        var mapper = mock(TestLeaveMapper.class);
        var service = new TestLeaveServiceImpl(mapper, null, null);
        var record = new TestLeave();
        record.setApplyCode(existingCode);
        when(mapper.selectById(17L)).thenReturn(record);
        var event = new ProcessEvent();
        event.setBusinessId("17");
        event.setStatus(BusinessStatusEnum.DRAFT.getStatus());
        event.setSubmit(true);
        event.setParams(Map.of(FlowConstant.BUSINESS_CODE, "new-code"));
        service.processHandler(event);
        assertEquals(existingCode.isEmpty() ? "new-code" : existingCode, record.getApplyCode());
        assertEquals(BusinessStatusEnum.WAITING.getStatus(), record.getStatus());
        verify(mapper).updateById(record);
    }
}
