package org.dromara.common.oss.config;

/**
 * 配置对象接口
 *
 * @param <T> 配置类型
 * @param <B> 配置构建器类型
 *
 * @author 秋辞未寒
 */
public interface Config<T,B> {

    /**
     * 配置对象拷贝
     * @return 拷贝后的新配置对象
     */
    T copy();

    /**
     * 转为构建器对象
     * @return 构建器对象
     */
    B toBuilder();

}
