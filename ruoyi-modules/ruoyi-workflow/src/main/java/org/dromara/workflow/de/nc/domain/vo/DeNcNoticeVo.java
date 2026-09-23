package org.dromara.workflow.de.nc.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * NC-04 通知中心响应 VO（2026-09-23）。
 *
 * <p>对应 {@code sys_notice} 全部字段，供列表 / 详情端点使用。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeNcNoticeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long noticeId;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String status;
    private Long createDept;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
    private String remark;
}