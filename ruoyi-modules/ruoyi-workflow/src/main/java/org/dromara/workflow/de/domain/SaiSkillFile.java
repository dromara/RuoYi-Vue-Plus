package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * sai_skill_file 实体（Map 型存根 domain 化——列真源 ry_ai.sql DDL「6.2 Skill 支撑文件内容表」）。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Data
@TableName("sai_skill_file")
public class SaiSkillFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** Skill ID */
    private Long skillId;

    /** 文件相对路径 */
    private String filePath;

    /** 文件内容 */
    private String content;

    /** 文件大小（字节） */
    private Integer fileSize;

    /** 编码方式 */
    private String encoding;

    /** 创建时间（DDL 列名为 created_at） */
    private Date createdAt;

    /** 更新时间（DDL 列名为 updated_at） */
    private Date updatedAt;
}
