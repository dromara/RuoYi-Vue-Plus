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

    public S3StorageException(String message) {
        super(message);
    }

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public S3StorageException(Throwable cause) {
        super(cause);
    }

    public S3StorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static S3StorageException form(String message) {
        return new S3StorageException(message);
    }

    public static S3StorageException form(String message, Throwable cause) {
        return new S3StorageException(message, cause);
    }

    public static S3StorageException form(Throwable cause) {
        return new S3StorageException(cause);
    }

    public static S3StorageException form(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        return new S3StorageException(message, cause, enableSuppression, writableStackTrace);
    }

}
