package com.ruoyi.system.domain.bo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 云存储配置业务对象 sys_oss_config
 *
 * @author ruoyi
 * @date 2021-08-11
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("云存储配置业务对象")
public class SysOssConfigBo extends BaseEntity {

	/**
	 * 主建
	 */
	@ApiModelProperty("主建")
	private Integer ossConfigId;
    /**
     * 配置key
     */
    @ApiModelProperty(value = "configKey", required = true)
    @NotBlank(message = "configKey不能为空", groups = { AddGroup.class, EditGroup.class })
	@Size(min = 2, max = 100, message = "configKey长度必须介于2和20 之间")
    private String configKey;

    /**
     * accessKey
     */
    @ApiModelProperty(value = "accessKey")
	@NotBlank(message = "accessKey不能为空", groups = { AddGroup.class, EditGroup.class })
	@Size(min = 2, max = 100, message = "accessKey长度必须介于2和100 之间")
    private String accessKey;

    /**
     * 秘钥
     */
    @ApiModelProperty(value = "secretKey")
	@NotBlank(message = "secretKey不能为空", groups = { AddGroup.class, EditGroup.class })
	@Size(min = 2, max = 100, message = "secretKey长度必须介于2和100 之间")
    private String secretKey;

    /**
     * 桶名称
     */
    @ApiModelProperty(value = "bucketName")
	@NotBlank(message = "bucketName不能为空", groups = { AddGroup.class, EditGroup.class })
	@Size(min = 2, max = 100, message = "bucketName长度必须介于2和100之间")
    private String bucketName;

    /**
     * 前缀
     */
    @ApiModelProperty(value = "前缀")
    private String prefix;

    /**
     * 访问站点
     */
    @ApiModelProperty(value = "endpoint")
	@NotBlank(message = "endpoint不能为空", groups = { AddGroup.class, EditGroup.class })
	@Size(min = 2, max = 100, message = "endpoint长度必须介于2和100之间")
    private String endpoint;

    /**
     * 是否htpps（0否 1是）
     */
    @ApiModelProperty(value = "是否htpps（0否 1是）")
    private String isHttps;

    /**
     * 域
     */
    @ApiModelProperty(value = "region")
    private String region;

    /**
     * 扩展字段
     */
    @ApiModelProperty(value = "扩展字段")
    private String ext1;


    /**
     * 分页大小
     */
    @ApiModelProperty("分页大小")
    private Integer pageSize;

    /**
     * 当前页数
     */
    @ApiModelProperty("当前页数")
    private Integer pageNum;

    /**
     * 排序列
     */
    @ApiModelProperty("排序列")
    private String orderByColumn;

    /**
     * 排序的方向desc或者asc
     */
    @ApiModelProperty(value = "排序的方向", example = "asc,desc")
    private String isAsc;
	/**
	 * 状态(0正常 1停用)
	 */
	@ApiModelProperty("状态(0正常 1停用)")
	private String status;
}
