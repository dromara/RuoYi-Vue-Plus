package org.dromara.common.core.enums;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessStatusEnum 单元测试")
class BusinessStatusEnumTest {

    /**
     * 验证状态码查询、描述查询以及运行中和已结束状态集合。
     */
    @Test
    @DisplayName("查询业务状态")
    void shouldResolveBusinessStatuses() {
        assertEquals(BusinessStatusEnum.DRAFT, BusinessStatusEnum.getByStatus("draft"));
        assertNull(BusinessStatusEnum.getByStatus("unknown"));
        assertEquals("已完成", BusinessStatusEnum.findByStatus("finish"));
        assertEquals("", BusinessStatusEnum.findByStatus(" "));
        assertEquals(List.of("draft", "waiting", "back", "cancel"), BusinessStatusEnum.runningStatus());
        assertEquals(List.of("finish", "invalid", "termination"), BusinessStatusEnum.finishStatus());
    }

    /**
     * 验证流程状态分类能够正确识别可重新发起状态和终止类状态。
     */
    @Test
    @DisplayName("分类业务状态")
    void shouldClassifyBusinessStatuses() {
        assertTrue(BusinessStatusEnum.isDraftOrCancelOrBack("draft"));
        assertTrue(BusinessStatusEnum.isDraftOrCancelOrBack("cancel"));
        assertTrue(BusinessStatusEnum.isDraftOrCancelOrBack("back"));
        assertFalse(BusinessStatusEnum.isDraftOrCancelOrBack("waiting"));
        assertTrue(BusinessStatusEnum.initialState("invalid"));
        assertTrue(BusinessStatusEnum.initialState("termination"));
        assertFalse(BusinessStatusEnum.initialState("waiting"));
    }

    /**
     * 验证启动流程仅允许草稿、撤销和退回等可发起状态，并为禁止状态返回业务异常。
     */
    @Test
    @DisplayName("校验流程启动状态")
    void shouldValidateStartStatus() {
        assertDoesNotThrow(() -> BusinessStatusEnum.checkStartStatus("draft"));
        assertEquals("该单据已提交过申请,正在审批中！",
            assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkStartStatus("waiting")).getMessage());
        assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkStartStatus("finish"));
        assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkStartStatus("invalid"));
        assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkStartStatus("termination"));
        assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkStartStatus(" "));
    }

    /**
     * 验证撤销、退回及作废校验分别拒绝所有已结束或重复操作状态。
     */
    @Test
    @DisplayName("校验流程变更状态")
    void shouldValidateCancelBackAndInvalidStatuses() {
        assertDoesNotThrow(() -> BusinessStatusEnum.checkCancelStatus("waiting"));
        for (String status : List.of("cancel", "finish", "invalid", "termination", "back", " ")) {
            assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkCancelStatus(status));
        }
        assertDoesNotThrow(() -> BusinessStatusEnum.checkBackStatus("waiting"));
        for (String status : List.of("back", "finish", "invalid", "termination", "cancel", " ")) {
            assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkBackStatus(status));
        }
        assertDoesNotThrow(() -> BusinessStatusEnum.checkInvalidStatus("waiting"));
        for (String status : List.of("finish", "invalid", "termination", " ")) {
            assertThrows(ServiceException.class, () -> BusinessStatusEnum.checkInvalidStatus(status));
        }
    }
}
