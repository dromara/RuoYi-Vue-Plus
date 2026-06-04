package org.dromara.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
     * @param key   用户 ID
     * @param other 额外参数
     * @return 用户名
     */
    @Override
    public String translation(Object key, String other) {
        return userService.selectUserNameById(Convert.toLong(key));
    }

    /**
     * 批量将用户 ID 翻译为用户名。
     *
     * @param keys  用户 ID 集合
     * @param other 额外参数
     * @return 用户 ID 与用户名映射
     */
    @Override
    public Map<Object, String> translationBatch(Set<Object> keys, String other) {
        Set<Long> userIds = collectLongIds(keys);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> userNames = new LinkedHashMap<>(StreamUtils.toMap(userService.selectListByIds(userIds), UserDTO::getUserId, UserDTO::getUserName));
        Map<Object, String> result = new LinkedHashMap<>(keys.size());
        for (Object key : keys) {
            result.put(key, buildValue(key, userNames));
        }
        return result;
    }

    /**
     * 根据原始键构建用户名翻译值。
     *
     * @param source    原始键
     * @param userNames 用户 ID 与用户名映射
     * @return 用户名
     */
    private String buildValue(Object source, Map<Long, String> userNames) {
        if (source instanceof String ids) {
            return joinMappedValues(ids, userNames::get);
        }
        return userNames.get(Convert.toLong(source));
    }
}
