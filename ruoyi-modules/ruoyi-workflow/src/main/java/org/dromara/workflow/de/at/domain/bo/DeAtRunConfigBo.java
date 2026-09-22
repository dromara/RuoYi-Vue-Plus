package org.dromara.workflow.de.at.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * AT-04 自动化运行配置 BO（并发 / 去重 / 重试 / 错过补跑 / 失败通知）。
 *
 * @author AIP
 */
@Data
public class DeAtRunConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long jobId;

    private Integer concurrency;
    private Boolean dedup;
    private Integer maxRetries;
    private Boolean missRun;
    private Boolean notifyOnFailure;
}