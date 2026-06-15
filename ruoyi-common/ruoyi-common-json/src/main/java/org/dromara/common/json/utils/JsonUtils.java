package org.dromara.common.json.utils;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 工具类
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtils {

    /**
     * 全局 JSON 映射器。
     */
    private static final JsonMapper JSON_MAPPER = SpringUtils.getBean(JsonMapper.class);

    /**
     * 获取全局 JsonMapper 实例。
     *
     * @return JsonMapper
     */
    public static JsonMapper getJsonMapper() {
        return JSON_MAPPER;
    }

    /**
     * 将对象转换为JSON格式的字符串
     *
     * @param object 要转换的对象
     * @return JSON格式的字符串，如果对象为null，则返回null
     * @throws RuntimeException 如果转换过程中发生JSON处理异常，则抛出运行时异常
     */
    public static String toJsonString(Object object) {
        if (ObjectUtil.isNull(object)) {
            return null;
        }
        return JSON_MAPPER.writeValueAsString(object);
    }

    /**
     * 将JSON格式的字符串转换为指定类型的对象
     *
     * @param text  JSON格式的字符串
     * @param clazz 要转换的目标对象类型
     * @param <T>   目标对象的泛型类型
     * @return 转换后的对象，如果字符串为空则返回null
     * @throws RuntimeException 如果转换过程中发生IO异常，则抛出运行时异常
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        return JSON_MAPPER.readValue(text, clazz);
    }

    /**
     * 将字节数组转换为指定类型的对象
     *
     * @param bytes 字节数组
     * @param clazz 要转换的目标对象类型
     * @param <T>   目标对象的泛型类型
     * @return 转换后的对象，如果字节数组为空则返回null
     * @throws RuntimeException 如果转换过程中发生IO异常，则抛出运行时异常
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        return JSON_MAPPER.readValue(bytes, clazz);
    }

    /**
     * 将JSON格式的字符串转换为指定类型的对象，支持复杂类型
     *
     * @param text          JSON格式的字符串
     * @param typeReference 指定类型的TypeReference对象
     * @param <T>           目标对象的泛型类型
     * @return 转换后的对象，如果字符串为空则返回null
     * @throws RuntimeException 如果转换过程中发生IO异常，则抛出运行时异常
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return JSON_MAPPER.readValue(text, typeReference);
    }

    /**
     * 将JSON格式的字符串转换为Dict对象
     *
     * @param text JSON格式的字符串
     * @return 转换后的Dict对象，如果字符串为空或者不是JSON格式则返回null
     * @throws RuntimeException 如果转换过程中发生IO异常，则抛出运行时异常
     */
    public static Dict parseMap(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return JSON_MAPPER.readValue(text, JSON_MAPPER.getTypeFactory().constructType(Dict.class));
    }

    /**
     * 将JSON格式的字符串转换为Dict对象的列表
     *
     * @param text JSON格式的字符串
     * @return 转换后的Dict对象的列表，如果字符串为空则返回null
     * @throws RuntimeException 如果转换过程中发生IO异常，则抛出运行时异常
     */
    public static List<Dict> parseArrayMap(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return JSON_MAPPER.readValue(text, JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, Dict.class));
    }

    /**
     * 将JSON格式的字符串转换为指定类型对象的列表
     *
     * @param text  JSON格式的字符串
     * @param clazz 要转换的目标对象类型
     * @param <T>   目标对象的泛型类型
     * @return 转换后的对象的列表，如果字符串为空则返回空列表
     * @throws RuntimeException 如果转换过程中发生IO异常，则抛出运行时异常
     */
    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StringUtils.isEmpty(text)) {
            return new ArrayList<>();
        }
        return JSON_MAPPER.readValue(text, JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    /**
     * 将对象转换为 JSON 字符串，并递归移除指定字段。
     *
     * @param object     要转换的对象
     * @param fieldNames 需要移除的字段名
     * @return 移除字段后的 JSON 字符串
     */
    public static String toJsonStringExcludeFields(Object object, String... fieldNames) {
        if (ObjectUtil.isNull(object)) {
            return null;
        }
        JsonNode node = JSON_MAPPER.valueToTree(object);
        removeFields(node, fieldNames);
        return toJsonString(node);
    }

    /**
     * 从 JSON 树中递归移除指定字段。
     *
     * @param node       JSON 节点
     * @param fieldNames 需要移除的字段名
     * @return 原 JSON 节点
     */
    public static JsonNode removeFields(JsonNode node, String... fieldNames) {
        if (node == null || ArrayUtil.isEmpty(fieldNames)) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            for (String fieldName : fieldNames) {
                if (StringUtils.isNotBlank(fieldName)) {
                    objectNode.remove(fieldName);
                }
            }
        }
        for (JsonNode child : node) {
            removeFields(child, fieldNames);
        }
        return node;
    }

    /**
     * 判断字符串是否为合法 JSON（对象或数组）
     *
     * @param str 待校验字符串
     * @return true = 合法 JSON，false = 非法或空
     */
    public static boolean isJson(String str) {
        return readTreeQuietly(str) != null;
    }

    /**
     * 判断字符串是否为 JSON 对象（{}）
     *
     * @param str 待校验字符串
     * @return true = JSON 对象
     */
    public static boolean isJsonObject(String str) {
        JsonNode node = readTreeQuietly(str);
        return node != null && node.isObject();
    }

    /**
     * 判断字符串是否为 JSON 数组（[]）
     *
     * @param str 待校验字符串
     * @return true = JSON 数组
     */
    public static boolean isJsonArray(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        JsonNode node = readTreeQuietly(str);
        return node != null && node.isArray();
    }

    /**
     * 安静读取 JSON 树，解析失败时返回 null。
     *
     * @param str JSON 字符串
     * @return JSON 节点，解析失败或空字符串时返回 null
     */
    private static JsonNode readTreeQuietly(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        try {
            return JSON_MAPPER.readTree(str);
        } catch (Exception e) {
            return null;
        }
    }

}
