package com.ruoyi.common.utils.spring;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

/**
 * spring工具类
 *
 * @author Lion Li
 */
@Component
public final class SpringUtils extends SpringUtil {

	/**
	 * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
	 *
	 * @param name
	 * @return boolean
	 */
	public static boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	/**
	 * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。
	 * 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
	 *
	 * @param name
	 * @return boolean
	 */
	public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	/**
	 * @param name
	 * @return Class 注册对象的类型
	 */
	public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(name);
	}

	/**
	 * 如果给定的bean名字在bean定义中有别名，则返回这些别名
	 *
	 * @param name
	 */
	public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getAliases(name);
	}

	/**
	 * 获取aop代理对象
	 *
	 * @param invoker
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAopProxy(T invoker) {
		return (T) AopContext.currentProxy();
	}

}
