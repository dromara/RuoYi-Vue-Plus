package com.ruoyi.common.translation.core.impl;

import com.ruoyi.common.core.service.UserService;
import com.ruoyi.common.translation.annotation.TranslationType;
import com.ruoyi.common.translation.constant.TransConstant;
import com.ruoyi.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@Component
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            return userService.selectUserNameById(id);
        }
        return null;
    }
}
