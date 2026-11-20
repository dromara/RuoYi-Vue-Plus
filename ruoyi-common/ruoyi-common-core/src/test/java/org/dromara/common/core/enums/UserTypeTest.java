package org.dromara.common.core.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTypeTest {

    @Test
    void acceptsOnlyExactUserTypeWithoutEchoingInvalidInput() {
        assertEquals(UserType.SYS_USER, UserType.getUserType("sys_user"));
        assertEquals(UserType.APP_USER, UserType.getUserType("app_user"));
        assertEquals("用户类型无效", assertThrows(RuntimeException.class,
            () -> UserType.getUserType("prefix_sys_user_suffix")).getMessage());
        assertEquals("用户类型无效", assertThrows(RuntimeException.class,
            () -> UserType.getUserType("sys_user-7")).getMessage());
    }
}
