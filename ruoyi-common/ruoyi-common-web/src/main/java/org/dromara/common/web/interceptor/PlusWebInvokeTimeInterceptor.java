package org.dromara.common.web.interceptor;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.web.filter.RepeatedlyRequestWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Web 调用时间统计拦截器，同时记录请求参数并对敏感字段做脱敏处理。
 *
 * @author Lion Li
 * @since 3.3.0
 */
@Slf4j
public class PlusWebInvokeTimeInterceptor implements HandlerInterceptor {

    private final static ThreadLocal<StopWatch> KEY_CACHE = new ThreadLocal<>();

    /**
     * 请求参数日志最大长度。
     */
    private static final int MAX_PARAM_LOG_LENGTH = 4000;

    /**
     * 请求进入控制器前记录入参并启动耗时统计。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  目标处理器
     * @return 始终返回 true，继续后续处理流程
     * @throws Exception 读取请求体或解析 JSON 失败时抛出
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String url = request.getMethod() + " " + request.getRequestURI();
        // 打印请求参数
        if (isJsonRequest(request)) {
            String jsonParam = "";
            if (request instanceof RepeatedlyRequestWrapper) {
                jsonParam = IoUtil.read(request.getReader());
                if (StringUtils.isNotBlank(jsonParam)) {
                    jsonParam = sanitizeJsonParam(jsonParam);
                }
            }
            log.info("[PLUS]开始请求 => URL[{}],参数类型[json],参数:[{}]", url, limit(jsonParam));
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (MapUtil.isNotEmpty(parameterMap)) {
                Map<String, String[]> map = new LinkedHashMap<>(parameterMap);
                MapUtil.removeAny(map, SystemConstants.EXCLUDE_PROPERTIES);
                String parameters = JsonUtils.toJsonString(map);
                log.info("[PLUS]开始请求 => URL[{}],参数类型[param],参数:[{}]", url, limit(parameters));
            } else {
                log.info("[PLUS]开始请求 => URL[{}],无参数", url);
            }
        }

        StopWatch stopWatch = new StopWatch();
        KEY_CACHE.set(stopWatch);
        stopWatch.start();

        return true;
    }

    /**
     * 清洗 JSON 请求参数日志，解析失败时不影响主请求。
     *
     * @param jsonParam 原始 JSON 字符串
     * @return 清洗后的参数日志
     */
    private String sanitizeJsonParam(String jsonParam) {
        try {
            JsonMapper jsonMapper = JsonUtils.getJsonMapper();
            JsonNode rootNode = jsonMapper.readTree(jsonParam);
            JsonUtils.removeFields(rootNode, SystemConstants.EXCLUDE_PROPERTIES);
            return rootNode.toString();
        } catch (Exception e) {
            log.debug("[PLUS]请求参数 JSON 解析失败，跳过结构化脱敏: {}", e.getMessage());
            return jsonParam;
        }
    }

    /**
     * 限制日志字段长度。
     *
     * @param value 原始字符串
     * @return 截断后的字符串
     */
    private String limit(String value) {
        return StringUtils.substring(value, 0, MAX_PARAM_LOG_LENGTH);
    }

    /**
     * 请求完成后输出最终耗时，并清理线程内缓存的计时器。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  目标处理器
     * @param ex       请求处理过程中的异常
     * @throws Exception 拦截器链路抛出的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        StopWatch stopWatch = KEY_CACHE.get();
        if (ObjectUtil.isNotNull(stopWatch)) {
            stopWatch.stop();
            log.info("[PLUS]结束请求 => URL[{}],耗时:[{}]毫秒", request.getMethod() + " " + request.getRequestURI(), stopWatch.getDuration().toMillis());
            KEY_CACHE.remove();
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
