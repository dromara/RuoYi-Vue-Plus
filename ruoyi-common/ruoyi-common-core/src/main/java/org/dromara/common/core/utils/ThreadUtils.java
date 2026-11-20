package org.dromara.common.core.utils;

import cn.hutool.core.exceptions.ExceptionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * 线程工具
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadUtils {

    /**
     * 批量执行任务，原样传递任务中的运行时异常，其他任务异常保留根因后包装。
     *
     * @param runnableList 任务列表
     * @throws RuntimeException 等待任务时线程被中断，中断标记会保留
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
            throw ExceptionUtil.wrapRuntime(cause);
        }

    }

    /**
     * 批量执行有返回值的任务，异常处理与集合重载一致。
     *
     * @param supplierList 任务列表
     * @param <T>          返回值类型
     * @return 按提交顺序返回的任务结果
     */
    @SafeVarargs
    public static <T> List<T> virtualSubmitAll(Supplier<T>... supplierList) {
        return virtualSubmitAll(List.of(supplierList));
    }

    /**
     * 批量执行有返回值的任务，原样传递任务中的运行时异常，其他任务异常保留根因后包装。
     *
     * @param supplierList 任务列表
     * @param <T>          返回值类型
     * @return 按提交顺序返回的任务结果
     * @throws RuntimeException 等待任务时线程被中断，中断标记会保留
     */
    public static <T> List<T> virtualSubmitAll(Collection<? extends Supplier<T>> supplierList) {
        List<Future<T>> futureList = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Supplier<T> supplier : supplierList) {
                futureList.add(executor.submit(supplier::get));
            }
            List<T> resultList = new ArrayList<>(futureList.size());
            for (Future<T> future : futureList) {
                resultList.add(future.get());
            }
            return resultList;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程执行被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw ExceptionUtil.wrapRuntime(cause);
        }
    }

}
