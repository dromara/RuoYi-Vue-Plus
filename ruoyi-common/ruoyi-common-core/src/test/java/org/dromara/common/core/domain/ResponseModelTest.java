package org.dromara.common.core.domain;

import org.dromara.common.core.constant.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("公共响应模型单元测试")
class ResponseModelTest {

    /**
     * 验证响应工厂方法设置正确的状态码、消息和业务数据。
     */
    @Test
    @DisplayName("构建成功、失败和警告响应")
    void responseFactoriesShouldSetExpectedFields() {
        R<String> success = R.ok("done", "payload");
        R<String> failure = R.fail("failed", "payload");
        R<String> warning = R.warn("warning", "payload");

        assertAll(
            () -> assertEquals(HttpStatus.SUCCESS, success.getCode()),
            () -> assertEquals("done", success.getMsg()),
            () -> assertEquals("payload", success.getData()),
            () -> assertEquals(HttpStatus.ERROR, failure.getCode()),
            () -> assertEquals(HttpStatus.WARN, warning.getCode())
        );
    }

    /**
     * 验证成功状态判断能够处理失败响应和空响应。
     */
    @Test
    @DisplayName("正确识别响应成功状态")
    void responseStatusChecksShouldHandleNullAndError() {
        assertTrue(R.isSuccess(R.ok()));
        assertFalse(R.isSuccess(R.fail()));
        assertFalse(R.isSuccess(null));
        assertTrue(R.isError(null));
    }

    /**
     * 验证仅传集合时分页总数使用集合实际大小。
     */
    @Test
    @DisplayName("分页结果按集合大小计算总数")
    void pageResultShouldUseCollectionSizeAsTotal() {
        PageResult<String> result = PageResult.build(List.of("a", "b"));

        assertEquals(2L, result.getTotal());
        assertEquals(List.of("a", "b"), result.getRows());
    }

    /**
     * 验证空行集合会被标准化为空列表，避免调用方判空。
     */
    @Test
    @DisplayName("分页结果将空集合参数转换为空列表")
    void pageResultShouldNormalizeNullRows() {
        PageResult<String> result = PageResult.build(null, 10L);
        PageResult<String> constructed = new PageResult<>(null, 5L);

        assertNotNull(result.getRows());
        assertTrue(result.getRows().isEmpty());
        assertTrue(constructed.getRows().isEmpty());
        assertEquals(10L, result.getTotal());
    }

}
