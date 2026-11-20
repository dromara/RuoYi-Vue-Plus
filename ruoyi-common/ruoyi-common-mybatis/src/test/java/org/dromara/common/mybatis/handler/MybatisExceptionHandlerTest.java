package org.dromara.common.mybatis.handler;

import org.dromara.common.core.domain.R;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MybatisExceptionHandlerTest {

    @Test
    void hidesDatabaseExceptionDetails() {
        MyBatisSystemException exception = mock(MyBatisSystemException.class);
        when(exception.getMessage()).thenReturn("SQL failed against internal_table");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/records");

        R<Void> result = new MybatisExceptionHandler().handleCannotFindDataSourceException(exception, request);

        assertEquals(500, result.getCode());
        assertEquals("数据库操作失败，请联系管理员", result.getMsg());
    }
}
