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

    /**
     * 创建绑定指定翻译注解的序列化处理器。
     *
     * @param translation 当前字段上声明的翻译注解
     */
    public TranslationHandler(Translation translation) {
        this.translation = translation;
    }

    /**
     * 将原始字段值翻译为展示值并写回序列化结果。
     *
     * @param value 原始字段值
     * @param gen   Json 输出器
     * @param ctxt  序列化上下文
     * @throws JacksonException Json 序列化异常
     */
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

    /**
     * 按字段上的 {@link Translation} 注解创建上下文相关的翻译序列化器。
     *
     * @param ctxt     序列化上下文
     * @param property 当前序列化属性
     * @return 存在翻译注解时返回新的翻译处理器，否则沿用默认序列化器
     */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        Translation translation = property.getAnnotation(Translation.class);
        if (Objects.nonNull(translation)) {
            return new TranslationHandler(translation);
        }
        return super.createContextual(ctxt, property);
    }
}
