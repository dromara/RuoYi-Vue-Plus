package org.dromara.system.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.event.OnlineUserCleanEvent;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.springframework.context.ApplicationContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleRevocationTest {

    @ParameterizedTest
    @CsvSource({"true,1","true,0","false,1","false,0"})
    void invalidatesAffectedUsersAfterRemovingRole(boolean batch, int rows) {
        var links = mock(SysUserRoleMapper.class, RETURNS_DEEP_STUBS);
        var chain = links.lambda();
        when(chain.eq(any(), any())).thenReturn(chain);
        var ids = List.of(8L, 9L);
        when(chain.in(any(), eq(ids))).thenReturn(chain);
        when(chain.deleteCount()).thenReturn(rows);
        var service = new SysRoleServiceImpl(mock(), mock(), links, mock());
        var link = new SysUserRole();
        link.setRoleId(19L);
        link.setUserId(8L);
        var context = mock(ApplicationContext.class);
        try (var login = mockStatic(LoginHelper.class); var spring = mockStatic(SpringUtils.class)) {
            login.when(LoginHelper::getUserId).thenReturn(7L);
            spring.when(SpringUtils::context).thenReturn(context);
            assertEquals(rows, batch ? service.deleteAuthUsers(19L, ids) : service.deleteAuthUser(link));
            verify(chain).eq(any(), eq(19L));
            if (batch) {
                verify(chain).in(any(), eq(ids));
            } else {
                verify(chain).eq(any(), eq(8L));
            }
            if (rows > 0) {
                verify(context).publishEvent(OnlineUserCleanEvent.byUsers(batch ? ids : List.of(8L)));
            } else {
                verifyNoInteractions(context);
            }
        }
    }
}
