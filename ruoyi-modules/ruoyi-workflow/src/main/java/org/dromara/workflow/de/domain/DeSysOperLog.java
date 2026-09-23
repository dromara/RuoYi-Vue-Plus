package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * {@code sys_oper_log} 镜像实体（SY-03 系统操作日志 · 2026-09-23）。
 *
 * <p><b>底座表</b>：{@code sys_oper_log} —— DDL
 * {@code 04-backend-code/script/sql/ry_vue.sql:520-549}
 * （{@code oper_id bigint(20) not null} / {@code title varchar(50)} /
 * {@code business_type int(2) default 0} / {@code oper_name varchar(50)} /
 * {@code oper_url varchar(255)} / {@code status int(1) default 0} /
 * {@code oper_time datetime}，主键 {@code (oper_id)}，索引
 * {@code idx_sys_oper_log_bt/uid/s/ot}）。
 *
 * <p><b>只读镜像</b>：本类只用于查询 / 列表 / 导出 / 清空，**不写** sys_oper_log
 * （操作日志由底座 controller 自动写入）。本镜像与底座 controller 共用同表，
 * SELECT 结果天然一致。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sys_oper_log")
public class DeSysOperLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sys_oper_log.oper_id（主键） */
    @TableId(value = "oper_id", type = IdType.ASSIGN_ID)
    private Long operId;

    /** sys_oper_log.title */
    private String title;

    /** sys_oper_log.business_type */
    private Integer businessType;

    /** sys_oper_log.method */
    private String method;

    /** sys_oper_log.request_method */
    private String requestMethod;

    /** sys_oper_log.operator_type */
    private Integer operatorType;

    /** sys_oper_log.oper_name */
    private String operName;

    /** sys_oper_log.user_id */
    private Long userId;

    /** sys_oper_log.dept_id */
    private Long deptId;

    /** sys_oper_log.dept_name */
    private String deptName;

    /** sys_oper_log.client_key */
    private String clientKey;

    /** sys_oper_log.device_type */
    private String deviceType;

    /** sys_oper_log.browser */
    private String browser;

    /** sys_oper_log.os */
    private String os;

    /** sys_oper_log.oper_url */
    private String operUrl;

    /** sys_oper_log.oper_ip */
    private String operIp;

    /** sys_oper_log.oper_location */
    private String operLocation;

    /** sys_oper_log.oper_param */
    private String operParam;

    /** sys_oper_log.json_result */
    private String jsonResult;

    /** sys_oper_log.status */
    private Integer status;

    /** sys_oper_log.error_msg */
    private String errorMsg;

    /** sys_oper_log.oper_time */
    private Date operTime;

    /** sys_oper_log.cost_time */
    private Long costTime;
}