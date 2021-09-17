package com.ruoyi.framework.aspectj;

import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RateType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 限流处理
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    // 配置织入点
    @Pointcut("@annotation(com.ruoyi.common.annotation.RateLimiter)")
    public void rateLimiterPointCut() {
    }

    @Before("rateLimiterPointCut()")
    public void doBefore(JoinPoint point) throws Throwable {
        RateLimiter rateLimiter = getAnnotationRateLimiter(point);
        String key = rateLimiter.key();
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        try {
            RateType rateType = RateType.OVERALL;
            if (rateLimiter.limitType() == LimitType.IP) {
                rateType = RateType.PER_CLIENT;
            }
            // 返回 false 说明 获取令牌失败
            if (!RedisUtils.rateLimiter(key, rateType, count, time)) {
                throw new ServiceException("访问过于频繁，请稍后再试");
            }
            log.info("限制请求'{}',缓存key'{}'", count, key);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("服务器限流异常，请稍后再试");
        }
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private RateLimiter getAnnotationRateLimiter(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            return method.getAnnotation(RateLimiter.class);
        }
        return null;
    }

}
