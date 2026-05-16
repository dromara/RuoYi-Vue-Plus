package org.dromara.common.mybatis.helper;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.domain.DataPermissionAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 数据权限助手
 *
 * @author Lion Li
 * @version 3.5.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
public class DataPermissionHelper {

    private static final String DATA_PERMISSION_KEY = "data:permission";
    private static final String ACCESS_KEY = "data:permission:access";

    private static final ThreadLocal<DataPermission> PERMISSION_CACHE = new ThreadLocal<>();

    /**
     * 获取当前执行mapper权限注解
     *
     * @return 返回当前执行mapper权限注解
     */
    public static DataPermission getPermission() {
        return PERMISSION_CACHE.get();
    }

    /**
     * 设置当前执行mapper权限注解
     *
     * @param dataPermission 数据权限注解
     */
    public static void setPermission(DataPermission dataPermission) {
        PERMISSION_CACHE.set(dataPermission);
    }

    /**
     * 删除当前执行mapper权限注解
     */
    public static void removePermission() {
        PERMISSION_CACHE.remove();
    }

    /**
     * 从上下文中获取指定键的变量值，并将其转换为指定的类型
     *
     * @param key 变量的键
     * @param <T> 变量值的类型
     * @return 指定键的变量值，如果不存在则返回 null
     */
    public static <T> T getVariable(String key) {
        Map<String, Object> context = getContext();
        return (T) context.get(key);
    }

    /**
     * 向上下文中设置指定键的变量值
     *
     * @param key   要设置的变量的键
     * @param value 要设置的变量值
     */
    public static void setVariable(String key, Object value) {
        Map<String, Object> context = getContext();
        context.put(key, value);
    }

    /**
     * 获取当前数据权限访问控制对象。
     *
     * @return 访问控制对象
     */
    public static DataPermissionAccess getAccess() {
        return getVariable(ACCESS_KEY);
    }

    /**
     * 设置当前数据权限访问控制对象。
     *
     * @param access 访问控制对象
     */
    public static void setAccess(DataPermissionAccess access) {
        setVariable(ACCESS_KEY, access);
    }

    /**
     * 获取数据权限上下文
     *
     * @return 存储在SaStorage中的Map对象，用于存储数据权限相关的上下文信息
     * @throws NullPointerException 如果数据权限上下文类型异常，则抛出NullPointerException
     */
    public static Map<String, Object> getContext() {
        SaStorage saStorage = SaHolder.getStorage();
        Object attribute = saStorage.get(DATA_PERMISSION_KEY);
        if (ObjectUtil.isNull(attribute)) {
            saStorage.set(DATA_PERMISSION_KEY, new HashMap<>());
            attribute = saStorage.get(DATA_PERMISSION_KEY);
        }
        if (attribute instanceof Map map) {
            return map;
        }
        throw new IllegalStateException("data permission context type exception");
    }

    /**
     * 在忽略数据权限中执行
     *
     * @param handle 处理执行方法
     */
    public static void ignore(Runnable handle) {
        DataPermissionIgnoreContext.enable();
        try {
            handle.run();
        } finally {
            DataPermissionIgnoreContext.disable();
        }
    }

    /**
     * 在忽略数据权限中执行
     *
     * @param handle 处理执行方法
     * @return 执行结果
     */
    public static <T> T ignore(Supplier<T> handle) {
        DataPermissionIgnoreContext.enable();
        try {
            return handle.get();
        } finally {
            DataPermissionIgnoreContext.disable();
        }
    }

}
