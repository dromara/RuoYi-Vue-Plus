package com.ruoyi.framework.aspectj;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.properties.TokenProperties;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.RepeatSubmitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 防止重复提交
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Aspect
@Component
public class RepeatSubmitAspect {

    private final TokenProperties tokenProperties;
    private final RepeatSubmitProperties repeatSubmitProperties;
    private final LockTemplate lockTemplate;

    // 配置织入点
    @Pointcut("@annotation(com.ruoyi.common.annotation.RepeatSubmit)")
    public void repeatSubmitPointCut() {
    }

    @Before("repeatSubmitPointCut()")
    public void doBefore(JoinPoint point) throws Throwable {
        RepeatSubmit repeatSubmit = getAnnotationRateLimiter(point);
        // 如果注解不为0 则使用注解数值
        long intervalTime = repeatSubmitProperties.getIntervalTime();
        if (repeatSubmit.intervalTime() > 0) {
            intervalTime = repeatSubmit.timeUnit().toMillis(repeatSubmit.intervalTime());
        }
        if (intervalTime < 1000) {
            throw new ServiceException("重复提交间隔时间不能小于'1'秒");
        }
        HttpServletRequest request = ServletUtils.getRequest();
        String nowParams = StrUtil.join(",", point.getArgs());

        // 请求地址（作为存放cache的key值）
        String url = request.getRequestURI();

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = request.getHeader(tokenProperties.getHeader());
        if (StringUtils.isEmpty(submitKey)) {
            submitKey = url;
        }
        submitKey = SecureUtil.md5(submitKey + ":" + nowParams);
        // 唯一标识（指定key + 消息头）
        String cacheRepeatKey = Constants.REPEAT_SUBMIT_KEY + submitKey;
        LockInfo lock = lockTemplate.lock(cacheRepeatKey, intervalTime, intervalTime / 2);
        if (lock == null) {
            throw new ServiceException("不允许重复提交，请稍后再试!");
        }
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private RepeatSubmit getAnnotationRateLimiter(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            return method.getAnnotation(RepeatSubmit.class);
        }
        return null;
    }

}
