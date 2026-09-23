package org.dromara.workflow.de.nc.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * NC-04 通知中心列表查询 BO（2026-09-23）。
 *
 * <p>用于 GET 列表端点的查询条件。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeNcNoticeQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 公告标题（模糊） */
    private String noticeTitle;

    /** 公告类型（精确） */
    private String noticeType;

    /** 公告状态（精确） */
    private String status;

    /** 创建者（精确） */
    private Long createBy;
}