package org.dromara.common.oss.s3.config;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

/**
 * S3 异步执行器配置
 *
 * @param enabledVirtualThread 是否启用虚拟线程
 * @param corePoolSize         核心线程数
 *                             <p>
 *                             默认为当前CPU核心数，该配置项在配置了虚拟线程后会失效
 * @author 秋辞未寒
 */
@Builder
public record S3AsyncExecutorConfig(
    boolean enabledVirtualThread
    , int corePoolSize
) implements Config<S3AsyncExecutorConfig, S3AsyncExecutorConfig.S3AsyncExecutorConfigBuilder>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认核心线程数 = 当前处理器核心数
     */
    public static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * 默认异步执行器配置
     */
    public static final S3AsyncExecutorConfig DEFAULT = S3AsyncExecutorConfig.builder()
        .enabledVirtualThread(false)
        .corePoolSize(DEFAULT_CORE_POOL_SIZE)
        .build();

    /**
     * 是否启用虚拟线程
     */
    @Override
    public boolean enabledVirtualThread() {
        return enabledVirtualThread;
    }

    /**
     * 核心线程数
     */
    @Override
    public int corePoolSize() {
        return corePoolSize;
    }

    @Override
    public S3AsyncExecutorConfig copy() {
        return toBuilder().build();
    }

    @Override
    public S3AsyncExecutorConfigBuilder toBuilder() {
        return builder()
            .enabledVirtualThread(enabledVirtualThread)
            .corePoolSize(corePoolSize);
    }
}
