package com.ruoyi.framework.aspectj;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.ruoyi.common.annotation.DataSource;
import com.ruoyi.common.utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 多数据源处理
 *
 * @author Lion Li
 */
@Aspect
@Order(-500)
@Component
public class DataSourceAspect {

	@Around("@annotation(dataSource) || @within(dataSource)")
	public Object around(ProceedingJoinPoint point, DataSource dataSource) throws Throwable {
		if (StringUtils.isNotNull(dataSource)) {
			DynamicDataSourceContextHolder.poll();
			String source = dataSource.value().getSource();
			DynamicDataSourceContextHolder.push(source);
		}

		try {
			return point.proceed();
		} finally {
			// 销毁数据源 在执行方法之后
			DynamicDataSourceContextHolder.clear();
		}
	}

}
