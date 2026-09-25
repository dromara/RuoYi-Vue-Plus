package org.dromara.common.web;

import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.web.core.I18nLocaleResolver;
import org.dromara.common.web.core.WaveAndCircleCaptcha;
import org.dromara.common.web.filter.XssHttpServletRequestWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.awt.Image;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("common-web 功能单元测试")
class WebFunctionTest {

    /**
     * 验证请求头可以使用短横线或下划线解析区域信息，并支持默认区域回退。
     */
    @Test
    @DisplayName("解析请求语言区域")
    void shouldResolveRequestLocale() {
        I18nLocaleResolver resolver = new I18nLocaleResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("content-language", "zh_CN");

        assertEquals(Locale.SIMPLIFIED_CHINESE, resolver.resolveLocale(request));

        MockHttpServletRequest emptyRequest = new MockHttpServletRequest();
        assertEquals(Locale.getDefault(), resolver.resolveLocale(emptyRequest));
        assertDoesNotThrow(() -> resolver.setLocale(request, new MockHttpServletResponse(), Locale.ENGLISH));
    }

    /**
     * 验证普通参数和多值参数中的 HTML 标签会被移除并清理首尾空白。
     */
    @Test
    @DisplayName("清洗请求参数中的 HTML 标签")
    void shouldSanitizeRequestParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("name", " <b>admin</b> ");
        request.setParameter("roles", "<i>user</i>", " <script>root</script> ");
        XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);

        assertEquals("admin", wrapper.getParameter("name"));
        assertArrayEquals(new String[]{"user", "root"}, wrapper.getParameterValues("roles"));
        assertArrayEquals(new String[]{"user", "root"}, wrapper.getParameterMap().get("roles"));
    }

    /**
     * 验证 JSON 请求体执行 XSS 清洗，非 JSON 请求仍使用原始输入流。
     *
     * @throws Exception 读取测试请求体失败
     */
    @Test
    @DisplayName("按内容类型清洗 JSON 请求体")
    void shouldSanitizeOnlyJsonRequestBody() throws Exception {
        MockHttpServletRequest jsonRequest = new MockHttpServletRequest();
        jsonRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonRequest.setContent("{\"name\":\"<b>admin</b>\"}".getBytes(StandardCharsets.UTF_8));
        XssHttpServletRequestWrapper jsonWrapper = new XssHttpServletRequestWrapper(jsonRequest);

        String sanitized = new String(jsonWrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(jsonWrapper.isJsonRequest());
        assertEquals("{\"name\":\"admin\"}", sanitized);

        MockHttpServletRequest textRequest = new MockHttpServletRequest();
        textRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
        textRequest.setContent("<b>raw</b>".getBytes(StandardCharsets.UTF_8));
        XssHttpServletRequestWrapper textWrapper = new XssHttpServletRequestWrapper(textRequest);
        assertFalse(textWrapper.isJsonRequest());
        assertEquals("<b>raw</b>", new String(textWrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    /**
     * 验证控制器通用响应转换和重定向字符串格式。
     */
    @Test
    @DisplayName("转换控制器操作结果")
    void shouldConvertControllerResults() {
        TestController controller = new TestController();

        assertEquals(HttpStatus.SUCCESS, controller.rows(1).getCode());
        assertEquals(HttpStatus.ERROR, controller.rows(0).getCode());
        assertEquals(HttpStatus.SUCCESS, controller.result(true).getCode());
        assertEquals("redirect:/index", controller.redirect("/index"));
    }

    /**
     * 验证自定义验证码仍兼容 Hutool 的验证码生命周期，并生成符合配置尺寸和字符数的图片。
     */
    @Test
    @DisplayName("生成带干扰元素的验证码")
    void shouldGenerateCaptchaThroughHutoolContract() {
        WaveAndCircleCaptcha captcha = new WaveAndCircleCaptcha(160, 60, 5, 4);

        captcha.createCode();
        Image image = captcha.getImage();

        assertEquals(5, captcha.getCode().length());
        assertEquals(160, image.getWidth(null));
        assertEquals(60, image.getHeight(null));
        assertTrue(captcha.verify(captcha.getCode()));
        assertFalse(captcha.verify("incorrect"));
    }

    private static class TestController extends BaseController {

        /**
         * 暴露受保护的行数转换方法供测试调用。
         *
         * @param rows 影响行数
         * @return 统一响应
         */
        private R<Void> rows(int rows) {
            return toAjax(rows);
        }

        /**
         * 暴露受保护的布尔转换方法供测试调用。
         *
         * @param result 操作结果
         * @return 统一响应
         */
        private R<Void> result(boolean result) {
            return toAjax(result);
        }
    }
}
