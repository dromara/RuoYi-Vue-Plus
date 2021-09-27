package com.ruoyi.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池 拒绝策略 泛型
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum ThreadPoolRejectedPolicy {

    CALLER_RUNS_POLICY("调用方执行", ThreadPoolExecutor.CallerRunsPolicy.class),
    DISCARD_OLDEST_POLICY("放弃最旧的", ThreadPoolExecutor.DiscardOldestPolicy.class),
    DISCARD_POLICY("丢弃", ThreadPoolExecutor.DiscardPolicy.class),
    ABORT_POLICY("中止", ThreadPoolExecutor.AbortPolicy.class);

    private final String name;
    private final Class<? extends RejectedExecutionHandler> clazz;

}
