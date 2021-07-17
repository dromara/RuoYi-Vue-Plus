package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件上传表 sys_oss
 *
 * @author chkj
 * @date 2019-07-15
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@TableName("sys_oss")
public class SysOss implements Serializable {
	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 文件后缀名
	 */
	private String fileSuffix;

	/**
	 * URL地址
	 */
	private String url;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	private Date createTime;

	/**
	 * 上传人
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 服务商
	 */
	private Integer service;
}
