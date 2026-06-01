package org.dromara.common.core.xss;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HtmlUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dromara.common.core.utils.StringUtils;

/**
 * 自定义xss校验注解实现
 *
 * @author Lion Li
 */
public class XssValidator implements ConstraintValidator<Xss, String> {

    /**
     * 校验字符串是否包含 HTML 标签。
     *
     * @param value                      待校验值
     * @param constraintValidatorContext 校验上下文
     * @return true 校验通过 false 包含 HTML 标签
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return !ReUtil.contains(HtmlUtil.RE_HTML_MARK, value);
    }

}
