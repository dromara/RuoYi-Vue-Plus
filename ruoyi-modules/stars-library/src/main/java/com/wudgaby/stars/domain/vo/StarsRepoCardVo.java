package com.wudgaby.stars.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户仓库卡片视图
 */
@Data
public class StarsRepoCardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String fullName;

    private String owner;

    private String repoName;

    private String language;

    private Integer stargazersCount;

    private String category;

    private List<String> tags;

    private String summaryOneLiner;

    private String summaryText;

    private String summaryStatus;

    private String note;

    private String importSource;

    private String githubUrl;

    private String zreadUrl;

    private String deepwikiUrl;

}
