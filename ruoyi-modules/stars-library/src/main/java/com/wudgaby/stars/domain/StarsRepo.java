package com.wudgaby.stars.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仓库全局缓存 stars_repo
 */
@Data
@TableName("stars_repo")
public class StarsRepo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * owner/repo
     */
    private String fullName;

    /**
     * 仓库所有者
     */
    private String owner;

    /**
     * 仓库名
     */
    private String repoName;

    /**
     * 仓库描述
     */
    private String description;

    /**
     * 主要编程语言
     */
    private String language;

    /**
     * Star 数
     */
    private Integer stargazersCount;

    /**
     * GitHub 页面 URL
     */
    private String htmlUrl;

    /**
     * README 前 3000 字符缓存
     */
    private String readmeSnippet;

    /**
     * README 缓存时间
     */
    private LocalDateTime readmeCachedAt;

    /**
     * GitHub 侧更新时间
     */
    private LocalDateTime githubUpdatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
