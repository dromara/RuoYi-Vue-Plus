package com.wudgaby.stars.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.wudgaby.stars.domain.StarsUserRepo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-仓库关系列表视图对象
 */
@Data
@AutoMapper(target = StarsUserRepo.class)
public class StarsUserRepoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long repoId;

    private String importSource;

    private String note;

    private String category;

    private String summaryOneLiner;

    private String summaryStatus;

    private LocalDateTime importTime;

    private LocalDateTime updateTime;

}
