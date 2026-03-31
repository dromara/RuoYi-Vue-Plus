package org.dromara.common.json.enhance;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
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

    private final JsonMapper jsonMapper;

    private final List<JsonFieldProcessor> processors;

    private final Map<Class<?>, List<PropertyMetadata>> propertyCache = new ConcurrentHashMap<>();

    public JsonValueEnhancer(JsonMapper jsonMapper, List<JsonFieldProcessor> processors) {
        this.jsonMapper = jsonMapper;
        List<JsonFieldProcessor> sortedProcessors = new ArrayList<>(processors);
        AnnotationAwareOrderComparator.sort(sortedProcessors);
        this.processors = Collections.unmodifiableList(sortedProcessors);
    }

    public Object enhance(Object body) {
        if (body == null || body instanceof JsonNode || processors.isEmpty()) {
            return body;
        }
        return enhanceTree(body);
    }

    public boolean supports(Class<?> converterType) {
        return !processors.isEmpty()
            && !StringHttpMessageConverter.class.isAssignableFrom(converterType)
            && !ResourceHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    private JsonNode enhanceTree(Object value) {
        JsonEnhancementContext context = new JsonEnhancementContext(jsonMapper);
        collectValue(value, context, new IdentityHashMap<>());
        processors.forEach(processor -> processor.prepare(context));
        return renderValue(value, context, new IdentityHashMap<>());
    }

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
                processors.forEach(processor -> processor.collect(fieldContext, context));
                collectValue(propertyValue, context, visited);
            }
        } finally {
            visited.remove(value);
        }
    }

    private JsonNode renderValue(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        switch (value) {
            case null -> {
                return jsonMapper.nullNode();
            }
            case JsonNode jsonNode -> {
                return jsonNode;
            }
            case Map<?, ?> map -> {
                ObjectNode objectNode = jsonMapper.createObjectNode();
                map.forEach((key, childValue) -> objectNode.set(String.valueOf(key), renderValue(childValue, context, visited)));
                return objectNode;
            }
            case Iterable<?> iterable -> {
                ArrayNode arrayNode = jsonMapper.createArrayNode();
                for (Object child : iterable) {
                    arrayNode.add(renderValue(child, context, visited));
                }
                return arrayNode;
            }
            default -> {
            }
        }
        if (value.getClass().isArray()) {
            ArrayNode arrayNode = jsonMapper.createArrayNode();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                arrayNode.add(renderValue(Array.get(value, i), context, visited));
            }
            return arrayNode;
        }
        if (isSimpleValue(value.getClass())) {
            return jsonMapper.valueToTree(value);
        }
        if (visited.put(value, Boolean.TRUE) != null) {
            return jsonMapper.valueToTree(value);
        }
        try {
            ObjectNode objectNode = jsonMapper.createObjectNode();
            for (PropertyMetadata metadata : getProperties(value.getClass())) {
                Object originalValue = metadata.getValue(value);
                JsonFieldContext fieldContext = new JsonFieldContext(value, metadata.propertyName(), metadata.member(), originalValue);
                Object processedValue = originalValue;
                boolean changed = false;
                for (JsonFieldProcessor processor : processors) {
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
        } finally {
            visited.remove(value);
        }
    }

    private JsonNode enhanceTranslatedValue(Object value, JsonEnhancementContext context, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || value instanceof JsonNode || isSimpleValue(value.getClass())) {
            return renderValue(value, context, visited);
        }
        return enhanceTree(value);
    }

    private List<PropertyMetadata> getProperties(Class<?> type) {
        return propertyCache.computeIfAbsent(type, this::resolveProperties);
    }

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

    private record PropertyMetadata(String propertyName, AnnotatedMember member) {

        Object getValue(Object source) {
            return member.getValue(source);
        }

    }

}
