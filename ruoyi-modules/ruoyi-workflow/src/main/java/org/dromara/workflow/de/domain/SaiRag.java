package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_rag 实体（D-010 顺序推进批④ · Map 型存根 domain 化——列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
@TableName("sai_rag")
public class SaiRag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 知识库名
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标
     */
    private String icon;

    /**
     * 嵌入模型ID
     */
    private Long embeddingModelId;

    /**
     * 向量维度
     */
    private Integer dimensionOfVectorModel;

    /**
     * 重排模型ID
     */
    private Long rerankModelId;

    /**
     * 搜索引擎实例ID
     */
    private Long searchEngineInstanceId;

    /**
     * 向量库实例ID
     */
    private Long vectorStoreInstanceId;

    /**
     * 搜索引擎启用
     */
    private Integer searchEngineEnable;

    /**
     * 分隔符
     */
    private String delimiter;

    /**
     * RAG增强
     */
    private String ragEnhancement;

    /**
     * 配置
     */
    private String config;

    /**
     * 去重策略
     */
    private Integer dedupStrategy;

    /**
     * 冲突动作
     */
    private Integer dedupAction;

    /**
     * 上传确认
     */
    private Integer uploadConfirm;

    /**
     * 创建时间
     */
    private Date createDt;

    /**
     * 更新时间
     */
    private Date updateDt;
}
