package com.ruoyi.framework.config;

import com.ruoyi.common.exception.CustomException;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 异步配置
 *
 * @author Lion Li
 */
@EnableAsync
@Configuration
public class AsyncConfig extends AsyncConfigurerSupport {

	@Autowired
	@Qualifier("scheduledExecutorService")
	private ScheduledExecutorService scheduledExecutorService;

    /**
     * 异步执行需要使用权限框架自带的包装线程池  保证权限信息的传递
     */
    @Override
    public Executor getAsyncExecutor() {
        return new DelegatingSecurityContextExecutorService(scheduledExecutorService);
    }

    /**
     * 异步执行异常处理
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            throwable.printStackTrace();
            throw new CustomException(
                    "Exception message - " + throwable.getMessage()
                    + ", Method name - " + method.getName()
                    + ", Parameter value - " + Arrays.toString(objects));
        };
    }

}
