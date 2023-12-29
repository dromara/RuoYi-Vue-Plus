package org.dromara.common.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程持有类
 *
 * @author Michelle.Chung
 */
public class ThreadLocalHolder {

    /**
     * 初始化 (支持异步)
     */
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = TransmittableThreadLocal.withInitial(HashMap::new);

    /**
     * 设置值
     *
     * @param key 键
     * @param value 值
     */
    public static <T> void set(String key, T value) {
        THREAD_LOCAL.get().put(key, value);
    }

    /**
     * 获取值
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) THREAD_LOCAL.get().get(key);
    }

    /**
     * 移除值
     *
     * @param key 键
     */
    public static void remove(String key) {
        THREAD_LOCAL.get().remove(key);
    }

    /**
     * 清空值
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }

}
