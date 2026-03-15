package org.dromara.common.oss.s3.builder;

/**
 * 构建器
 *
 * @param <T> 参数类型
 * @param <R> 构建目标类型
 * @author 秋辞未寒
 */
public interface Builder<T,R> {

    R build(T param);

}
