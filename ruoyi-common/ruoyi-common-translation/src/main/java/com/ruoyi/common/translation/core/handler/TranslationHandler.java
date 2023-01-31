package com.ruoyi.common.translation.core.handler;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.translation.annotation.Translation;
import com.ruoyi.common.translation.core.TranslationInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻译处理器
 *
 * @author Lion Li
 */
@Slf4j
public class TranslationHandler extends JsonSerializer<Object> implements ContextualSerializer {

    /**
     * 全局翻译实现类映射器
     */
    public static final Map<String, TranslationInterface> TRANSLATION_MAPPER = new ConcurrentHashMap<>();

    private Translation translation;

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            TranslationInterface trans = TRANSLATION_MAPPER.get(translation.type());
            if (ObjectUtil.isNotNull(trans)) {
                String result = trans.translation(value, translation.other());
                gen.writeString(StringUtils.isNotBlank(result) ? result : value.toString());
            } else {
                gen.writeString(value.toString());
            }
        } catch (BeansException e) {
            log.error("数据未查到, 采用默认处理 => {}", e.getMessage());
            gen.writeString(value.toString());
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        Translation translation = property.getAnnotation(Translation.class);
        if (Objects.nonNull(translation)) {
            this.translation = translation;
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
