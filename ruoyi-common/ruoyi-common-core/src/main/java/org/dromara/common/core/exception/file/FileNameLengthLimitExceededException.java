package org.dromara.common.core.exception.file;

import java.io.Serial;

/**
 * 文件名称超长限制异常类
 *
 * @author ruoyi
 */
public class FileNameLengthLimitExceededException extends FileException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造文件名称超长异常。
     *
     * @param defaultFileNameLength 默认文件名长度限制
     */
    public FileNameLengthLimitExceededException(int defaultFileNameLength) {
        super("upload.filename.exceed.length", new Object[]{defaultFileNameLength});
    }
}
