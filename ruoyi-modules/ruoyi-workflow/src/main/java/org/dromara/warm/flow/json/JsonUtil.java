/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.json;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.warm.flow.exception.FlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jackson 3：map和json字符串转换工具类
 *
 * @author warm
 */
public final class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    private JsonUtil() {
    }

    /**
     * 将字符串转为map
     *
     * @param jsonStr json字符串
     * @return map
     */
    public static Map<String, Object> strToMap(String jsonStr) {
        if (StrUtil.isNotEmpty(jsonStr)) {
            try {
                return JSON_MAPPER.readValue(jsonStr,
                    JSON_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            } catch (Exception e) {
                log.error("json转换异常", e);
                throw new FlowException("json转换异常");
            }
        }
        return new HashMap<>();
    }

    /**
     * 将字符串转为bean
     *
     * @param jsonStr json字符串
     * @param clazz   Class<T>
     * @return T
     */
    public static <T> T strToBean(String jsonStr, Class<T> clazz) {
        if (StrUtil.isNotEmpty(jsonStr)) {
            try {
                return JSON_MAPPER.readValue(jsonStr, clazz);
            } catch (Exception e) {
                log.error("json转换异常", e);
                throw new FlowException("json转换异常");
            }
        }
        return null;
    }

    /**
     * 将字符串转为集合
     *
     * @param jsonStr json字符串
     * @return List<T>
     */
    public static <T> List<T> strToList(String jsonStr) {
        if (StrUtil.isNotEmpty(jsonStr)) {
            try {
                return JSON_MAPPER.readValue(jsonStr, new TypeReference<List<T>>() {
                });
            } catch (Exception e) {
                log.error("json转换异常", e);
                throw new FlowException("json转换异常");
            }
        }
        return null;
    }

    /**
     * 将对象转为字符串
     *
     * @param variable object
     * @return json字符串
     */
    public static String objToStr(Object variable) {
        if (ObjectUtil.isNotNull(variable)) {
            try {
                return JSON_MAPPER.writeValueAsString(variable);
            } catch (Exception e) {
                log.error("Map转换异常", e);
                throw new FlowException("Map转换异常");
            }
        }
        return null;
    }
}
