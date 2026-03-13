package org.dromara.common.oss.exception;

import java.io.Serial;

/**
 * OSS异常类
 *
 * @author Lion Li
 */
public class OssException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建 OSS 业务异常。
     *
     * @param msg 异常消息
     */
    public OssException(String msg) {
        super(msg);
    }

}
