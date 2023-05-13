package com.ruoyi.common.excel;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>Excel下拉可选项</h1>
 * 注意：为确保下拉框解析正确，传值务必使用createOptionValue()做为值的拼接
 *
 * @author Emil.Zhang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class DropDownOptions {
    /**
     * 一级下拉所在列index，从0开始算
     */
    private int index = 0;
    /**
     * 二级下拉所在的index，从0开始算，不能与一级相同
     */
    private int nextIndex = 0;
    /**
     * 一级下拉所包含的数据
     */
    private List<String> options = new ArrayList<>();
    /**
     * 二级下拉所包含的数据Map
     * <p>以每一个一级选项值为Key，每个一级选项对应的二级数据为Value</p>
     */
    private Map<String, List<String>> nextOptions = new HashMap<>();
    /**
     * 分隔符
     */
    private static final String DELIMITER = "_";

    /**
     * 创建只有一级的下拉选
     */
    public DropDownOptions(int index, List<String> options) {
        this.index = index;
        this.options = options;
    }

    /**
     * <h2>创建每个选项可选值</h2>
     * <p>注意：不能以数字，特殊符号开头，选项中不可以包含任何运算符号</p>
     *
     * @param vars 可选值内包含的参数
     * @return 合规的可选值
     */
    public static String createOptionValue(Object... vars) {
        StringBuilder stringBuffer = new StringBuilder();
        String regex = "^[\\S\\d\\u4e00-\\u9fa5]+$";
        for (int i = 0; i < vars.length; i++) {
            String var = StrUtil.trimToEmpty(String.valueOf(vars[i]));
            if (!var.matches(regex)) {
                throw new ServiceException("选项数据不符合规则，仅允许使用中英文字符以及数字");
            }
            stringBuffer.append(var);
            if (i < vars.length - 1) {
                // 直至最后一个前，都以_作为切割线
                stringBuffer.append(DELIMITER);
            }
        }
        if (stringBuffer.toString().matches("^\\d_*$")) {
            throw new ServiceException("禁止以数字开头");
        }
        return stringBuffer.toString();
    }

    /**
     * 将处理后合理的可选值解析为原始的参数
     *
     * @param option 经过处理后的合理的可选项
     * @return 原始的参数
     */
    public static List<String> analyzeOptionValue(String option) {
        return StrUtil.split(option, DELIMITER, true, true);
    }
}
