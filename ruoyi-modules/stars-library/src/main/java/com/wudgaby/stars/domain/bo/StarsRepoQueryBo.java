package com.wudgaby.stars.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 仓库列表查询条件
 */
@Data
public class StarsRepoQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String keyword;

    private String category;

    private List<Long> tagIds;

    private String importSource;

    private String summaryStatus;

    private String orderBy;

}
