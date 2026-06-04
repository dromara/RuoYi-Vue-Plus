package org.dromara.common.json.enhance;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Array;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一响应增强器，支持在出站前执行翻译、脱敏等字段处理。
 */
public class JsonValueEnhancer {

    /**
     * JSON 映射器。
     */
    private final JsonMapper jsonMapper;

    /**
     * 字段增强处理器列表。
     */
    private final List<JsonFieldProcessor> processors;

    /**
     * 类型属性元数据缓存。
     */
    private final Map<Class<?>, List<PropertyMetadata>> propertyCache = new ConcurrentHashMap<>();

    /**
     * 构造统一响应增强器。
     *
     * @param jsonMapper JSON 映射器
     * @param processors 字段处理器列表
     */
    public JsonValueEnhancer(JsonMapper jsonMapper, List<JsonFieldProcessor> processors) {
        this.jsonMapper = jsonMapper;
        List<JsonFieldProcessor> sortedProcessors = new ArrayList<>(processors);
        AnnotationAwareOrderComparator.sort(sortedProcessors);
        this.processors = Collections.unmodifiableList(sortedProcessors);
    }

    /**
     * 增强响应对象。
     *
     * @param body 响应对象
     * @return 增强后的响应对象
     */
    public Object enhance(Object body) {
        if (body == null || body instanceof JsonNode || processors.isEmpty()) {
            return body;
        }
        JsonEnhancementContext context = new JsonEnhancementContext(jsonMapper);
        collectValue(body, context, new IdentityHashMap<>());
        if (!context.isProcessingRequired()) {
            return body;
        }
        processors.forEach(processor -> processor.prepare(context));
        return renderValue(body, context, new IdentityHashMap<>());
    }

    /**
     * 判断消息转换器是否支持响应增强。
     *
     * @param converterType 消息转换器类型
     * @return true 支持 false 不支持
     */
    public boolean supports(Class<?> converterType) {
        return !processors.isEmpty()
            && !ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)
            && !StringHttpMessageConverter.class.isAssignableFrom(converterType)
            && !ResourceHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * 对已处理后的对象再次执行树形增强。
     *
     * @param value 待增强对象
     * @return 增强后的 JSON 节点
     */
    private JsonNode enhanceTree(Object value) {
        JsonEnhancementContext context = new JsonEnhancementContext(jsonMapper);
        collectValue(value, context, new IdentityHashMap<>());
        if (!context.isProcessingRequired()) {
            return jsonMapper.valueToTree(value);
        }
        processors.forEach(processor -> processor.prepare(context));
        return renderValue(value, context, new IdentityHashMap<>());
    }

    /**
     * 递归收集对象中需要增强的字段信息。
     *
     * @param value   当前对象
     * @param context 增强上下文
     * @param visited 已访问对象集合，用于避免循环引用
     */
    private void collectValue(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(child -> collectValue(child, context, visited));
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(child -> collectValue(child, context, visited));
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collectValue(Array.get(value, i), context, visited);
            }
            return;
        }
        if (isSimpleValue(value.getClass()) || visited.put(value, Boolean.TRUE) != null) {
            return;
        }
        try {
            for (PropertyMetadata metadata : getProperties(value.getClass())) {
                Object propertyValue = metadata.getValue(value);
                JsonFieldContext fieldContext = new JsonFieldContext(value, metadata.propertyName(), metadata.member(), propertyValue);
                collectField(fieldContext, context);
                collectValue(propertyValue, context, visited);
            }
        } finally {
            visited.remove(value);
        }
    }

    /**
     * 收集单个字段的增强信息。
     *
     * @param fieldContext 字段上下文
     * @param context      增强上下文
     */
    private void collectField(JsonFieldContext fieldContext, JsonEnhancementContext context) {
        for (JsonFieldProcessor processor : processors) {
            if (processor.supports(fieldContext)) {
                context.markProcessingRequired();
                processor.collect(fieldContext, context);
            }
        }
    }

    /**
     * 将对象渲染为增强后的 JSON 节点。
     *
     * @param value   当前对象
     * @param context 增强上下文
     * @param visited 已访问对象集合，用于避免循环引用
     * @return JSON 节点
     */
    private JsonNode renderValue(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        switch (value) {
            case null -> {
                return jsonMapper.nullNode();
            }
            case JsonNode jsonNode -> {
                return jsonNode;
            }
            case Map<?, ?> map -> {
                return renderMap(map, context, visited);
            }
            case Iterable<?> iterable -> {
                return renderIterable(iterable, context, visited);
            }
            default -> {
            }
        }
        if (value.getClass().isArray()) {
            return renderArray(value, context, visited);
        }
        if (isSimpleValue(value.getClass())) {
            return jsonMapper.valueToTree(value);
        }
        if (visited.put(value, Boolean.TRUE) != null) {
            return jsonMapper.valueToTree(value);
        }
        try {
            return renderPojo(value, context, visited);
        } finally {
            visited.remove(value);
        }
    }

    /**
     * 渲染 Map 对象。
     *
     * @param map     Map 对象
     * @param context 增强上下文
     * @param visited 已访问对象集合
     * @return 对象节点
     */
    private ObjectNode renderMap(Map<?, ?> map, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        ObjectNode objectNode = jsonMapper.createObjectNode();
        map.forEach((key, childValue) -> objectNode.set(String.valueOf(key), renderValue(childValue, context, visited)));
        return objectNode;
    }

    /**
     * 渲染可迭代对象。
     *
     * @param iterable 可迭代对象
     * @param context  增强上下文
     * @param visited  已访问对象集合
     * @return 数组节点
     */
    private ArrayNode renderIterable(Iterable<?> iterable, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        ArrayNode arrayNode = jsonMapper.createArrayNode();
        for (Object child : iterable) {
            arrayNode.add(renderValue(child, context, visited));
        }
        return arrayNode;
    }

    /**
     * 渲染数组对象。
     *
     * @param value   数组对象
     * @param context 增强上下文
     * @param visited 已访问对象集合
     * @return 数组节点
     */
    private ArrayNode renderArray(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        ArrayNode arrayNode = jsonMapper.createArrayNode();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            arrayNode.add(renderValue(Array.get(value, i), context, visited));
        }
        return arrayNode;
    }

    /**
     * 渲染普通 Java 对象。
     *
     * @param value   Java 对象
     * @param context 增强上下文
     * @param visited 已访问对象集合
     * @return 对象节点
     */
    private ObjectNode renderPojo(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        ObjectNode objectNode = jsonMapper.createObjectNode();
        for (PropertyMetadata metadata : getProperties(value.getClass())) {
            Object originalValue = metadata.getValue(value);
            JsonFieldContext fieldContext = new JsonFieldContext(value, metadata.propertyName(), metadata.member(), originalValue);
            Object processedValue = originalValue;
            boolean changed = false;
            for (JsonFieldProcessor processor : processors) {
                if (!processor.supports(fieldContext)) {
                    continue;
                }
                Object nextValue = processor.process(fieldContext, processedValue, context);
                changed = changed || !Objects.equals(processedValue, nextValue);
                processedValue = nextValue;
            }
            JsonNode childNode = changed
                ? enhanceTranslatedValue(processedValue, context, visited)
                : renderValue(processedValue, context, visited);
            objectNode.set(metadata.propertyName(), childNode);
        }
        return objectNode;
    }

    /**
     * 对字段处理后得到的复杂对象执行二次增强。
     *
     * @param value   字段处理后的值
     * @param context 增强上下文
     * @param visited 已访问对象集合
     * @return JSON 节点
     */
    private JsonNode enhanceTranslatedValue(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || value instanceof JsonNode || isSimpleValue(value.getClass())) {
            return renderValue(value, context, visited);
        }
        return enhanceTree(value);
    }

    /**
     * 获取指定类型可序列化属性元数据。
     *
     * @param type 类型
     * @return 属性元数据列表
     */
    private List<PropertyMetadata> getProperties(Class<?> type) {
        return propertyCache.computeIfAbsent(type, this::resolveProperties);
    }

    /**
     * 解析指定类型可序列化属性元数据。
     *
     * @param type 类型
     * @return 属性元数据列表
     */
    private List<PropertyMetadata> resolveProperties(Class<?> type) {
        if (isSimpleValue(type) || type.isArray() || Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type)) {
            return Collections.emptyList();
        }
        JavaType javaType = jsonMapper.constructType(type);
        SerializationConfig config = jsonMapper.serializationConfig();
        ClassIntrospector classIntrospector = config.classIntrospectorInstance().forOperation(config);
        AnnotatedClass annotatedClass = classIntrospector.introspectClassAnnotations(javaType);
        List<BeanPropertyDefinition> definitions = classIntrospector.introspectForSerialization(javaType, annotatedClass).findProperties();
        List<PropertyMetadata> properties = new ArrayList<>(definitions.size());
        for (BeanPropertyDefinition definition : definitions) {
            AnnotatedMember member = definition.getAccessor();
            if (member == null) {
                member = definition.getField();
            }
            if (member == null) {
                continue;
            }
            member.fixAccess(true);
            properties.add(new PropertyMetadata(definition.getName(), member));
        }
        return Collections.unmodifiableList(properties);
    }

    /**
     * 判断类型是否为简单值类型。
     *
     * @param type 类型
     * @return true 简单值 false 复杂对象
     */
    private boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
            || CharSequence.class.isAssignableFrom(type)
            || Number.class.isAssignableFrom(type)
            || Boolean.class == type
            || Character.class == type
            || Date.class.isAssignableFrom(type)
            || Temporal.class.isAssignableFrom(type)
            || Enum.class.isAssignableFrom(type)
            || UUID.class.isAssignableFrom(type)
            || Class.class == type;
    }

    /**
     * JSON 属性元数据。
     *
     * @param propertyName 属性名称
     * @param member       Jackson 属性成员
     */
    private record PropertyMetadata(String propertyName, AnnotatedMember member) {

        /**
         * 从源对象读取属性值。
         *
         * @param source 源对象
         * @return 属性值
         */
        Object getValue(Object source) {
            return member.getValue(source);
        }

    }

}
