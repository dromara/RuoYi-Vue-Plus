package com.wudgaby.stars.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仓库列表/详情 MyBatis 查询行
 */
@Data
public class StarsRepoCardRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long repoId;

    private String fullName;

    private String owner;

    private String repoName;

    private String description;

    private String language;

    private Integer stargazersCount;

    private String htmlUrl;

    private String readmeSnippet;

    private String category;

    private String tagNamesCsv;

    private String tagIdsCsv;

    private String summaryOneLiner;

    private String summaryText;

    private String summaryStatus;

    private String note;

    private String importSource;

    private String classificationSource;

    private String summarySource;

    private LocalDateTime importTime;

    private LocalDateTime updateTime;

}
