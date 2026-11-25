package org.dromara.system.service.impl;

import org.dromara.system.domain.vo.SysDeptVo;
import org.dromara.common.core.utils.SpringUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepartmentDirectoryTest {

    @Test
    void disabledParentBlocksChildCreationBeforeInsert() {
        var mapper = mock(org.dromara.system.mapper.SysDeptMapper.class);
        var dept = new org.dromara.system.domain.SysDept();
        dept.setStatus("1");
        when(mapper.selectById(15L)).thenReturn(dept);
        var request = new org.dromara.system.domain.bo.SysDeptBo();
        request.setParentId(15L);
        var service = new SysDeptServiceImpl(mapper, mock(), mock());
        assertThrows(org.dromara.common.core.exception.ServiceException.class, () -> service.insertDept(request));
        verify(mapper, never()).insert(any(org.dromara.system.domain.SysDept.class));
    }

    @Test
    void treeKeepsOrphanRootAndDisabledChild() {
        var service = new SysDeptServiceImpl(mock(), mock(), mock());
        assertTrue(service.buildDeptTreeSelect(List.of()).isEmpty());
        var root = new SysDeptVo();
        root.setDeptId(15L);
        root.setParentId(3L);
        root.setOrderNum(1);
        root.setDeptName("销售");
        var child = new SysDeptVo();
        child.setDeptId(18L);
        child.setParentId(15L);
        child.setDeptName("售后");
        child.setOrderNum(2);
        child.setStatus("1");
        var tree = service.buildDeptTreeSelect(List.of(child, root)).getFirst();
        assertEquals(15L, tree.getId());
        assertEquals("销售", tree.getName());
        assertEquals(18L, tree.getChildren().getFirst().getId());
        assertEquals(true, tree.getChildren().getFirst().get("disabled"));
    }

    @Test
    void returnsLeadersAndNamesOfExistingDepartmentsInRequestOrder() {
        var service = new SysDeptServiceImpl(mock(), mock(), mock());
        var cached = mock(SysDeptServiceImpl.class);
        var first = new SysDeptVo();
        first.setDeptName("销售");
        first.setLeader(51L);
        var last = new SysDeptVo();
        last.setDeptName("采购");
        when(cached.selectDeptById(7L)).thenReturn(first);
        when(cached.selectDeptById(5L)).thenReturn(last);
        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(() -> SpringUtils.getAopProxy(service)).thenReturn(cached);
            assertEquals("销售,采购", service.selectDeptNameByIds("7,6,5"));
            assertEquals(51L, service.selectDeptLeaderById(7L));
            assertNull(service.selectDeptLeaderById(6L));
        }
        assertTrue(service.selectChildDeptIds(null).isEmpty());
        assertTrue(service.selectDeptAndChildIds(null).isEmpty());
        assertTrue(service.selectDeptNamesByIds(List.of()).isEmpty());
    }
}
