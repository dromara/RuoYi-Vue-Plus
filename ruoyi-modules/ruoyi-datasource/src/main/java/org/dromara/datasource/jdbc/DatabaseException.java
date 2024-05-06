package org.dromara.datasource.jdbc;

import java.io.Serial;

/**
 * Database 异常类
 *
 * @author ixyxj
 */
public class DatabaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable throwable) {
        super(throwable);
    }

    public DatabaseException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
