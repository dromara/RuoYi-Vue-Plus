package org.dromara.workflow.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 任务状态枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum FormTypeEnum {
    /**
     * 自定义表单
     */
    STATIC("static", "自定义表单"),
    /**
     * 动态表单
     */
    DYNAMIC("dynamic", "动态表单");

    /**
     * 类型
     */
    private final String type;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 表单类型
     *
     * @param formType 表单类型
     */
    public static String findByType(String formType) {
        if (StringUtils.isBlank(formType)) {
            return StrUtil.EMPTY;
        }

        return Arrays.stream(FormTypeEnum.values())
            .filter(statusEnum -> statusEnum.getType().equals(formType))
            .findFirst()
            .map(FormTypeEnum::getDesc)
            .orElse(StrUtil.EMPTY);
    }
}

