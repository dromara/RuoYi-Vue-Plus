package com.ruoyi.system.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前在线会话
 *
 * @author Lion Li
 */

@Data
@Schema(name = "当前在线会话业务对象")
public class SysUserOnline {

    /**
     * 会话编号
     */
    @Schema(name = "会话编号")
    private String tokenId;

    /**
     * 部门名称
     */
    @Schema(name = "部门名称")
    private String deptName;

    /**
     * 用户名称
     */
    @Schema(name = "用户名称")
    private String userName;

    /**
     * 登录IP地址
     */
    @Schema(name = "登录IP地址")
    private String ipaddr;

    /**
     * 登录地址
     */
    @Schema(name = "登录地址")
    private String loginLocation;

    /**
     * 浏览器类型
     */
    @Schema(name = "浏览器类型")
    private String browser;

    /**
     * 操作系统
     */
    @Schema(name = "操作系统")
    private String os;

    /**
     * 登录时间
     */
    @Schema(name = "登录时间")
    private Long loginTime;

}
