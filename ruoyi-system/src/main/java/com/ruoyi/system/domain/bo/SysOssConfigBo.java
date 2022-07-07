package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 对象存储配置业务对象 sys_oss_config
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "对象存储配置业务对象")
public class SysOssConfigBo extends BaseEntity {

    /**
     * 主建
     */
    @Schema(name = "主建", required = true)
    @NotNull(message = "主建不能为空", groups = {EditGroup.class})
    private Long ossConfigId;

    /**
     * 配置key
     */
    @Schema(name = "配置key", required = true)
    @NotBlank(message = "配置key不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "configKey长度必须介于2和20 之间")
    private String configKey;

    /**
     * accessKey
     */
    @Schema(name = "accessKey", required = true)
    @NotBlank(message = "accessKey不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "accessKey长度必须介于2和100 之间")
    private String accessKey;

    /**
     * 秘钥
     */
    @Schema(name = "secretKey", required = true)
    @NotBlank(message = "secretKey不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "secretKey长度必须介于2和100 之间")
    private String secretKey;

    /**
     * 桶名称
     */
    @Schema(name = "桶名称", required = true)
    @NotBlank(message = "桶名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "bucketName长度必须介于2和100之间")
    private String bucketName;

    /**
     * 前缀
     */
    @Schema(name = "前缀")
    private String prefix;

    /**
     * 访问站点
     */
    @Schema(name = "访问站点", required = true)
    @NotBlank(message = "访问站点不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 100, message = "endpoint长度必须介于2和100之间")
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
     * 状态（0=正常,1=停用）
     */
    @Schema(name = "状态（0=正常,1=停用）")
    private String status;

    /**
     * 域
     */
    @Schema(name = "域")
    private String region;

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
