package com.ruoyi.framework.interceptor.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.filter.RepeatedlyRequestWrapper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.RepeatSubmitProperties;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.framework.interceptor.RepeatSubmitInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 判断请求url和数据是否和上一次相同，
 * 如果和上次相同，则是重复提交表单。
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Component
public class SameUrlDataInterceptor extends RepeatSubmitInterceptor {
	public final String REPEAT_PARAMS = "repeatParams";

	public final String REPEAT_TIME = "repeatTime";

	private final TokenProperties tokenProperties;
	private final RepeatSubmitProperties repeatSubmitProperties;
	private final RedisCache redisCache;


	@SuppressWarnings("unchecked")
	@Override
	public boolean isRepeatSubmit(RepeatSubmit repeatSubmit, HttpServletRequest request) {
		// 如果注解不为0 则使用注解数值
		long intervalTime = repeatSubmitProperties.getIntervalTime();
		if (repeatSubmit.intervalTime() > 0) {
			intervalTime = repeatSubmit.timeUnit().toMillis(repeatSubmit.intervalTime());
		}
		String nowParams = "";
		if (request instanceof RepeatedlyRequestWrapper) {
			RepeatedlyRequestWrapper repeatedlyRequest = (RepeatedlyRequestWrapper) request;
			try {
				nowParams = IoUtil.readUtf8(repeatedlyRequest.getInputStream());
			} catch (IOException e) {
				log.warn("读取流出现问题！");
			}
		}

		// body参数为空，获取Parameter的数据
		if (StringUtils.isEmpty(nowParams)) {
			nowParams = JsonUtils.toJsonString(request.getParameterMap());
		}
		Map<String, Object> nowDataMap = new HashMap<String, Object>();
		nowDataMap.put(REPEAT_PARAMS, nowParams);
		nowDataMap.put(REPEAT_TIME, System.currentTimeMillis());

		// 请求地址（作为存放cache的key值）
		String url = request.getRequestURI();

		// 唯一值（没有消息头则使用请求地址）
		String submitKey = request.getHeader(tokenProperties.getHeader());
		if (StringUtils.isEmpty(submitKey)) {
			submitKey = url;
		}

		// 唯一标识（指定key + 消息头）
		String cacheRepeatKey = Constants.REPEAT_SUBMIT_KEY + submitKey;

		Object sessionObj = redisCache.getCacheObject(cacheRepeatKey);
		if (sessionObj != null) {
			Map<String, Object> sessionMap = (Map<String, Object>) sessionObj;
			if (sessionMap.containsKey(url)) {
				Map<String, Object> preDataMap = (Map<String, Object>) sessionMap.get(url);
				if (compareParams(nowDataMap, preDataMap) && compareTime(nowDataMap, preDataMap, intervalTime)) {
					return true;
				}
			}
		}
		Map<String, Object> cacheMap = new HashMap<String, Object>();
		cacheMap.put(url, nowDataMap);
		redisCache.setCacheObject(cacheRepeatKey, cacheMap, Convert.toInt(intervalTime), TimeUnit.MILLISECONDS);
		return false;
	}

	/**
	 * 判断参数是否相同
	 */
	private boolean compareParams(Map<String, Object> nowMap, Map<String, Object> preMap) {
		String nowParams = (String) nowMap.get(REPEAT_PARAMS);
		String preParams = (String) preMap.get(REPEAT_PARAMS);
		return nowParams.equals(preParams);
	}

	/**
	 * 判断两次间隔时间
	 */
	private boolean compareTime(Map<String, Object> nowMap, Map<String, Object> preMap, long intervalTime) {
		long time1 = (Long) nowMap.get(REPEAT_TIME);
		long time2 = (Long) preMap.get(REPEAT_TIME);
		return (time1 - time2) < intervalTime;
	}
}
