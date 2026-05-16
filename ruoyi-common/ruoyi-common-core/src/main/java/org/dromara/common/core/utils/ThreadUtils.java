package org.dromara.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 线程工具
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadUtils {

    /**
     * 批量执行任务
     */
    public static void virtualInvokeAll(Runnable... runnableList) {
        List<Future<?>> callableList = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Runnable runnable : runnableList) {
                callableList.add(executor.submit(runnable));
            }
            for (Future<?> future : callableList) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程执行被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("线程执行异常：" + cause.getMessage(), cause);
        }

    }

}
