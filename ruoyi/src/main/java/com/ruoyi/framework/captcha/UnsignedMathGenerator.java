package com.ruoyi.framework.captcha;

import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.math.Calculator;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 无符号计算生成器
 *
 * @author Lion Li
 */
public class UnsignedMathGenerator implements CodeGenerator {

	private static final long serialVersionUID = -5514819971774091076L;

	private static final String operators = "+-*";

	/**
	 * 参与计算数字最大长度
	 */
	private final int numberLength;

	/**
	 * 构造
	 */
	public UnsignedMathGenerator() {
		this(2);
	}

	/**
	 * 构造
	 *
	 * @param numberLength 参与计算最大数字位数
	 */
	public UnsignedMathGenerator(int numberLength) {
		this.numberLength = numberLength;
	}

	@Override
	public String generate() {
		final int limit = getLimit();
		int min = RandomUtil.randomInt(limit);
		int max = RandomUtil.randomInt(min, limit);
		String number1 = Integer.toString(max);
		String number2 = Integer.toString(min);
		number1 = StrUtil.padAfter(number1, this.numberLength, CharUtil.SPACE);
		number2 = StrUtil.padAfter(number2, this.numberLength, CharUtil.SPACE);

		return number1 + RandomUtil.randomChar(operators) + number2 + '=';
	}

	@Override
	public boolean verify(String code, String userInputCode) {
		int result;
		try {
			result = Integer.parseInt(userInputCode);
		} catch (NumberFormatException e) {
			// 用户输入非数字
			return false;
		}

		final int calculateResult = (int) Calculator.conversion(code);
		return result == calculateResult;
	}

	/**
	 * 获取验证码长度
	 *
	 * @return 验证码长度
	 */
	public int getLength() {
		return this.numberLength * 2 + 2;
	}

	/**
	 * 根据长度获取参与计算数字最大值
	 *
	 * @return 最大值
	 */
	private int getLimit() {
		return Integer.parseInt("1" + StrUtil.repeat('0', this.numberLength));
	}
}
