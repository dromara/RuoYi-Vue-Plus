package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_rag_document_image 实体（Map 型存根 domain 化——列真源 ry_ai.sql DDL「4.2 RAG 文档图片表」）。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Data
@TableName("sai_rag_document_image")
public class SaiRagDocumentImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 所属知识库ID */
    private Long ragId;

    /** 所属文档ID */
    private Long documentId;

    /** 所属分块ID */
    private Long chunkId;

    /** 关联资源ID */
    private Long resourceId;

    /** 图片序号 */
    private Integer imageIndex;

    /** 图片URL */
    private String imageUrl;

    /** 图片说明 */
    private String caption;

    /** 图编号 */
    private String figureNo;

    /** 图标题 */
    private String figureTitle;

    /** 章节标题 */
    private String sectionTitle;

    /** 来源页码 */
    private Integer sourcePage;

    /** 文档名 */
    private String documentName;

    /** OCR 文本 */
    private String ocrText;

    /** 创建时间 */
    private Date createDt;

    /** 更新时间 */
    private Date updateDt;
}
