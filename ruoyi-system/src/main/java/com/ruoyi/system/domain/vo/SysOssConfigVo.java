package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 对象存储配置视图对象 sys_oss_config
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Data
@ApiModel("对象存储配置视图对象")
@ExcelIgnoreUnannotated
public class SysOssConfigVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主建
     */
    @ApiModelProperty("主建")
    private Long ossConfigId;

    /**
     * 配置key
     */
    @ApiModelProperty("配置key")
    private String configKey;

    /**
     * accessKey
     */
    @ApiModelProperty("accessKey")
    private String accessKey;

    /**
     * 秘钥
     */
    @ApiModelProperty("secretKey")
    private String secretKey;

    /**
     * 桶名称
     */
    @ApiModelProperty("桶名称")
    private String bucketName;

    /**
     * 前缀
     */
    @ApiModelProperty("前缀")
    private String prefix;

    /**
     * 访问站点
     */
    @ApiModelProperty("访问站点")
    private String endpoint;

    /**
     * 自定义域名
     */
    @ApiModelProperty("自定义域名")
    private String domain;

    /**
     * 是否https（Y=是,N=否）
     */
    @ApiModelProperty("是否https（Y=是,N=否）")
    private String isHttps;

    /**
     * 域
     */
    @ApiModelProperty("域")
    private String region;

    /**
     * 状态（0=正常,1=停用）
     */
    @ApiModelProperty("状态（0=正常,1=停用）")
    private String status;

    /**
     * 扩展字段
     */
    @ApiModelProperty("扩展字段")
    private String ext1;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;

}
