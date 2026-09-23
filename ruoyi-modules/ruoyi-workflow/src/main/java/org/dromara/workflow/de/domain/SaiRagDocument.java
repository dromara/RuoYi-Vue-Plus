package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_rag_document 实体（D-010 顺序推进批④ · Map 型存根 domain 化——列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
@TableName("sai_rag_document")
public class SaiRagDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long ragId;

    /**
     * 文档名
     */
    private String name;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源路径
     */
    private String sourcePath;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 存储类型
     */
    private String storageType;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 内容
     */
    private String content;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 分块数
     */
    private Integer chunkCount;

    /**
     * 页数
     */
    private Integer pageCount;

    /**
     * 元素数
     */
    private Integer elementCount;

    /**
     * 表格数
     */
    private Integer tableCount;

    /**
     * 图片数
     */
    private Integer imageCount;

    /**
     * 解析耗时ms
     */
    private Integer parseTime;

    /**
     * Markdown内容
     */
    private String mdContent;

    /**
     * 文档元数据
     */
    private String docMetadata;

    /**
     * 内容哈希
     */
    private String contentHash;

    /**
     * 关联资源ID
     */
    private Long resourceId;

    /**
     * 创建时间
     */
    private Date createDt;

    /**
     * 更新时间
     */
    private Date updateDt;
}
