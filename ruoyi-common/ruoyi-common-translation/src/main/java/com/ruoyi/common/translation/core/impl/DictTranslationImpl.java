package com.ruoyi.common.translation.core.impl;

import com.ruoyi.common.translation.annotation.TranslationType;
import com.ruoyi.common.translation.constant.TransConstant;
import com.ruoyi.common.translation.core.TranslationInterface;
import org.springframework.stereotype.Component;

/**
 * 翻译接口 (实现类需标注 {@link com.ruoyi.common.translation.annotation.TranslationType} 注解标明翻译类型)
 *
 * @author Lion Li
 */
@Component
@TranslationType(type = TransConstant.DICT_TYPE_TO_LABEL)
public class DictTranslationImpl implements TranslationInterface {

    /**
     * 翻译
     *
     * @param key 需要被翻译的键
     * @return 返回键对应的值
     */
    public String translation(Object key) {
        if (key instanceof String dictType) {
            return "dict";
        }
        return null;
    }
}
