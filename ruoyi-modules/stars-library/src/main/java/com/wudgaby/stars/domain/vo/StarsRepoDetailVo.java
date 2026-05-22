package com.wudgaby.stars.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户仓库详情视图
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StarsRepoDetailVo extends StarsRepoCardVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long repoId;

    private String description;

    private String readmeSnippet;

    private String classificationSource;

    private String summarySource;

    private LocalDateTime importTime;

    private LocalDateTime updateTime;

    private List<Long> tagIds;

}
