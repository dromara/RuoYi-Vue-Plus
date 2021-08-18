package com.ruoyi.common.core.domain.entity;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 字典类型表 sys_dict_type
 *
 * @author ruoyi
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_dict_type")
@ExcelIgnoreUnannotated
public class SysDictType implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 字典主键
	 */
	@ExcelProperty(value = "字典主键")
	@TableId(value = "dict_id", type = IdType.AUTO)
	private Long dictId;

	/**
	 * 字典名称
	 */
	@ExcelProperty(value = "字典名称")
	@NotBlank(message = "字典名称不能为空")
	@Size(min = 0, max = 100, message = "字典类型名称长度不能超过100个字符")
	private String dictName;

	/**
	 * 字典类型
	 */
	@ExcelProperty(value = "字典类型")
	@NotBlank(message = "字典类型不能为空")
	@Size(min = 0, max = 100, message = "字典类型类型长度不能超过100个字符")
	private String dictType;

	/**
	 * 状态（0正常 1停用）
	 */
	@ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
	@ExcelDictFormat(dictType = "sys_common_status")
	private String status;

	/**
	 * 创建者
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	private Date createTime;

	/**
	 * 更新者
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

	/**
	 * 更新时间
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Date updateTime;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 请求参数
	 */
	@TableField(exist = false)
	private Map<String, Object> params = new HashMap<>();

}
