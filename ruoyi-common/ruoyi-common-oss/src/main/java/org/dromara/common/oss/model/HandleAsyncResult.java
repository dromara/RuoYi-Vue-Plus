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

    /**
     * 获取异步处理结果。
     *
     * @return 异步处理结果
     */
    public Optional<T> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * 获取异步处理异常。
     *
     * @return 异步处理异常
     */
    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    /**
     * 是否处理成功。
     *
     * @return true 成功 false 失败
     */
    public boolean isSuccess() {
        return getError().isEmpty();
    }

    /**
     * 是否处理失败。
     *
     * @return true 失败 false 成功
     */
    public boolean isFailure() {
        return getError().isPresent();
    }

    /**
     * 构建异步处理结果。
     *
     * @param result 结果
     * @param error  异常
     * @param <T>    结果类型
     * @return 异步处理结果
     */
    public static <T> HandleAsyncResult<T> of(T result, Throwable error) {
        return new HandleAsyncResult<T>(result, error);
    }

    /**
     * 构建成功的异步处理结果。
     *
     * @param result 结果
     * @param <T>    结果类型
     * @return 异步处理结果
     */
    public static <T> HandleAsyncResult<T> success(T result) {
        return new HandleAsyncResult<T>(result, null);
    }

    /**
     * 构建失败的异步处理结果。
     *
     * @param error 异常
     * @param <T>   结果类型
     * @return 异步处理结果
     */
    public static <T> HandleAsyncResult<T> failure(Throwable error) {
        return new HandleAsyncResult<T>(null, error);
    }
}
