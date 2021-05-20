package com.ruoyi.common.core.page;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 表格分页数据对象
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@ApiModel("分页响应对象")
public class TableDataInfo<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 总记录数
	 */
	@ApiModelProperty("总记录数")
	private long total;

	/**
	 * 列表数据
	 */
	@ApiModelProperty("列表数据")
	private List<T> rows;

	/**
	 * 消息状态码
	 */
	@ApiModelProperty("消息状态码")
	private int code;

	/**
	 * 消息内容
	 */
	@ApiModelProperty("消息内容")
	private String msg;

	/**
	 * 分页
	 *
	 * @param list  列表数据
	 * @param total 总记录数
	 */
	public TableDataInfo(List<T> list, long total) {
		this.rows = list;
		this.total = total;
	}

}
