package org.dromara.common.core.utils;

import cn.hutool.core.exceptions.ValidateException;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.core.utils.file.MimeTypeUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.core.utils.regex.RegexUtils;
import org.dromara.common.core.utils.regex.RegexValidator;
import org.dromara.common.core.xss.XssValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("common-core 公共工具边界单元测试")
class CoreUtilityBoundaryTest {

    /**
     * 验证下载文件名对中文、空格和加号执行 URL 编码，并同步写入浏览器可读取的响应头。
     */
    @Test
    @DisplayName("编码并设置下载文件名响应头")
    void shouldEncodeAndExposeAttachmentFileName() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        String encoded = "%E6%B5%8B%E8%AF%95%20report%2B1.xlsx";

        assertEquals(encoded, FileUtils.percentEncode("测试 report+1.xlsx"));

        FileUtils.setAttachmentResponseHeader(response, "测试 report+1.xlsx");

        verify(response).addHeader("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
        verify(response).setHeader("Content-disposition",
            "attachment; filename=" + encoded + ";filename*=utf-8''" + encoded);
        verify(response).setHeader("download-filename", encoded);
    }

    /**
     * 验证上传文件类型判断忽略扩展名大小写，同时拒绝未列入白名单的可执行文件。
     */
    @Test
    @DisplayName("识别允许的文件扩展名")
    void shouldRecognizeAllowedFileExtensionsCaseInsensitively() {
        assertTrue(MimeTypeUtils.isImage("JpEg"));
        assertTrue(MimeTypeUtils.isVideo("MP4"));
        assertTrue(MimeTypeUtils.isMedia("Mp3"));
        assertTrue(MimeTypeUtils.isDefaultAllowed("PDF"));
        assertFalse(MimeTypeUtils.isDefaultAllowed("exe"));
        assertFalse(MimeTypeUtils.isImage(null));
    }

    /**
     * 验证正则提取失败时稳定回退默认值，并校验账号与状态的有效边界。
     */
    @Test
    @DisplayName("处理正则提取与业务格式边界")
    void shouldHandleRegexExtractionAndValidationBoundaries() {
        assertEquals("42", RegexUtils.extractFromString("order-42", "order-(\\d+)", "none"));
        assertEquals("none", RegexUtils.extractFromString("missing", "order-(\\d+)", "none"));
        assertEquals("none", RegexUtils.extractFromString("order-42", "([", "none"));

        assertTrue(RegexValidator.isAccount("user_1"));
        assertFalse(RegexValidator.isAccount("1user"));
        assertFalse(RegexValidator.isAccount("user"));
        assertTrue(RegexValidator.isStatus("0"));
        assertTrue(RegexValidator.isStatus("1"));
        assertFalse(RegexValidator.isStatus("2"));
        ValidateException exception = assertThrows(ValidateException.class,
            () -> RegexValidator.validateAccount("bad", "账号格式错误"));
        assertEquals("账号格式错误", exception.getMessage());
    }

    /**
     * 验证反射工具可以沿 JavaBean 属性路径读取和修改嵌套对象，保障 Excel 等调用方的字段访问。
     */
    @Test
    @DisplayName("读写嵌套 JavaBean 属性")
    void shouldReadAndWriteNestedBeanProperties() {
        TestRoot root = new TestRoot(new TestChild("before"));

        assertEquals("before", ReflectUtils.invokeGetter(root, "child.name"));

        ReflectUtils.invokeSetter(root, "child.name", "after");

        assertEquals("after", root.getChild().getName());
    }

    /**
     * 验证 XSS 校验允许空值和普通文本，并拒绝任意 HTML 标签输入。
     */
    @Test
    @DisplayName("拒绝包含 HTML 标签的文本")
    void shouldRejectHtmlMarkup() {
        XssValidator validator = new XssValidator();

        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("plain text", null));
        assertFalse(validator.isValid("<script>alert(1)</script>", null));
        assertFalse(validator.isValid("hello <b>world</b>", null));
    }

    private static class TestRoot {
        private final TestChild child;

        /**
         * 创建带子对象的测试根对象。
         *
         * @param child 子对象
         */
        private TestRoot(TestChild child) {
            this.child = child;
        }

        /**
         * 返回子对象，供嵌套反射路径访问。
         *
         * @return 子对象
         */
        public TestChild getChild() {
            return child;
        }
    }

    private static class TestChild {
        private String name;

        /**
         * 创建具有初始名称的测试子对象。
         *
         * @param name 初始名称
         */
        private TestChild(String name) {
            this.name = name;
        }

        /**
         * 返回名称，供嵌套反射路径读取。
         *
         * @return 名称
         */
        public String getName() {
            return name;
        }

        /**
         * 修改名称，供嵌套反射路径写入。
         *
         * @param name 新名称
         */
        public void setName(String name) {
            this.name = name;
        }
    }
}
