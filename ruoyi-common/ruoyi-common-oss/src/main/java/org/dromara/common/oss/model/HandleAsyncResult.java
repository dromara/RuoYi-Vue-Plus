package org.dromara.common.oss.model;

import java.util.Optional;

/**
 * 处理异步结果
 *
 * @param result 结果
 * @param error  异常错误
 * @param <T>    结果类型
 * @author 秋辞未寒
 */
public record HandleAsyncResult<T>(
        T result,
        Throwable error
) {

    public Optional<T> getResult() {
        return Optional.ofNullable(result);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    public boolean isSuccess() {
        return getError().isEmpty();
    }

    public boolean isFailure() {
        return getError().isPresent();
    }

    public static <T> HandleAsyncResult<T> of(T result, Throwable error) {
        return new HandleAsyncResult<T>(result, error);
    }

    public static <T> HandleAsyncResult<T> success(T result) {
        return new HandleAsyncResult<T>(result, null);
    }

    public static <T> HandleAsyncResult<T> failure(Throwable error) {
        return new HandleAsyncResult<T>(null, error);
    }
}
