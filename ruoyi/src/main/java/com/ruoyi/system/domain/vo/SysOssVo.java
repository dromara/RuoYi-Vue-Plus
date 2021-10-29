package com.ruoyi.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * OSS对象存储视图对象 sys_oss
 *
 * @author Lion Li
 */
@Data
@ApiModel("OSS对象存储视图对象")
public class SysOssVo {

    private static final long serialVersionUID = 1L;

    /**
     * 对象存储主键
     */
    @ApiModelProperty("对象存储主键")
    private Long ossId;

    /**
     * 文件名
     */
    @ApiModelProperty("文件名")
    private String fileName;

    /**
     * 原名
     */
    @ApiModelProperty("原名")
    private String originalName;

    /**
     * 文件后缀名
     */
    @ApiModelProperty("文件后缀名")
    private String fileSuffix;

    /**
     * URL地址
     */
    @ApiModelProperty("URL地址")
    private String url;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createTime;

    /**
     * 上传人
     */
    @ApiModelProperty("上传人")
    private String createBy;

    /**
     * 服务商
     */
    @ApiModelProperty("服务商")
    private String service;


}
