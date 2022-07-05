package com.ruoyi.framework.interceptor;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.ruoyi.common.filter.RepeatedlyRequestWrapper;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.Map;

/**
 * web的调用时间统计拦截器
 * dev环境有效
 *
 * @author Lion Li
 * @since 3.3.0
 */
@Slf4j
public class PlusWebInvokeTimeInterceptor implements HandlerInterceptor {

    private final String prodProfile = "prod";

    private final TransmittableThreadLocal<StopWatch> invokeTimeTL = new TransmittableThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!prodProfile.equals(SpringUtils.getActiveProfile())) {
            String url = request.getMethod() + " " + request.getRequestURI();

            // 打印请求参数
            if (isJsonRequest(request)) {
                String jsonParam = "";
                if (request instanceof RepeatedlyRequestWrapper) {
                    BufferedReader reader = request.getReader();
                    jsonParam = IoUtil.read(reader);
                }
                log.debug("[PLUS]开始请求 => URL[{}],参数类型[json],参数:[{}]", url, jsonParam);
            } else {
                Map<String, String[]> parameterMap = request.getParameterMap();
                if (MapUtil.isNotEmpty(parameterMap)) {
                    String parameters = JsonUtils.toJsonString(parameterMap);
                    log.debug("[PLUS]开始请求 => URL[{}],参数类型[param],参数:[{}]", url, parameters);
                } else {
                    log.debug("[PLUS]开始请求 => URL[{}],无参数", url);
                }
            }

            StopWatch stopWatch = new StopWatch();
            invokeTimeTL.set(stopWatch);
            stopWatch.start();
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (!prodProfile.equals(SpringUtils.getActiveProfile())) {
            StopWatch stopWatch = invokeTimeTL.get();
            stopWatch.stop();
            log.debug("[PLUS]结束请求 => URL[{}],耗时:[{}]毫秒", request.getMethod() + " " + request.getRequestURI(), stopWatch.getTime());
            invokeTimeTL.remove();
        }
    }

    /**
     * 判断本次请求的数据类型是否为json
     *
     * @param request request
     * @return boolean
     */
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null) {
            return StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE);
        }
        return false;
    }

}
