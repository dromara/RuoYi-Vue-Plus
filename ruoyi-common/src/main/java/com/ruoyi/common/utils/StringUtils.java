package com.ruoyi.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * 字符串工具类
 *
 * @author Lion Li
 */
public class StringUtils extends StrUtil {

	/**
	 * * 判断一个对象是否为空
	 *
	 * @param object Object
	 * @return true：为空 false：非空
	 */
	public static boolean isNull(Object object) {
		return Validator.isNull(object);
	}

	/**
	 * * 判断一个对象是否非空
	 *
	 * @param object Object
	 * @return true：非空 false：空
	 */
	public static boolean isNotNull(Object object) {
		return Validator.isNotNull(object);
	}

	/**
	 * 替换所有
	 */
	public static String replaceEach(String text, String[] searchList, String[] replacementList) {
		return org.apache.commons.lang3.StringUtils.replaceEach(text, searchList, replacementList);
	}

	/**
	 * 验证该字符串是否是数字
	 *
	 * @param value 字符串内容
	 * @return 是否是数字
	 */
	public static boolean isNumeric(CharSequence value) {
		return Validator.isNumber(value);
	}

	/**
	 * 是否为http(s)://开头
	 *
	 * @param link 链接
	 * @return 结果
	 */
	public static boolean ishttp(String link) {
		return Validator.isUrl(link);
	}

	/**
	 * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
	 *
	 * @param str  指定字符串
	 * @param strs 需要检查的字符串数组
	 * @return 是否匹配
	 */
	public static boolean matches(String str, List<String> strs) {
		if (isEmpty(str) || CollUtil.isEmpty(strs)) {
			return false;
		}
		for (String pattern : strs) {
			if (ReUtil.isMatch(pattern, str)) {
				return true;
			}
		}
		return false;
	}
}
