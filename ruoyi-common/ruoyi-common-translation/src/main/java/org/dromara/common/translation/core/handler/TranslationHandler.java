package org.dromara.common.translation.core.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.core.TranslationInterface;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻译处理器
 *
 * @author Lion Li
 */
@Slf4j
public class TranslationHandler extends ValueSerializer<Object> {

    /**
     * 全局翻译实现类映射器
     */
    public static final Map<String, TranslationInterface<?>> TRANSLATION_MAPPER = new ConcurrentHashMap<>();

    private final Translation translation;

    /**
     * 提供给 jackson 创建上下文序列化器时使用 不然会报错
     */
    public TranslationHandler() {
        this.translation = null;
    }

    public TranslationHandler(Translation translation) {
        this.translation = translation;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        TranslationInterface<?> trans = TRANSLATION_MAPPER.get(translation.type());
        if (ObjectUtil.isNotNull(trans)) {
            // 如果映射字段不为空 则取映射字段的值
            if (StringUtils.isNotBlank(translation.mapper())) {
                value = ReflectUtils.invokeGetter(gen.currentValue(), translation.mapper());
            }
            // 如果为 null 直接写出
            if (ObjectUtil.isNull(value)) {
                gen.writeNull();
                return;
            }
            try {
                Object result = trans.translation(value, translation.other());
                gen.writePOJO(result);
            } catch (Exception e) {
                log.error("翻译处理异常，type: {}, value: {}", translation.type(), value, e);
                // 出现异常时输出原始值而不是中断序列化
                gen.writePOJO(value);
            }
        } else {
            gen.writePOJO(value);
        }
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        Translation translation = property.getAnnotation(Translation.class);
        if (Objects.nonNull(translation)) {
            return new TranslationHandler(translation);
        }
        return super.createContextual(ctxt, property);
    }
}
