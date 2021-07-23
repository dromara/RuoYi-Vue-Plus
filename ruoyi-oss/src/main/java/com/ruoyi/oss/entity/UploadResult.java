package com.ruoyi.oss.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 上传返回体
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class UploadResult {

	/**
	 * 文件路径
	 */
	private String url;

	/**
	 * 文件名
	 */
	private String filename;
}
