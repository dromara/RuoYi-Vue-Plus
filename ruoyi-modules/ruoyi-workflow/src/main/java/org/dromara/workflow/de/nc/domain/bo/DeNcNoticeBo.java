package org.dromara.workflow.de.nc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * NC-04 通知中心 CRUD 请求 BO（2026-09-23）。
 *
 * <p>对应底座 {@code sys_notice} 写入/更新字段。本 BO 独立于
 * {@code org.dromara.system.domain.bo.SysNoticeBo}（在 ruoyi-system 模块），
 * 由 de/ 镜像直接 INSERT/UPDATE sys_notice。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeNcNoticeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 公告 ID（更新时必填，新增时由数据库自增生成） */
    private Long noticeId;

    /** 公告标题（必填，≤50 字符；DDL notice_title varchar(50) not null） */
    @NotBlank
    @Size(max = 50)
    private String noticeTitle;

    /** 公告类型（1通知 2公告；DDL notice_type char(1) not null） */
    @NotBlank
    @Size(max = 1)
    private String noticeType;

    /** 公告内容（DDL notice_content longblob；应用层以字符串读写） */
    private String noticeContent;

    /** 公告状态（0正常 1关闭） */
    private String status;

    /** 备注 */
    private String remark;
}