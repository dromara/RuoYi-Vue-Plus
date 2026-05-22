package com.wudgaby.stars.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.wudgaby.stars.domain.StarsImportJob;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导入任务列表视图对象
 */
@Data
@AutoMapper(target = StarsImportJob.class)
public class StarsImportJobVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String jobType;

    private String sourceLogin;

    private Integer importLimit;

    private String status;

    private Integer totalCount;

    private Integer processedCount;

    private Integer failedCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

}
