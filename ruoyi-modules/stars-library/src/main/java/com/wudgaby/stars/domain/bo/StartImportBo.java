package com.wudgaby.stars.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 启动 Stars 导入请求（limit 为空时使用配置默认值）
 */
@Data
public class StartImportBo {

    /**
     * 导入条数上限（取 GitHub 最近 starred 的 N 条）
     */
    @Min(1)
    @Max(10000)
    private Integer limit;
}
