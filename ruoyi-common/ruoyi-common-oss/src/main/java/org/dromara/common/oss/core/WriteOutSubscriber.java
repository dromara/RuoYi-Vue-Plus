package org.dromara.common.oss.core;

import java.io.IOException;

/**
 * 写出订阅器
 *
 * @author 秋辞未寒
 */
@FunctionalInterface
public interface WriteOutSubscriber<T> {

    /**
     * 将订阅到的数据写出到目标对象。
     *
     * @param out 写出目标
     * @throws IOException 写出异常
     */
    void writeTo(T out) throws IOException;

}
