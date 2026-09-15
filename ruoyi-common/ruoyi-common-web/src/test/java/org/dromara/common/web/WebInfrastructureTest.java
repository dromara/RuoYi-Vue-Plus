package org.dromara.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.enhance.JsonValueEnhancer;
import org.dromara.common.web.advice.ResponseEnhancementAdvice;
import org.dromara.common.web.config.properties.XssProperties;
import org.dromara.common.web.filter.RepeatableFilter;
import org.dromara.common.web.filter.RepeatedlyRequestWrapper;
import org.dromara.common.web.filter.XssFilter;
import org.dromara.common.web.filter.XssHttpServletRequestWrapper;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.boot.json.JsonParseException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.expression.ExpressionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("common-web 基础设施单元测试")
class WebInfrastructureTest {

    /**
     * 验证请求体被缓存后可以通过输入流和字符读取器分别完整读取。
     */
    @Test
    @DisplayName("重复读取缓存的请求体")
    void shouldReadCachedRequestBodyRepeatedly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContent("中文-body".getBytes(StandardCharsets.UTF_8));

        RepeatedlyRequestWrapper wrapper = new RepeatedlyRequestWrapper(request, response);

        assertEquals("中文-body", new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("中文-body", wrapper.getReader().readLine());
        assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());
        assertEquals(StandardCharsets.UTF_8.name(), response.getCharacterEncoding());
    }

    /**
     * 验证可重复读过滤器只包装 JSON 请求，普通文本请求保持原对象透传。
     */
    @Test
    @DisplayName("仅包装 JSON 请求")
    void shouldWrapOnlyJsonRequest() throws Exception {
        RepeatableFilter filter = new RepeatableFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ServletRequest> forwarded = new AtomicReference<>();
        FilterChain chain = (request, servletResponse) -> forwarded.set(request);
        MockHttpServletRequest jsonRequest = request("POST", "/json", MediaType.APPLICATION_JSON_VALUE);
        jsonRequest.setContent("{}".getBytes(StandardCharsets.UTF_8));

        filter.doFilter(jsonRequest, response, chain);
        assertInstanceOf(RepeatedlyRequestWrapper.class, forwarded.get());

        MockHttpServletRequest textRequest = request("POST", "/text", MediaType.TEXT_PLAIN_VALUE);
        filter.doFilter(textRequest, response, chain);
        assertSame(textRequest, forwarded.get());
    }

    /**
     * 验证 XSS 过滤器跳过只读和排除路径请求，并包装需要清洗的写请求。
     */
    @Test
    @DisplayName("按请求方法和排除路径执行 XSS 包装")
    void shouldApplyXssWrapperOnlyToIncludedWriteRequests() throws Exception {
        XssProperties properties = new XssProperties();
        properties.setExcludeUrls(List.of("/upload/**"));
        XssFilter filter = new XssFilter(properties);
        filter.init(null);
        AtomicReference<ServletRequest> forwarded = new AtomicReference<>();
        FilterChain chain = (request, response) -> forwarded.set(request);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpServletRequest getRequest = request("GET", "/users", null);
        filter.doFilter(getRequest, response, chain);
        assertSame(getRequest, forwarded.get());

        MockHttpServletRequest excludedRequest = request("POST", "/upload/avatar", null);
        filter.doFilter(excludedRequest, response, chain);
        assertSame(excludedRequest, forwarded.get());

        MockHttpServletRequest writeRequest = request("POST", "/users", null);
        filter.doFilter(writeRequest, response, chain);
        assertInstanceOf(XssHttpServletRequestWrapper.class, forwarded.get());
    }

    /**
     * 验证响应增强仅作用于 JSON 响应，并将转换器支持判断委托给增强器。
     */
    @Test
    @DisplayName("仅增强 JSON 响应体")
    void shouldEnhanceOnlyJsonResponseBody() {
        JsonValueEnhancer enhancer = mock(JsonValueEnhancer.class);
        when(enhancer.supports(JacksonJsonHttpMessageConverter.class)).thenReturn(true);
        when(enhancer.enhance("body")).thenReturn("enhanced");
        ResponseEnhancementAdvice advice = new ResponseEnhancementAdvice(enhancer);

        assertTrue(advice.supports(null, JacksonJsonHttpMessageConverter.class));
        assertEquals("enhanced", advice.beforeBodyWrite("body", null, MediaType.APPLICATION_JSON,
            JacksonJsonHttpMessageConverter.class, null, null));
        assertEquals("body", advice.beforeBodyWrite("body", null, MediaType.TEXT_PLAIN,
            JacksonJsonHttpMessageConverter.class, null, null));
        verify(enhancer).enhance("body");
    }

    /**
     * 验证业务异常的自定义状态码和默认失败状态码均被正确映射到统一响应。
     */
    @Test
    @DisplayName("映射业务异常状态码")
    void shouldMapServiceExceptionCodes() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("POST", "/orders", null);

        R<Void> forbidden = handler.handleServiceException(
            new ServiceException("forbidden", HttpStatus.FORBIDDEN), request);
        R<Void> failed = handler.handleServiceException(new ServiceException("failed"), request);

        assertEquals(HttpStatus.FORBIDDEN, forbidden.getCode());
        assertEquals("forbidden", forbidden.getMsg());
        assertEquals(HttpStatus.ERROR, failed.getCode());
        assertEquals("failed", failed.getMsg());
    }

    /**
     * 验证参数类型不匹配异常会返回包含参数名、目标类型和原始值的可诊断消息。
     */
    @Test
    @DisplayName("映射参数类型不匹配异常")
    void shouldMapArgumentTypeMismatchDetails() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("userId");
        doReturn(Long.class).when(exception).getRequiredType();
        when(exception.getValue()).thenReturn("abc");

        R<Void> result = new GlobalExceptionHandler().handleMethodArgumentTypeMismatchException(
            exception, request("GET", "/users/abc", null));

        assertEquals(HttpStatus.ERROR, result.getCode());
        assertEquals("请求参数类型不匹配，参数[userId]要求类型为：'java.lang.Long'，但输入值为：'abc'", result.getMsg());
    }

    /**
     * 验证 Spring Web 与 Jackson 的常见异常被映射为项目约定的状态码和稳定提示。
     */
    @Test
    @DisplayName("映射框架解析与路由异常")
    void shouldMapFrameworkExceptionsToStableResponses() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = request("POST", "/missing", null);
        HttpRequestMethodNotSupportedException method = mock(HttpRequestMethodNotSupportedException.class);
        when(method.getMethod()).thenReturn("POST");
        when(method.getMessage()).thenReturn("Method POST is not supported");
        NoHandlerFoundException noHandler = mock(NoHandlerFoundException.class);

        R<Void> methodResult = handler.handleHttpRequestMethodNotSupported(method, request);
        R<Void> routeResult = handler.handleNoHandlerFoundException(noHandler, request);
        R<Void> jsonResult = handler.handleJsonParseException(mock(JsonParseException.class), request);
        R<Void> bodyResult = handler.handleHttpMessageNotReadableException(
            mock(HttpMessageNotReadableException.class), request);
        R<Void> spelResult = handler.handleSpelException(new ExpressionException("bad expression"), request);

        assertEquals(405, methodResult.getCode());
        assertEquals("Method POST is not supported", methodResult.getMsg());
        assertEquals(404, routeResult.getCode());
        assertEquals("请求地址不存在", routeResult.getMsg());
        assertEquals(400, jsonResult.getCode());
        assertEquals("请求数据格式错误", jsonResult.getMsg());
        assertEquals(400, bodyResult.getCode());
        assertEquals("请求参数格式错误", bodyResult.getMsg());
        assertEquals(500, spelResult.getCode());
        assertEquals("SpEL解析失败：bad expression", spelResult.getMsg());
    }

    /**
     * 验证未知异常返回带可追踪编号的统一提示，而不会泄漏原始异常内容。
     */
    @Test
    @DisplayName("隐藏未知异常并生成错误编号")
    void shouldHideUnexpectedFailureBehindTraceableErrorId() {
        R<Void> result = new GlobalExceptionHandler().handleRuntimeException(
            new RuntimeException("database password leaked"), request("GET", "/users", null));

        assertEquals(HttpStatus.ERROR, result.getCode());
        assertTrue(result.getMsg().matches("发生未知异常，请联系管理员 \\[错误编号: \\d{8}]"));
        assertTrue(!result.getMsg().contains("password"));
    }

    /**
     * 创建带请求方法、路径和内容类型的 Mock 请求。
     *
     * @param method      HTTP 方法
     * @param path        servlet 路径
     * @param contentType 内容类型
     * @return Mock 请求
     */
    private static MockHttpServletRequest request(String method, String path, String contentType) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setContentType(contentType);
        return request;
    }
}
