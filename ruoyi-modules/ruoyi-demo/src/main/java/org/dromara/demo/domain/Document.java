package org.dromara.demo.domain;

import lombok.Data;

/**
 * 文档实体
 */
@Data
public class Document {

    /**
     * es中的唯一id
     */
    private String id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    private String content;

}
