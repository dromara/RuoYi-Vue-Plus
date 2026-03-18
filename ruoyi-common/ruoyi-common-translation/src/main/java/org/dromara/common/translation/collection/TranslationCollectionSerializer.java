package org.dromara.common.translation.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.reflect.AnnotationUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集合翻译序列化器
 *
 * @author 秋辞未寒
 */
@Slf4j
public class TranslationCollectionSerializer extends ValueSerializer<TranslationCollectionWrapper<?>> {

    /**
     * 全局翻译实现类映射器
     */
    public static final Map<String, TranslationCollectionProcessor<?,?>> TRANSLATION_MAPPER = new ConcurrentHashMap<>();

    @Override
    public void serialize(TranslationCollectionWrapper<?> value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        if (CollUtil.isEmpty(value)) {
            return;
        }
        Class<?> elementType = value.getElementType();
        if (elementType == null) {
            elementType = ClassUtil.getClass(CollUtil.getFirst(value));
        }
        // 获取包装原内容（很重要！！！！！！如果原样使用的话，会无限递归！）
        Collection<?> collection = value.getCollection();
        TranslationCollection annotation = AnnotationUtils.getAnnotation(elementType, TranslationCollection.class);
        if (annotation != null ) {
            String type = annotation.type();
            String other = annotation.other();
            TranslationCollectionProcessor processor = TRANSLATION_MAPPER.get(type);
            if (processor != null) {
                try {
                    collection = processor.process(collection,other);
                } catch (Exception e) {
                    log.error("翻译处理异常，type: {}, value: {}", type, value, e);
                    // 出现异常时输出原始值而不是中断序列化
                    collection = value.getCollection();
                }
            }
        }
        gen.writeStartArray();
        for (Object o : collection) {
            gen.writePOJO(o);
        }
        gen.writeEndArray();
    }

}
