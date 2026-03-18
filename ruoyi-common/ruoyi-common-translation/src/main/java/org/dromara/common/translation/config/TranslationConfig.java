package org.dromara.common.translation.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.collection.TranslationCollectionProcessor;
import org.dromara.common.translation.collection.TranslationCollectionSerializer;
import org.dromara.common.translation.collection.TranslationCollectionType;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.common.translation.core.handler.TranslationBeanSerializerModifier;
import org.dromara.common.translation.core.handler.TranslationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ser.SerializerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 翻译模块配置类
 *
 * @author Lion Li
 */
@Slf4j
@AutoConfiguration
public class TranslationConfig {

    @Autowired
    private List<TranslationInterface<?>> translationInterfaces;

    @Autowired
    private List<TranslationCollectionProcessor<?,?>> translationCollectionHandlers;

    @PostConstruct
    public void init() {
        Map<String, TranslationInterface<?>> map = new HashMap<>(translationInterfaces.size());
        for (TranslationInterface<?> trans : translationInterfaces) {
            if (trans.getClass().isAnnotationPresent(TranslationType.class)) {
                TranslationType annotation = trans.getClass().getAnnotation(TranslationType.class);
                map.put(annotation.type(), trans);
            } else {
                log.warn(trans.getClass().getName() + " 翻译实现类未标注 TranslationType 注解!");
            }
        }
        TranslationHandler.TRANSLATION_MAPPER.putAll(map);

        Map<String, TranslationCollectionProcessor<Object,Object>> handlerMaps = new HashMap<>(translationCollectionHandlers.size());
        for (TranslationCollectionProcessor<?,?> trans : translationCollectionHandlers) {
            if (trans.getClass().isAnnotationPresent(TranslationCollectionType.class)) {
                TranslationCollectionType annotation = trans.getClass().getAnnotation(TranslationCollectionType.class);
                handlerMaps.put(annotation.type(), (TranslationCollectionProcessor<Object, Object>) trans);
            } else {
                log.warn(trans.getClass().getName() + " 翻译实现类未标注 TranslationCollectionType 注解!");
            }
        }
        TranslationCollectionSerializer.TRANSLATION_MAPPER.putAll(handlerMaps);
    }

    @Bean
    public JsonMapperBuilderCustomizer translationInitCustomizer() {
        return builder -> {
            SerializerFactory serializerFactory = builder.serializerFactory();
            serializerFactory = serializerFactory.withSerializerModifier(new TranslationBeanSerializerModifier());
            builder.serializerFactory(serializerFactory);
        };
    }

}
