package org.dromara.common.redis;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.redis.annotation.RateLimiter;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.redis.aspectj.RateLimiterAspect;
import org.dromara.common.redis.aspectj.RepeatSubmitAspect;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.common.redis.utils.QueueUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.redis.utils.SequenceUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBatch;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RMapAsync;
import org.redisson.api.ObjectListener;
import org.redisson.api.RPriorityBlockingQueue;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.RateType;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Redis 基础设施契约单元测试")
class RedisAspectContractTest {

    private static RedissonClient redissonClient;
    private static CacheManager cacheManager;

    /**
     * 注入仅包含 mock RedissonClient 的内存 Spring 容器，避免连接真实 Redis 并覆盖工具类初始化契约。
     */
    @BeforeAll
    static void initializeRedisInfrastructure() {
        redissonClient = mock(RedissonClient.class);
        cacheManager = mock(CacheManager.class);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("redissonClient", redissonClient);
        context.getBeanFactory().registerSingleton("cacheManager", cacheManager);
        context.getBeanFactory().registerSingleton("jsonMapper", JsonMapper.builder().build());
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 每个用例前清理 Redisson mock 的行为和配置，避免不同工具契约互相影响。
     */
    @BeforeEach
    void resetRedisClient() {
        reset(redissonClient);
        reset(cacheManager);
    }

    /**
     * 验证对象缓存的普通写入、带 TTL 写入、条件写入、读取、过期和删除都委托给 RBucket。
     */
    @Test
    @DisplayName("委托对象缓存和 TTL 操作")
    void shouldDelegateObjectCacheOperations() {
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        Duration duration = Duration.ofMinutes(5);
        when(redissonClient.<String>getBucket("object:key")).thenReturn(bucket);
        when(bucket.setIfAbsent("created", duration)).thenReturn(true);
        when(bucket.setIfExists("updated", duration)).thenReturn(false);
        when(bucket.get()).thenReturn("value");
        when(bucket.remainTimeToLive()).thenReturn(12_000L);
        when(bucket.expire(duration)).thenReturn(true);
        when(bucket.isExists()).thenReturn(true);
        when(bucket.delete()).thenReturn(true);

        RedisUtils.setCacheObject("object:key", "plain");
        RedisUtils.setCacheObject("object:key", "timed", duration);

        assertTrue(RedisUtils.setObjectIfAbsent("object:key", "created", duration));
        assertFalse(RedisUtils.setObjectIfExists("object:key", "updated", duration));
        assertEquals("value", RedisUtils.getCacheObject("object:key"));
        assertEquals(12_000L, RedisUtils.getTimeToLive("object:key"));
        assertTrue(RedisUtils.expire("object:key", duration));
        assertTrue(RedisUtils.isExistsObject("object:key"));
        assertTrue(RedisUtils.deleteObject("object:key"));
        assertFalse(RedisUtils.deleteObject((String) null));

        verify(bucket).set("plain");
        verify(bucket).set("timed", duration);
    }

    /**
     * 验证保留 TTL 优先使用 Redisson 原生命令，命令不可用时按剩余 TTL 降级写入。
     */
    @Test
    @DisplayName("保留对象 TTL 并兼容旧 Redis")
    void shouldKeepObjectTtlWithCompatibilityFallback() {
        @SuppressWarnings("unchecked")
        RBucket<String> nativeBucket = mock(RBucket.class);
        @SuppressWarnings("unchecked")
        RBucket<String> timedFallback = mock(RBucket.class);
        @SuppressWarnings("unchecked")
        RBucket<String> plainFallback = mock(RBucket.class);
        when(redissonClient.<String>getBucket("ttl:native")).thenReturn(nativeBucket);
        when(redissonClient.<String>getBucket("ttl:timed")).thenReturn(timedFallback);
        when(redissonClient.<String>getBucket("ttl:plain")).thenReturn(plainFallback);
        doThrow(new UnsupportedOperationException("keep ttl unsupported"))
            .when(timedFallback).setAndKeepTTL("value");
        doThrow(new UnsupportedOperationException("keep ttl unsupported"))
            .when(plainFallback).setAndKeepTTL("value");
        when(timedFallback.remainTimeToLive()).thenReturn(1_500L);
        when(plainFallback.remainTimeToLive()).thenReturn(-1L);

        RedisUtils.setCacheObject("ttl:native", "value", true);
        RedisUtils.setCacheObject("ttl:timed", "value", true);
        RedisUtils.setCacheObject("ttl:plain", "value", true);

        verify(nativeBucket).setAndKeepTTL("value");
        verify(timedFallback).set("value", Duration.ofMillis(1_500L));
        verify(plainFallback).set("value");
    }

    /**
     * 验证 List、Set 和 Map 工具方法保留 Redisson 的追加、范围读取、去重和 Hash 批量读取语义。
     */
    @Test
    @DisplayName("委托 Redis 集合操作")
    void shouldDelegateCollectionOperations() {
        @SuppressWarnings("unchecked")
        RList<String> list = mock(RList.class);
        @SuppressWarnings("unchecked")
        RSet<String> set = mock(RSet.class);
        @SuppressWarnings("unchecked")
        RMap<String, String> map = mock(RMap.class);
        when(redissonClient.<String>getList("list:key")).thenReturn(list);
        when(redissonClient.<String>getSet("set:key")).thenReturn(set);
        when(redissonClient.<String, String>getMap("map:key")).thenReturn(map);
        when(list.addAll(List.of("a", "b"))).thenReturn(true);
        when(list.add("c")).thenReturn(true);
        when(list.readAll()).thenReturn(List.of("a", "b", "c"));
        when(list.range(1, 2)).thenReturn(List.of("b", "c"));
        when(set.addAll(Set.of("a", "b"))).thenReturn(true);
        when(set.add("c")).thenReturn(true);
        when(set.readAll()).thenReturn(Set.of("a", "b", "c"));
        when(map.readAllMap()).thenReturn(Map.of("a", "1", "b", "2"));
        when(map.keySet()).thenReturn(Set.of("a", "b"));
        when(map.get("a")).thenReturn("1");
        when(map.remove("a")).thenReturn("1");
        when(map.getAll(Set.of("a", "b"))).thenReturn(Map.of("a", "1", "b", "2"));

        assertTrue(RedisUtils.setCacheList("list:key", List.of("a", "b")));
        assertTrue(RedisUtils.addCacheList("list:key", "c"));
        assertEquals(List.of("a", "b", "c"), RedisUtils.getCacheList("list:key"));
        assertEquals(List.of("b", "c"), RedisUtils.getCacheListRange("list:key", 1, 2));
        assertTrue(RedisUtils.setCacheSet("set:key", Set.of("a", "b")));
        assertTrue(RedisUtils.addCacheSet("set:key", "c"));
        assertEquals(Set.of("a", "b", "c"), RedisUtils.getCacheSet("set:key"));

        RedisUtils.setCacheMap("map:key", Map.of("a", "1", "b", "2"));
        RedisUtils.setCacheMap("map:null", null);
        RedisUtils.setCacheMapValue("map:key", "c", "3");
        assertEquals(Map.of("a", "1", "b", "2"), RedisUtils.getCacheMap("map:key"));
        assertEquals(Set.of("a", "b"), RedisUtils.getCacheMapKeySet("map:key"));
        assertEquals("1", RedisUtils.getCacheMapValue("map:key", "a"));
        assertEquals("1", RedisUtils.delCacheMapValue("map:key", "a"));
        assertEquals(Map.of("a", "1", "b", "2"),
            RedisUtils.getMultiCacheMapValue("map:key", Set.of("a", "b")));

        verify(map).putAll(Map.of("a", "1", "b", "2"));
        verify(map).put("c", "3");
        verify(redissonClient, never()).getMap("map:null");
    }

    /**
     * 验证多 Key 和多 Hash 字段删除使用 RBatch 一次执行，空集合不创建批处理。
     */
    @Test
    @DisplayName("批量删除 Redis 数据")
    void shouldDeleteKeysAndHashFieldsInBatches() {
        RBatch keyBatch = mock(RBatch.class);
        RBatch mapBatch = mock(RBatch.class);
        @SuppressWarnings("unchecked")
        RBucketAsync<Object> firstBucket = mock(RBucketAsync.class);
        @SuppressWarnings("unchecked")
        RBucketAsync<Object> secondBucket = mock(RBucketAsync.class);
        @SuppressWarnings("unchecked")
        RMapAsync<String, String> asyncMap = mock(RMapAsync.class);
        when(redissonClient.createBatch()).thenReturn(keyBatch, mapBatch);
        when(keyBatch.getBucket("a")).thenReturn(firstBucket);
        when(keyBatch.getBucket("b")).thenReturn(secondBucket);
        when(mapBatch.<String, String>getMap("map:key")).thenReturn(asyncMap);

        RedisUtils.deleteObject((java.util.Collection<?>) null);
        RedisUtils.deleteObject(List.of());
        RedisUtils.deleteObject(List.of("a", "b"));
        RedisUtils.delMultiCacheMapValue("map:key", Set.of("x", "y"));

        verify(firstBucket).deleteAsync();
        verify(secondBucket).deleteAsync();
        verify(keyBatch).execute();
        verify(asyncMap).removeAsync("x");
        verify(asyncMap).removeAsync("y");
        verify(mapBatch).execute();
        verify(redissonClient, times(2)).createBatch();
    }

    /**
     * 验证原子计数和 Key 扫描、模式删除、存在性检查使用 Redisson 对应 API。
     */
    @Test
    @DisplayName("委托原子值和 Key 管理")
    void shouldDelegateAtomicAndKeyOperations() {
        RAtomicLong atomic = mock(RAtomicLong.class);
        RKeys keys = mock(RKeys.class);
        when(redissonClient.getAtomicLong("counter")).thenReturn(atomic);
        when(redissonClient.getKeys()).thenReturn(keys);
        when(atomic.get()).thenReturn(10L);
        when(atomic.incrementAndGet()).thenReturn(11L);
        when(atomic.decrementAndGet()).thenReturn(9L);
        when(keys.getKeysStream(any(KeysScanOptions.class))).thenReturn(Stream.of("user:1", "user:2"));
        when(keys.countExists("user:1")).thenReturn(1L);

        RedisUtils.setAtomicValue("counter", 10L);

        assertEquals(10L, RedisUtils.getAtomicValue("counter"));
        assertEquals(11L, RedisUtils.incrAtomicValue("counter"));
        assertEquals(9L, RedisUtils.decrAtomicValue("counter"));
        assertEquals(List.of("user:1", "user:2"), RedisUtils.keys("user:*"));
        assertTrue(RedisUtils.hasKey("user:1"));
        RedisUtils.deleteKeys("user:*");

        verify(atomic).set(10L);
        verify(keys).deleteByPattern("user:*");
    }

    /**
     * 验证发布、订阅、消息回调和取消订阅的 Redisson Topic 契约。
     */
    @Test
    @DisplayName("委托 Redis 发布订阅")
    void shouldDelegatePublishAndSubscribeOperations() {
        RTopic topic = mock(RTopic.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<MessageListener<String>> listenerCaptor = ArgumentCaptor.forClass(MessageListener.class);
        AtomicReference<String> subscribedMessage = new AtomicReference<>();
        AtomicReference<String> publishedMessage = new AtomicReference<>();
        when(redissonClient.getTopic("events")).thenReturn(topic);
        when(topic.addListener(eq(String.class), listenerCaptor.capture())).thenReturn(42);

        int listenerId = RedisUtils.subscribeAndGetListenerId("events", String.class, subscribedMessage::set);
        listenerCaptor.getValue().onMessage("events", "received");
        RedisUtils.publish("events", "published", publishedMessage::set);
        RedisUtils.publish("events", "plain");
        RedisUtils.unsubscribe("events", listenerId);

        assertEquals(42, listenerId);
        assertEquals("received", subscribedMessage.get());
        assertEquals("published", publishedMessage.get());
        verify(topic).removeListener(42);
        verify(topic).publish("published");
        verify(topic).publish("plain");
    }

    /**
     * 验证 RedisUtils 限流入口按秒构建速率与超时配置，并区分获取成功和令牌耗尽。
     */
    @Test
    @DisplayName("委托 Redisson 令牌桶")
    void shouldDelegateRateLimiterOperations() {
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("rate:key")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true, false);
        when(limiter.availablePermits()).thenReturn(3L);

        assertEquals(3L, RedisUtils.rateLimiter("rate:key", RateType.OVERALL, 4, 10));
        assertEquals(-1L, RedisUtils.rateLimiter("rate:key", RateType.OVERALL, 4, 10, 5));

        verify(limiter).trySetRate(RateType.OVERALL, 4, Duration.ofSeconds(10), Duration.ZERO);
        verify(limiter).trySetRate(RateType.OVERALL, 4, Duration.ofSeconds(10), Duration.ofSeconds(5));
        verify(limiter, times(2)).tryAcquire();
    }

    /**
     * 验证对象、List、Set 和 Map 监听器被注册到正确的 Redisson 数据结构。
     */
    @Test
    @DisplayName("注册 Redis 对象监听器")
    void shouldRegisterObjectListeners() {
        ObjectListener listener = mock(ObjectListener.class);
        RBucket<Object> bucket = mock(RBucket.class);
        RList<Object> list = mock(RList.class);
        RSet<Object> set = mock(RSet.class);
        RMap<String, Object> map = mock(RMap.class);
        when(redissonClient.getBucket("bucket")).thenReturn(bucket);
        when(redissonClient.getList("list")).thenReturn(list);
        when(redissonClient.getSet("set")).thenReturn(set);
        when(redissonClient.<String, Object>getMap("map")).thenReturn(map);

        RedisUtils.addObjectListener("bucket", listener);
        RedisUtils.addListListener("list", listener);
        RedisUtils.addSetListener("set", listener);
        RedisUtils.addMapListener("map", listener);

        verify(bucket).addListener(listener);
        verify(list).addListener(listener);
        verify(set).addListener(listener);
        verify(map).addListener(listener);
        assertSame(redissonClient, RedisUtils.getClient());
    }

    /**
     * 验证普通阻塞队列和优先队列的写入、读取、删除、销毁及元素订阅委托给正确的 Redisson 类型。
     */
    @Test
    @DisplayName("委托 Redisson 阻塞队列")
    void shouldDelegateQueueOperations() {
        @SuppressWarnings("unchecked")
        RBlockingQueue<String> queue = mock(RBlockingQueue.class);
        @SuppressWarnings("unchecked")
        RPriorityBlockingQueue<String> priorityQueue = mock(RPriorityBlockingQueue.class);
        Function<String, CompletionStage<Void>> consumer = value -> CompletableFuture.completedFuture(null);
        when(redissonClient.<String>getBlockingQueue("normal")).thenReturn(queue);
        when(redissonClient.<String>getPriorityBlockingQueue("priority")).thenReturn(priorityQueue);
        when(queue.offer("a")).thenReturn(true);
        when(queue.poll()).thenReturn("a");
        when(queue.remove("a")).thenReturn(true);
        when(queue.delete()).thenReturn(true);
        when(priorityQueue.offer("b")).thenReturn(true);
        when(priorityQueue.poll()).thenReturn("b");
        when(priorityQueue.remove("b")).thenReturn(true);
        when(priorityQueue.delete()).thenReturn(true);

        assertTrue(QueueUtils.addQueueObject("normal", "a"));
        assertEquals("a", QueueUtils.getQueueObject("normal"));
        assertTrue(QueueUtils.removeQueueObject("normal", "a"));
        assertTrue(QueueUtils.destroyQueue("normal"));
        QueueUtils.subscribeBlockingQueue("normal", consumer);
        assertTrue(QueueUtils.addPriorityQueueObject("priority", "b"));
        assertEquals("b", QueueUtils.getPriorityQueueObject("priority"));
        assertTrue(QueueUtils.removePriorityQueueObject("priority", "b"));
        assertTrue(QueueUtils.destroyPriorityQueue("priority"));

        verify(queue).subscribeOnElements(consumer);
        assertSame(redissonClient, QueueUtils.getClient());
    }

    /**
     * 验证发号器会将非法初始值和步长恢复为默认值，并传递过期时间。
     */
    @Test
    @DisplayName("初始化 Redisson 发号器")
    void shouldInitializeIdGeneratorWithSafeDefaults() {
        RIdGenerator idGenerator = mock(RIdGenerator.class);
        Duration expiration = Duration.ofMinutes(3);
        when(redissonClient.getIdGenerator("order")).thenReturn(idGenerator);
        when(idGenerator.nextId()).thenReturn(12L, 13L);

        assertSame(idGenerator, SequenceUtils.getIdGenerator("order", expiration, 0, -1));
        assertEquals(12L, SequenceUtils.getNextId("order", expiration));
        assertEquals("13", SequenceUtils.getNextIdString("order", expiration));

        verify(idGenerator, times(3)).tryInit(SequenceUtils.DEFAULT_INIT_VALUE, SequenceUtils.DEFAULT_STEP_VALUE);
        verify(idGenerator, times(3)).expire(expiration);
    }

    /**
     * 验证日期和日期时间发号格式使用稳定的 Redis Key，并正确处理业务前缀与左补零。
     */
    @Test
    @DisplayName("格式化日期序列号")
    void shouldFormatDateBasedSequenceIds() {
        RIdGenerator dateGenerator = mock(RIdGenerator.class);
        RIdGenerator dateTimeGenerator = mock(RIdGenerator.class);
        LocalDate date = LocalDate.of(2026, 9, 15);
        LocalDateTime dateTime = LocalDateTime.of(2026, 9, 15, 12, 34, 56);
        when(redissonClient.getIdGenerator("ORD20260915")).thenReturn(dateGenerator);
        when(redissonClient.getIdGenerator("INV20260915123456")).thenReturn(dateTimeGenerator);
        when(dateGenerator.nextId()).thenReturn(7L);
        when(dateTimeGenerator.nextId()).thenReturn(42L);

        assertEquals("ORD202609150007", SequenceUtils.getDateId("ORD", true, 4, date, 10, 2));
        assertEquals("2026091512345600042",
            SequenceUtils.getDateTimeId("INV", false, 5, dateTime, 3, 5));

        verify(dateGenerator).tryInit(10, 2);
        verify(dateGenerator).expire(SequenceUtils.DEFAULT_EXPIRE_TIME_DAY);
        verify(dateTimeGenerator).tryInit(3, 5);
        verify(dateTimeGenerator).expire(SequenceUtils.DEFAULT_EXPIRE_TIME_MINUTE);
    }

    /**
     * 验证 CacheUtils 通过 Spring CacheManager 完成读写、驱逐和清理，缓存不存在时立即报错。
     */
    @Test
    @DisplayName("委托 Spring Cache 操作")
    void shouldDelegateSpringCacheOperations() {
        Cache cache = mock(Cache.class);
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(cacheManager.getCache("users")).thenReturn(cache);
        when(cache.get("1")).thenReturn(wrapper);
        when(wrapper.get()).thenReturn("alice");

        assertEquals("alice", CacheUtils.get("users", "1"));
        CacheUtils.put("users", "2", "bob");
        CacheUtils.evict("users", "1");
        CacheUtils.clear("users");

        verify(cache).put("2", "bob");
        verify(cache).evict("1");
        verify(cache).clear();
        assertThrows(IllegalArgumentException.class, () -> CacheUtils.get("missing", "1"));
    }

    /**
     * 验证限流切面按注解配置调用 Redisson 令牌桶，并在无剩余令牌时抛出业务异常。
     */
    @Test
    @DisplayName("执行默认接口限流")
    void shouldAcquireRateLimiterTokenAndRejectWhenExhausted() throws Exception {
        RateLimiter annotation = annotation("limitedEndpoint", RateLimiter.class);
        JoinPoint point = mock(JoinPoint.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/orders");
        RateLimiterAspect aspect = new RateLimiterAspect();
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter("global:rate_limit:/orders:order")).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true, false);
        when(limiter.availablePermits()).thenReturn(1L);

        try (MockedStatic<ServletUtils> servlet = mockStatic(ServletUtils.class)) {
            servlet.when(ServletUtils::getRequest).thenReturn(request);

            assertDoesNotThrow(() -> aspect.doBefore(point, annotation));
            ServiceException exception = assertThrows(ServiceException.class,
                () -> aspect.doBefore(point, annotation));

            assertEquals("请求过于频繁", exception.getMessage());
            verify(limiter, times(2)).trySetRate(RateType.OVERALL, 2, Duration.ofSeconds(10),
                Duration.ofSeconds(60));
            verify(limiter, times(2)).tryAcquire();
        }
    }

    /**
     * 验证防重间隔下限在访问请求和 Redis 前生效，避免无效配置进入运行期。
     */
    @Test
    @DisplayName("拒绝过短的防重复提交间隔")
    void shouldRejectTooShortRepeatSubmitInterval() throws Exception {
        RepeatSubmit annotation = annotation("invalidRepeat", RepeatSubmit.class);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> new RepeatSubmitAspect().doBefore(mock(JoinPoint.class), annotation));

        assertEquals("重复提交间隔时间不能小于'1'秒", exception.getMessage());
    }

    /**
     * 验证失败响应会删除本次写入的防重键，而成功写入使用请求参数生成稳定摘要。
     */
    @Test
    @DisplayName("失败响应释放防重复提交键")
    void shouldReleaseRepeatSubmitKeyAfterFailedResponse() throws Throwable {
        RepeatSubmit annotation = annotation("validRepeat", RepeatSubmit.class);
        JoinPoint point = mock(JoinPoint.class);
        when(point.getArgs()).thenReturn(new Object[]{Map.of("orderId", 1L)});
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
        RepeatSubmitAspect aspect = new RepeatSubmitAspect();
        @SuppressWarnings("unchecked")
        RBucket<Object> bucket = mock(RBucket.class);
        when(redissonClient.getBucket(anyString())).thenReturn(bucket);
        when(bucket.setIfAbsent("", Duration.ofSeconds(2))).thenReturn(true);
        when(bucket.delete()).thenReturn(true);

        try (MockedStatic<ServletUtils> servlet = mockStatic(ServletUtils.class)) {
            servlet.when(ServletUtils::getRequest).thenReturn(request);

            aspect.doBefore(point, annotation);
            aspect.doAfterReturning(point, annotation, R.fail("failed"));

            verify(redissonClient, times(2)).getBucket(startsWith("global:repeat_submit:/orders"));
            verify(bucket).setIfAbsent("", Duration.ofSeconds(2));
            verify(bucket).delete();
        }
    }

    /**
     * 验证防重参数摘要会过滤上传文件和嵌套容器中的 Servlet 对象，避免序列化基础设施对象。
     */
    @Test
    @DisplayName("过滤不可序列化的防重参数")
    void shouldFilterInfrastructureObjectsFromRepeatSubmitArguments() {
        RepeatSubmitAspect aspect = new RepeatSubmitAspect();
        MockMultipartFile file = new MockMultipartFile("file", new byte[]{1});

        assertTrue(aspect.isFilterObject(file));
        assertTrue(aspect.isFilterObject(List.of("value", file)));
        assertTrue(aspect.isFilterObject(Map.of("request", mock(HttpServletRequest.class))));
        assertFalse(aspect.isFilterObject(List.of("value", 1L)));
    }

    /**
     * 读取测试方法上的目标注解，确保测试使用与运行期相同的注解代理对象。
     *
     * @param methodName     测试方法名
     * @param annotationType 注解类型
     * @param <A>            注解类型
     * @return 方法注解
     */
    private static <A extends java.lang.annotation.Annotation> A annotation(String methodName, Class<A> annotationType)
        throws Exception {
        Method method = AnnotatedEndpoints.class.getDeclaredMethod(methodName);
        return method.getAnnotation(annotationType);
    }

    private static class AnnotatedEndpoints {

        /**
         * 提供默认类型的限流配置供切面测试。
         */
        @RateLimiter(key = "order", time = 10, count = 2, timeout = 60, message = "请求过于频繁")
        private void limitedEndpoint() {
        }

        /**
         * 提供不满足最小间隔的防重配置供校验测试。
         */
        @RepeatSubmit(interval = 999)
        private void invalidRepeat() {
        }

        /**
         * 提供有效防重配置供 Redis 键生命周期测试。
         */
        @RepeatSubmit(interval = 2, timeUnit = java.util.concurrent.TimeUnit.SECONDS, message = "重复请求")
        private void validRepeat() {
        }
    }
}
