package com.ruoyi.common.translation.core.impl;

import com.ruoyi.common.translation.annotation.TranslationType;
import com.ruoyi.common.translation.constant.TransConstant;
import com.ruoyi.common.translation.core.TranslationInterface;
import org.springframework.stereotype.Component;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@Component
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface {

    public String translation(Object key, String other) {
        // todo 待实现
        if (key instanceof Long id) {
            return "admin";
        }
        return null;
    }
}
