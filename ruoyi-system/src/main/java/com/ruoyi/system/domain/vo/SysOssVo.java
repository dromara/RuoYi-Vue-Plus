package com.ruoyi.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * OSS对象存储视图对象 sys_oss
 *
 * @author Lion Li
 */
@Data
@Schema(name = "OSS对象存储视图对象")
public class SysOssVo {

    private static final long serialVersionUID = 1L;

    /**
     * 对象存储主键
     */
    @Schema(name = "对象存储主键")
    private Long ossId;

    /**
     * 文件名
     */
    @Schema(name = "文件名")
    private String fileName;

    /**
     * 原名
     */
    @Schema(name = "原名")
    private String originalName;

    /**
     * 文件后缀名
     */
    @Schema(name = "文件后缀名")
    private String fileSuffix;

    /**
     * URL地址
     */
    @Schema(name = "URL地址")
    private String url;

    /**
     * 创建时间
     */
    @Schema(name = "创建时间")
    private Date createTime;

    /**
     * 上传人
     */
    @Schema(name = "上传人")
    private String createBy;

    /**
     * 服务商
     */
    @Schema(name = "服务商")
    private String service;


}
