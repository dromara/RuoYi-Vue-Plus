package org.dromara.common.web.interceptor;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
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
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
     * 请求进入控制器前记录入参并启动耗时统计。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 目标处理器
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
                    JsonMapper jsonMapper = JsonUtils.getJsonMapper();
                    JsonNode rootNode = jsonMapper.readTree(jsonParam);
                    removeSensitiveFields(rootNode, SystemConstants.EXCLUDE_PROPERTIES);
                    jsonParam = rootNode.toString();
                }
            }
            log.info("[PLUS]开始请求 => URL[{}],参数类型[json],参数:[{}]", url, jsonParam);
        } else {
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (MapUtil.isNotEmpty(parameterMap)) {
                Map<String, String[]> map = new LinkedHashMap<>(parameterMap);
                MapUtil.removeAny(map, SystemConstants.EXCLUDE_PROPERTIES);
                String parameters = JsonUtils.toJsonString(map);
                log.info("[PLUS]开始请求 => URL[{}],参数类型[param],参数:[{}]", url, parameters);
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
     * 递归移除 JSON 节点中的敏感字段，避免在日志中输出密码等敏感信息。
     *
     * @param node 当前 JSON 节点
     * @param excludeProperties 需要排除的字段名集合
     */
    private void removeSensitiveFields(JsonNode node, String[] excludeProperties) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            // 收集要删除的字段名（避免 ConcurrentModification）
            Set<String> fieldsToRemove = new HashSet<>();
            objectNode.propertyNames().forEach(fieldName -> {
                if (ArrayUtil.contains(excludeProperties, fieldName)) {
                    fieldsToRemove.add(fieldName);
                }
            });
            fieldsToRemove.forEach(objectNode::remove);
            // 递归处理子节点
            objectNode.values().forEach(child -> removeSensitiveFields(child, excludeProperties));
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode child : arrayNode) {
                removeSensitiveFields(child, excludeProperties);
            }
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    /**
     * 请求完成后输出最终耗时，并清理线程内缓存的计时器。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 目标处理器
     * @param ex 请求处理过程中的异常
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
