package org.dromara.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import org.dromara.common.core.service.UserService;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    /**
     * 将用户 ID 翻译为用户名。
     *
     * @param key 用户 ID
     * @param other 额外参数
     * @return 用户名
     */
    @Override
    public String translation(Object key, String other) {
        return userService.selectUserNameById(Convert.toLong(key));
    }
}
