package org.dromara.system.service.impl;

import org.dromara.system.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import org.dromara.system.domain.SysRoleDept;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.dromara.system.mapper.SysRoleDeptMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysDataScopeServiceImplTest {
    private final SysDeptMapper departments = mock();
    private final SysRoleDeptMapper assignments = mock();
    private final SysDataScopeServiceImpl service = new SysDataScopeServiceImpl(assignments, departments);

    @Test
    void missingIdsProduceEmptyScope() {
        assertEquals("-1", service.getRoleCustom(null));
        assertEquals("-1", service.getDeptAndChild(null));
        verifyNoInteractions(assignments, departments);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void returnsOnlyDepartmentsAssignedToRequestedRole(boolean hasAssignments) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "scope"), SysRoleDept.class);
        when(assignments.currentModelClass()).thenReturn(SysRoleDept.class);
        var query = new LambdaCrudChainWrapper<SysRoleDept, SysRoleDept>(assignments);
        when(assignments.lambda()).thenReturn(query);
        var first = new SysRoleDept();
        first.setDeptId(12L);
        var second = new SysRoleDept();
        second.setDeptId(18L);
        when(assignments.selectList(query)).thenReturn(hasAssignments ? List.of(first, second) : List.of());

        assertEquals(hasAssignments ? "12,18" : "-1", service.getRoleCustom(41L));
        assertTrue(query.getSqlSegment().contains("role_id ="));
        assertEquals(List.of(41L), List.copyOf(query.getParamNameValuePairs().values()));
    }

    @Test
    void departmentScopeUsesReturnedIdsOrEmptyScopeMarker() {
        when(departments.selectDeptAndChildById(21L)).thenReturn(List.of(22L, 21L));
        when(departments.selectDeptAndChildById(42L)).thenReturn(List.of());
        assertEquals("22,21", service.getDeptAndChild(21L));
        assertEquals("-1", service.getDeptAndChild(42L));
    }
}
