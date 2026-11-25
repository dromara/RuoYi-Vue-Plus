package org.dromara.system.api;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import org.dromara.common.core.utils.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通用 参数配置服务
 *
 * @author Lion Li
 */
public interface ConfigService {

    /**
     * 根据参数 key 获取参数值
     *
     * @param configKey 参数 key
     * @return 参数值
     */
    String getConfigValue(String configKey);

    /**
     * 根据参数 key 获取布尔值
     *
     * @param configKey 参数 key
     * @return Boolean 值
     */
    default Boolean getConfigBool(String configKey) {
        return Convert.toBool(getConfigValue(configKey));
    }

    /**
     * 根据参数 key 获取整数值
     *
     * @param configKey 参数 key
     * @return Integer 值
     */
    default Integer getConfigInt(String configKey) {
        return Convert.toInt(getConfigValue(configKey));
    }

    /**
     * 根据参数 key 获取长整型值
     *
     * @param configKey 参数 key
     * @return Long 值
     */
    default Long getConfigLong(String configKey) {
        return Convert.toLong(getConfigValue(configKey));
    }

    /**
     * 根据参数 key 获取 BigDecimal 值
     *
     * @param configKey 参数 key
     * @return BigDecimal 值
     */
    default BigDecimal getConfigDecimal(String configKey) {
        return Convert.toBigDecimal(getConfigValue(configKey));
    }

    /**
     * 根据参数 key 获取 Map 类型的配置
     *
     * @param configKey 参数 key
     * @return Dict 对象；配置为 null、空字符串或纯空白时返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    Dict getConfigMap(String configKey);

    /**
     * 根据参数 key 获取 Map 类型的配置列表
     *
     * @param configKey 参数 key
     * @return Dict 列表；配置为 null、空字符串或纯空白时返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    List<Dict> getConfigArrayMap(String configKey);

    /**
     * 根据参数 key 获取指定类型的配置对象
     *
     * @param configKey 参数 key
     * @param clazz     目标对象类型
     * @param <T>       目标对象泛型
     * @return 对象实例；配置为 null 或空字符串时返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    <T> T getConfigObject(String configKey, Class<T> clazz);

    /**
     * 根据参数 key 获取指定类型的配置列表
     *
     * @param configKey 参数 key
     * @param clazz     目标元素类型
     * @param <T>       元素类型泛型
     * @return 指定类型列表；配置为 null 或空字符串时返回空列表，JSON null 返回 null
     * @throws RuntimeException 配置不是有效 JSON 或无法转换为目标类型
     */
    <T> List<T> getConfigArray(String configKey, Class<T> clazz);

    /**
     * 根据参数 key 获取参数值，缺失或空白时使用默认值
     *
     * @param configKey    参数 key
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    default String getConfigValue(String configKey, String defaultValue) {
        return StringUtils.defaultIfBlank(getConfigValue(configKey), defaultValue);
    }

    /**
     * 根据参数 key 获取布尔值，缺失或空白时使用默认值
     * 非空值沿用 Hutool Convert 规则：true、yes、1 等为 true，其他字符串为 false
     *
     * @param configKey    参数 key
     * @param defaultValue 默认值
     * @return 转换后的配置值或默认值
     */
    default Boolean getConfigBool(String configKey, Boolean defaultValue) {
        return Convert.toBool(getConfigValue(configKey, null), defaultValue);
    }

    /**
     * 根据参数 key 获取整数值，缺失或转换失败时使用默认值
     *
     * @param configKey    参数 key
     * @param defaultValue 默认值
     * @return 转换后的配置值或默认值
     */
    default Integer getConfigInt(String configKey, Integer defaultValue) {
        return Convert.toInt(getConfigValue(configKey, null), defaultValue);
    }

    /**
     * 根据参数 key 获取长整型值，缺失或转换失败时使用默认值
     *
     * @param configKey    参数 key
     * @param defaultValue 默认值
     * @return 转换后的配置值或默认值
     */
    default Long getConfigLong(String configKey, Long defaultValue) {
        return Convert.toLong(getConfigValue(configKey, null), defaultValue);
    }

    /**
     * 根据参数 key 获取 BigDecimal 值，缺失或转换失败时使用默认值
     *
     * @param configKey    参数 key
     * @param defaultValue 默认值
     * @return 转换后的配置值或默认值
     */
    default BigDecimal getConfigDecimal(String configKey, BigDecimal defaultValue) {
        return Convert.toBigDecimal(getConfigValue(configKey, null), defaultValue);
    }

}
