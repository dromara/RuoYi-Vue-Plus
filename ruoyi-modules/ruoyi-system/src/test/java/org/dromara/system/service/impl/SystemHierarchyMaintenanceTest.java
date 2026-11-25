package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.bo.SysDeptBo;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemHierarchyMaintenanceTest {

    @Test
    void enablingChildAlsoEnablesItsRecordedAncestors() {
        var original = department(20, 10, "0,10");
        when(departments.selectById(20L)).thenReturn(original);
        var updated = department(20, 10, null);
        updated.setStatus("0");
        var bo = new SysDeptBo();
        var chain = departments.lambda();
        when(chain.set(any(), eq("0"))).thenReturn(chain);
        when(chain.in(any(), eq(List.of(0L,10L)))).thenReturn(chain);
        when(departments.updateById(updated)).thenReturn(1);
        try (var beans = mockStatic(SpringUtil.class); var mapping = mockStatic(MapstructUtils.class)) {
            mapping.when(() -> MapstructUtils.convert(bo, SysDept.class)).thenReturn(updated);
            assertEquals(1, service.updateDept(bo));
            verify(chain).update();
        }
    }

    private final SysDeptMapper departments = mock(SysDeptMapper.class, RETURNS_DEEP_STUBS);
    private final SysDeptServiceImpl service = spy(new SysDeptServiceImpl(departments, mock(SysRoleMapper.class), mock(SysUserMapper.class)));

    @ParameterizedTest
    @ValueSource(longs = {99, 20, 30})
    void rejectsMissingParentSelfAndDescendant(long parentId) {
        var current = department(20, 10, "0,10");
        when(departments.selectById(20L)).thenReturn(current);
        when(departments.selectById(99L)).thenReturn(null);
        when(departments.selectById(30L)).thenReturn(department(30, 21, "0,10,20,21"));
        assertThrows(ServiceException.class, () -> move(parentId));
        verify(departments, never()).updateById(any(SysDept.class));
        verify(departments, never()).updateBatchById(anyList());
    }

    @Test
    void movingDepartmentToRootRewritesItsAncestorPath() {
        when(departments.updateBatchById(anyList())).thenReturn(true);
        prepareMoveWithChild();
        when(departments.updateById(any(SysDept.class))).thenReturn(1);
        SysDept updated;
        try (var caches = mockStatic(CacheUtils.class)) {
            updated = move(0L);
        }
        assertEquals("0", updated.getAncestors());
        verify(departments).updateBatchById(argThat(nodes -> nodes.size() == 1
            && nodes.stream().anyMatch(node -> node.getDeptId().equals(21L) && "0,20".equals(node.getAncestors()))));
    }

    @Test
    void failedChildUpdateMustAbortDepartmentMove() {
        prepareMoveWithChild();
        when(departments.updateById(any(SysDept.class))).thenReturn(1);
        when(departments.updateBatchById(anyList())).thenReturn(false);
        assertThrows(ServiceException.class, () -> move(0L));
    }

    @Test
    void missingUpdateTargetMustLeaveDescendantsUntouched() {
        prepareMoveWithChild();
        when(departments.updateById(any(SysDept.class))).thenReturn(0);
        move(0L);
        verify(departments, never()).updateBatchById(anyList());
    }

    private void prepareMoveWithChild() {
        when(departments.selectById(20L)).thenReturn(department(20, 10, "0,10"));
        when(departments.selectById(0L)).thenReturn(null);
        var child = department(21, 20, "0,10,20");
        when(departments.lambda().findInSet(eq(20L), any(SFunction.class)).list())
            .thenReturn(List.of(child));
    }

    private SysDept move(long parentId) {
        var bo = new SysDeptBo();
        bo.setDeptId(20L);
        bo.setParentId(parentId);
        var updated = department(20, parentId, null);
        doNothing().when(service).checkDeptDataScope(parentId);
        try (var spring = mockStatic(SpringUtil.class); var mappings = mockStatic(MapstructUtils.class)) {
            mappings.when(() -> MapstructUtils.convert(bo, SysDept.class)).thenReturn(updated);
            service.updateDept(bo);
        }
        return updated;
    }

    private SysDept department(long id, long parent, String ancestors) {
        var dept = new SysDept();
        dept.setDeptId(id);
        dept.setParentId(parent);
        dept.setAncestors(ancestors);
        return dept;
    }
}
