package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_rag_chunk 实体（Map 型存根 domain 化——列真源 ry_ai.sql DDL「4.3 RAG 分块表」）。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Data
@TableName("sai_rag_chunk")
public class SaiRagChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 所属知识库ID */
    private Long ragId;

    /** 所属文档ID */
    private Long documentId;

    /** 段落序号 */
    private Integer paragraphIndex;

    /** 分块序号 */
    private Integer chunkIndex;

    /** 分块内容 */
    private String content;

    /** token 数 */
    private Integer tokenCount;

    /** 向量ID */
    private String vectorId;

    /** chunk 内容 SHA-256，用于向量去重 */
    private String contentHash;

    /** chunk 来源类型：TEXT 文本、IMAGE 图片 */
    private String sourceType;

    /** 创建时间 */
    private Date createDt;

    /** 更新时间 */
    private Date updateDt;
}
