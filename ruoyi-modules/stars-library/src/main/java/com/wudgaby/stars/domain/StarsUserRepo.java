package com.wudgaby.stars.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-仓库关系 stars_user_repo
 */
@Data
@TableName("stars_user_repo")
public class StarsUserRepo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * RuoYi 用户 ID
     */
    private Long userId;

    /**
     * 仓库 ID
     */
    private Long repoId;

    /**
     * 导入来源：self | github_username
     */
    private String importSource;

    /**
     * 收藏理由/备注
     */
    private String note;

    /**
     * 主分类
     */
    private String category;

    /**
     * 分类来源：ai | manual
     */
    private String classificationSource;

    /**
     * 中文一句话概述
     */
    private String summaryOneLiner;

    /**
     * 中文概述
     */
    private String summaryText;

    /**
     * 概述状态：pending | processing | done | failed | manual
     */
    private String summaryStatus;

    /**
     * 概述来源：ai | manual
     */
    private String summarySource;

    /**
     * 导入时间
     */
    private LocalDateTime importTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
