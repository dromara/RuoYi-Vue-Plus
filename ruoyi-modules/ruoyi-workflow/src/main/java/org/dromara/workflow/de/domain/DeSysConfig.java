package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * {@code sys_config} 镜像实体（SY-02 系统配置 · 2026-09-23）。
 *
 * <p><b>底座表</b>：{@code sys_config} —— DDL
 * {@code 04-backend-code/script/sql/ry_vue.sql:644-657}
 * （{@code config_id bigint(20) not null} / {@code config_name varchar(100)} /
 * {@code config_key varchar(100)} / {@code config_value varchar(500)} /
 * {@code config_type char(1) default 'N'}，主键 {@code (config_id)}）。
 *
 * <p><b>镜像原因</b>：同 {@link SysNotice} 处置手法 —— ruoyi-workflow 只依赖
 * ruoyi-api，拿不到 ruoyi-system 的 {@code SysConfig}。镜像独立 INSERT/UPDATE
 * sys_config，与底座 controller 双轨写入同一张表。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sys_config")
public class DeSysConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sys_config.config_id（主键） */
    @TableId(value = "config_id", type = IdType.ASSIGN_ID)
    private Long configId;

    /** sys_config.config_name */
    private String configName;

    /** sys_config.config_key */
    private String configKey;

    /** sys_config.config_value */
    private String configValue;

    /** sys_config.config_type（Y系统内置 N否） */
    private String configType;

    /** sys_config.create_dept */
    private Long createDept;

    /** sys_config.create_by */
    private Long createBy;

    /** sys_config.create_time */
    private Date createTime;

    /** sys_config.update_by */
    private Long updateBy;

    /** sys_config.update_time */
    private Date updateTime;

    /** sys_config.remark */
    private String remark;
}