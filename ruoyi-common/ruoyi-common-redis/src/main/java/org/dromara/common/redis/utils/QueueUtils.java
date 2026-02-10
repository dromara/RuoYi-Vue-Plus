package org.dromara.common.redis.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RPriorityBlockingQueue;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 分布式队列工具
 * 轻量级队列 重量级数据量 请使用 MQ
 * 要求 redis 5.X 以上
 *
 * @author Lion Li
 * @version 3.6.0 新增
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueUtils {

    private static final RedissonClient CLIENT = SpringUtils.getBean(RedissonClient.class);


    /**
     * 获取客户端实例
     */
    public static RedissonClient getClient() {
        return CLIENT;
    }

    /**
     * 添加普通队列数据
     *
     * @param queueName 队列名
     * @param data      数据
     */
    public static <T> boolean addQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.offer(data);
    }

    /**
     * 通用获取一个队列数据 没有数据返回 null(不支持延迟队列)
     *
     * @param queueName 队列名
     */
    public static <T> T getQueueObject(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.poll();
    }

    /**
     * 通用删除队列数据(不支持延迟队列)
     */
    public static <T> boolean removeQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.remove(data);
    }

    /**
     * 通用销毁队列 所有阻塞监听 报错(不支持延迟队列)
     */
    public static <T> boolean destroyQueue(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.delete();
    }

    /**
     * 添加优先队列数据
     *
     * @param queueName 队列名
     * @param data      数据
     */
    public static <T> boolean addPriorityQueueObject(String queueName, T data) {
        RPriorityBlockingQueue<T> priorityBlockingQueue = CLIENT.getPriorityBlockingQueue(queueName);
        return priorityBlockingQueue.offer(data);
    }

    /**
     * 优先队列获取一个队列数据 没有数据返回 null(不支持延迟队列)
     *
     * @param queueName 队列名
     */
    public static <T> T getPriorityQueueObject(String queueName) {
        RPriorityBlockingQueue<T> queue = CLIENT.getPriorityBlockingQueue(queueName);
        return queue.poll();
    }

    /**
     * 优先队列删除队列数据(不支持延迟队列)
     */
    public static <T> boolean removePriorityQueueObject(String queueName, T data) {
        RPriorityBlockingQueue<T> queue = CLIENT.getPriorityBlockingQueue(queueName);
        return queue.remove(data);
    }

    /**
     * 优先队列销毁队列 所有阻塞监听 报错(不支持延迟队列)
     */
    public static <T> boolean destroyPriorityQueue(String queueName) {
        RPriorityBlockingQueue<T> queue = CLIENT.getPriorityBlockingQueue(queueName);
        return queue.delete();
    }

    /**
     * 订阅阻塞队列(可订阅所有实现类)
     */
    public static <T> void subscribeBlockingQueue(String queueName, Function<T, CompletionStage<Void>> consumer) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        // 延迟队列已经被redisson官方废弃不建议使用
        // if (isDelayed) {
        //     // 订阅延迟队列
        //     CLIENT.getDelayedQueue(queue);
        // }
        queue.subscribeOnElements(consumer);
    }

}
