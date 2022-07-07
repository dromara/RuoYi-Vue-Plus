package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 对象存储配置视图对象 sys_oss_config
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Data
@Schema(name = "对象存储配置视图对象")
@ExcelIgnoreUnannotated
public class SysOssConfigVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主建
     */
    @Schema(name = "主建")
    private Long ossConfigId;

    /**
     * 配置key
     */
    @Schema(name = "配置key")
    private String configKey;

    /**
     * accessKey
     */
    @Schema(name = "accessKey")
    private String accessKey;

    /**
     * 秘钥
     */
    @Schema(name = "secretKey")
    private String secretKey;

    /**
     * 桶名称
     */
    @Schema(name = "桶名称")
    private String bucketName;

    /**
     * 前缀
     */
    @Schema(name = "前缀")
    private String prefix;

    /**
     * 访问站点
     */
    @Schema(name = "访问站点")
    private String endpoint;

    /**
     * 自定义域名
     */
    @Schema(name = "自定义域名")
    private String domain;

    /**
     * 是否https（Y=是,N=否）
     */
    @Schema(name = "是否https（Y=是,N=否）")
    private String isHttps;

    /**
     * 域
     */
    @Schema(name = "域")
    private String region;

    /**
     * 状态（0=正常,1=停用）
     */
    @Schema(name = "状态（0=正常,1=停用）")
    private String status;

    /**
     * 扩展字段
     */
    @Schema(name = "扩展字段")
    private String ext1;

    /**
     * 备注
     */
    @Schema(name = "备注")
    private String remark;

}
