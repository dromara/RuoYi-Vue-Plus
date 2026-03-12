package org.dromara.common.sensitive.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveService;
import org.dromara.common.sensitive.core.SensitiveStrategy;
import org.springframework.beans.BeansException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Objects;

/**
 * 数据脱敏json序列化工具
 *
 * @author Yjoioooo
 */
@Slf4j
public class SensitiveHandler extends ValueSerializer<String> {

    private final SensitiveStrategy strategy;
    private final String[] roleKey;
    private final String[] perms;

    /**
     * 提供给 jackson 创建上下文序列化器时使用 不然会报错
     */
    public SensitiveHandler() {
        this.strategy = null;
        this.roleKey = null;
        this.perms = null;
    }

    public SensitiveHandler(SensitiveStrategy strategy, String[] strings, String[] perms) {
        this.strategy = strategy;
        this.roleKey = strings;
        this.perms = perms;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        try {
            SensitiveService sensitiveService = SpringUtils.getBean(SensitiveService.class);
            if (ObjectUtil.isNotNull(sensitiveService) && sensitiveService.isSensitive(roleKey, perms)) {
                gen.writeString(strategy.desensitizer().apply(value));
            } else {
                gen.writeString(value);
            }
        } catch (BeansException e) {
            log.error("脱敏实现不存在, 采用默认处理 => {}", e.getMessage());
            gen.writeString(value);
        }
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass())) {
            return new SensitiveHandler(annotation.strategy(), annotation.roleKey(), annotation.perms());
        }
        return super.createContextual(ctxt, property);
    }
}
