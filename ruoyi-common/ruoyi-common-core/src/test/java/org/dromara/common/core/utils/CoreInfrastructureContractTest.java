package org.dromara.common.core.utils;

import jakarta.validation.ConstraintValidatorContext;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.common.core.service.DictService;
import org.dromara.common.core.validate.dicts.DictPattern;
import org.dromara.common.core.validate.dicts.DictPatternValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("common-core 基础设施契约单元测试")
class CoreInfrastructureContractTest {

    /**
     * 清理线程绑定的请求上下文，避免测试之间共享 Servlet 状态。
     */
    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 验证 Servlet 工具从 Spring 请求上下文读取参数、响应和会话，并保持类型转换约定。
     */
    @Test
    @DisplayName("读取线程绑定的 Servlet 上下文")
    void shouldReadRequestContextAndConvertParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("name", "alice");
        request.setParameter("age", "18");
        request.setParameter("enabled", "true");
        request.setParameter("roles", "admin", "user");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        assertSame(request, ServletUtils.getRequest());
        assertSame(response, ServletUtils.getResponse());
        assertEquals("alice", ServletUtils.getParameter("name"));
        assertEquals("fallback", ServletUtils.getParameter("missing", "fallback"));
        assertEquals(18, ServletUtils.getParameterToInt("age"));
        assertTrue(ServletUtils.getParameterToBool("enabled"));
        assertEquals("admin,user", ServletUtils.getParamMap(request).get("roles"));
        assertSame(request.getSession(), ServletUtils.getSession());
    }

    /**
     * 验证请求头解码、Ajax 识别、代理 IP 解析和 JSON 响应渲染保持稳定。
     */
    @Test
    @DisplayName("处理常用 HTTP 协议细节")
    void shouldHandleHeadersAjaxClientIpAndJsonRendering() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/report.json");
        request.addHeader("X-Name", ServletUtils.urlEncode("中文 value"));
        request.addHeader("X-Forwarded-For", "[2001:db8::1]");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals("中文 value", ServletUtils.getHeader(request, "X-Name"));
        assertEquals("", ServletUtils.getHeader(request, "missing"));
        assertEquals(ServletUtils.getHeaders(request).get("x-name"), request.getHeader("X-Name"));
        assertTrue(ServletUtils.isAjaxRequest(request));
        assertEquals("2001:db8::1", ServletUtils.getClientIP(request));

        ServletUtils.renderString(response, "{\"ok\":true}");

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("{\"ok\":true}", response.getContentAsString());
    }

    /**
     * 验证虚拟线程批量执行保持提交顺序，并把任务异常的真实原因传递给调用方。
     */
    @Test
    @DisplayName("批量执行虚拟线程任务")
    void shouldPreserveVirtualTaskOrderAndFailureCause() {
        List<Integer> results = ThreadUtils.virtualSubmitAll(
            () -> 1,
            () -> 2,
            () -> 3);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> ThreadUtils.virtualInvokeAll(() -> {
                throw new IllegalStateException("task-failed");
            }));

        assertEquals(List.of(1, 2, 3), results);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("task-failed"));
    }

    /**
     * 验证字典校验器按注解分隔符调用字典服务，并正确处理空值、缺失类型和未知字典值。
     */
    @Test
    @DisplayName("通过字典服务校验字段值")
    void shouldValidateDictionaryValuesThroughConfiguredService() {
        DictPattern annotation = mock(DictPattern.class);
        when(annotation.dictType()).thenReturn("sys_status");
        when(annotation.separator()).thenReturn("|");
        DictService dictService = mock(DictService.class);
        when(dictService.getDictLabel("sys_status", "0|1", "|")).thenReturn("正常|停用");
        when(dictService.getDictLabel("sys_status", "9", "|")).thenReturn("");
        DictPatternValidator validator = new DictPatternValidator();
        validator.initialize(annotation);

        try (MockedStatic<SpringUtil> spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(DictService.class)).thenReturn(dictService);

            assertTrue(validator.isValid(null, mock(ConstraintValidatorContext.class)));
            assertTrue(validator.isValid("0|1", null));
            assertFalse(validator.isValid("9", null));
        }

        DictPattern invalidAnnotation = mock(DictPattern.class);
        when(invalidAnnotation.dictType()).thenReturn(" ");
        DictPatternValidator invalidValidator = new DictPatternValidator();
        invalidValidator.initialize(invalidAnnotation);
        assertFalse(invalidValidator.isValid("0", null));
    }
}
