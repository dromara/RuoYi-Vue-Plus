package com.ruoyi.common.translation.core.impl;

import com.ruoyi.common.core.service.DictService;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.translation.annotation.TranslationType;
import com.ruoyi.common.translation.constant.TransConstant;
import com.ruoyi.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 字典翻译实现
 *
 * @author Lion Li
 */
@Component
@AllArgsConstructor
@TranslationType(type = TransConstant.DICT_TYPE_TO_LABEL)
public class DictTypeTranslationImpl implements TranslationInterface {

    private final DictService dictService;

    public String translation(Object key, String other) {
        if (key instanceof String dictValue && StringUtils.isNotBlank(other)) {
            return dictService.getDictLabel(other, dictValue);
        }
        return null;
    }
}
