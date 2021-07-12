package com.ruoyi.common.utils;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.constant.Constants;

/**
 * 字符串工具类
 *
 * @author ruoyi
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {
	/** 空字符串 */
	private static final String NULLSTR = "";

	/** 下划线 */
	private static final char SEPARATOR = '_';

	/**
	 * 是否为http(s)://开头
	 *
	 * @param link 链接
	 * @return 结果
	 */
	public static boolean ishttp(String link) {
		return StrUtil.startWithAny(link, Constants.HTTP, Constants.HTTPS);
	}

}
