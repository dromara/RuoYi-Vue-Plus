package com.ruoyi.framework.interceptor;

import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.ServletUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 防止重复提交拦截器
 *
 * 移除继承 HandlerInterceptorAdapter 过期类
 * 改为实现 HandlerInterceptor 接口(官方推荐写法)
 *
 * @author Lion Li
 */
@Component
public abstract class RepeatSubmitInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
		throws Exception {
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			Method method = handlerMethod.getMethod();
			RepeatSubmit annotation = method.getAnnotation(RepeatSubmit.class);
			if (annotation != null) {
				if (this.isRepeatSubmit(annotation, request)) {
					AjaxResult ajaxResult = AjaxResult.error("不允许重复提交，请稍后再试");
					ServletUtils.renderString(response, JsonUtils.toJsonString(ajaxResult));
					return false;
				}
			}
			return true;
		} else {
			return HandlerInterceptor.super.preHandle(request, response, handler);
		}
	}

	/**
	 * 验证是否重复提交由子类实现具体的防重复提交的规则
	 */
	public abstract boolean isRepeatSubmit(RepeatSubmit annotation, HttpServletRequest request);
}
