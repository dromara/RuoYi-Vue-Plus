package org.dromara.common.translation.core.handler;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

/**
 * Bean 序列化修改器 解决 Null 被单独处理问题
 *
 * @author Lion Li
 */
public class TranslationBeanSerializerModifier extends ValueSerializerModifier {

    /**
     * 为翻译字段补充空值序列化器，确保字段值为 {@code null} 时仍能走翻译处理链。
     *
     * @param config         当前序列化配置
     * @param beanDesc       Bean 描述提供者
     * @param beanProperties 当前 Bean 的属性写入器列表
     * @return 调整后的属性写入器列表
     */
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter writer : beanProperties) {
            // 如果序列化器为 TranslationHandler 的话 将 Null 值也交给他处理
            if (writer.getSerializer() instanceof TranslationHandler serializer) {
                writer.assignNullSerializer(serializer);
            }
        }
        return super.changeProperties(config, beanDesc, beanProperties);
    }

}
