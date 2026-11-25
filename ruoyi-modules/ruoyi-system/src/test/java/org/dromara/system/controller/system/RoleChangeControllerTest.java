package org.dromara.system.controller.system;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.domain.bo.SysRoleBo;
import org.dromara.system.event.OnlineUserCleanEvent;
import org.dromara.system.service.ISysRoleService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleChangeControllerTest {
    private final ISysRoleService roles = mock();
    private final SysRoleController controller = new SysRoleController(roles, mock(), mock());
    private final ApplicationContext context = mock();

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void permissionWriteInvalidatesSessionsOnlyForChangedRole(int rows) {
        var role = new SysRoleBo();
        role.setRoleId(17L);
        role.setMenuIds(new Long[]{9L});
        when(roles.updateRolePermission(role)).thenReturn(rows);

        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(context);
            var response = controller.editPermission(role);
            verify(roles).updateRolePermission(role);
            assertRoleChangeResult(response, rows);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void statusWriteInvalidatesSessionsOnlyForChangedRole(int rows) {
        var role = new SysRoleBo();
        role.setRoleId(17L);
        role.setStatus("1");
        when(roles.updateRoleStatus(17L, "1")).thenReturn(rows);

        try (var spring = mockStatic(SpringUtils.class)) {
            spring.when(SpringUtils::context).thenReturn(context);
            var response = controller.changeStatus(role);
            verify(roles).updateRoleStatus(17L, "1");
            assertRoleChangeResult(response, rows);
        }
    }

    private void assertRoleChangeResult(R<Void> response, int rows) {
        assertEquals(rows > 0, R.isSuccess(response));
        if (rows > 0) {
            verify(context).publishEvent(OnlineUserCleanEvent.byRole(17L));
            verifyNoMoreInteractions(context);
        } else {
            verifyNoInteractions(context);
        }
    }
}
