package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * {@code sys_notice} 镜像实体（NC-04 通知中心 CRUD · 2026-09-23）。
 *
 * <p><b>底座表</b>：{@code sys_notice} —— DDL
 * {@code 04-backend-code/script/sql/ry_vue.sql:688-701}
 * （{@code notice_id bigint(20) not null} / {@code notice_title varchar(50) not null} /
 * {@code notice_type char(1) not null} / {@code notice_content longblob} /
 * {@code status char(1) default '0'}，主键 {@code (notice_id)}）。
 *
 * <p><b>为什么本模块要自带一份镜像</b>（同 CX-10 处置手法）：
 * <ol>
 *   <li>ruoyi-workflow 只依赖 ruoyi-api（接口 + DTO），拿不到 ruoyi-system 的
 *       {@code org.dromara.system.domain.SysNotice}。</li>
 *   <li>底座 {@code ISysNoticeService} 接口方法（{@code insertNotice / updateNotice /
 *       deleteNoticeByIds / selectPageNoticeList / selectNoticeById}）需要
 *       {@code SysNoticeBo / SysNoticeVo} —— 同上不可达。</li>
 *   <li>本模块的镜像独立运行，不触碰底座 service；与底座 service 形成「应用层镜像
 *       + 底座原表」双轨，镜像写后底座原生读端点同步可见（同一库同一表）。</li>
 * </ol>
 *
 * <p><b>写入路径独立</b>：NC-04 不写底座 service，镜像直接 INSERT/UPDATE/DELETE
 * sys_notice；与底座 controller 的 service 形成两条写入路径，
 * 但走同一张表 ⇒ 数据天然一致（不依赖底座逻辑）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sys_notice")
public class DeSysNotice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sys_notice.notice_id（主键） */
    @TableId(value = "notice_id", type = IdType.ASSIGN_ID)
    private Long noticeId;

    /** sys_notice.notice_title */
    private String noticeTitle;

    /** sys_notice.notice_type（1通知 2公告） */
    private String noticeType;

    /** sys_notice.notice_content（longblob，应用层读写为字符串） */
    private String noticeContent;

    /** sys_notice.status（0正常 1关闭） */
    private String status;

    /** sys_notice.create_dept */
    private Long createDept;

    /** sys_notice.create_by */
    private Long createBy;

    /** sys_notice.create_time */
    private Date createTime;

    /** sys_notice.update_by */
    private Long updateBy;

    /** sys_notice.update_time */
    private Date updateTime;

    /** sys_notice.remark */
    private String remark;
}