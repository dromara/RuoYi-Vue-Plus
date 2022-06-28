package com.ruoyi.common.helper;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限助手
 *
 * @author Lion Li
 * @version 3.5.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked cast")
public class DataPermissionHelper {

    private static final String DATA_PERMISSION_KEY = "data:permission";

    public static <T> T getVariable(String key) {
        Map<String, Object> context = getContext();
        return (T) context.get(key);
    }


    public static void setVariable(String key, Object value) {
        Map<String, Object> context = getContext();
        context.put(key, value);
    }

    public static Map<String, Object> getContext() {
        SaStorage saStorage = SaHolder.getStorage();
        Object attribute = saStorage.get(DATA_PERMISSION_KEY);
        if (ObjectUtil.isNull(attribute)) {
            saStorage.set(DATA_PERMISSION_KEY, new HashMap<>());
            attribute = saStorage.get(DATA_PERMISSION_KEY);
        }
        if (attribute instanceof Map) {
            return (Map<String, Object>) attribute;
        }
        throw new NullPointerException("data permission context type exception");
    }
}
