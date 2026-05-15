package org.dromara.common.oss.exception;

import java.io.Serial;

/**
 * S3对象存储异常
 *
 * @author 秋辞未寒
 */
public class S3StorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 使用异常消息构造 S3 对象存储异常。
     *
     * @param message 异常消息
     */
    public S3StorageException(String message) {
        super(message);
    }

    /**
     * 使用异常消息和原因构造 S3 对象存储异常。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用异常原因构造 S3 对象存储异常。
     *
     * @param cause 异常原因
     */
    public S3StorageException(Throwable cause) {
        super(cause);
    }

    /**
     * 使用完整异常参数构造 S3 对象存储异常。
     *
     * @param message            异常消息
     * @param cause              异常原因
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 是否写入堆栈
     */
    public S3StorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param message 异常消息
     * @return S3 对象存储异常
     */
    public static S3StorageException form(String message) {
        return new S3StorageException(message);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param message 异常消息
     * @param cause   异常原因
     * @return S3 对象存储异常
     */
    public static S3StorageException form(String message, Throwable cause) {
        return new S3StorageException(message, cause);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param cause 异常原因
     * @return S3 对象存储异常
     */
    public static S3StorageException form(Throwable cause) {
        return new S3StorageException(cause);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param message            异常消息
     * @param cause              异常原因
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 是否写入堆栈
     * @return S3 对象存储异常
     */
    public static S3StorageException form(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        return new S3StorageException(message, cause, enableSuppression, writableStackTrace);
    }

}
