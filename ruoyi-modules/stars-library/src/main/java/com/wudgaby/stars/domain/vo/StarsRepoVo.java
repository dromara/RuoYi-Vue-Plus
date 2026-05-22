package com.wudgaby.stars.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.wudgaby.stars.domain.StarsRepo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仓库列表视图对象
 */
@Data
@AutoMapper(target = StarsRepo.class)
public class StarsRepoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String fullName;

    private String owner;

    private String repoName;

    private String description;

    private String language;

    private Integer stargazersCount;

    private String htmlUrl;

}
