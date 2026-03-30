package org.dromara.common.sensitive.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.json.enhance.JsonEnhancementContext;
import org.dromara.common.json.enhance.JsonFieldContext;
import org.dromara.common.json.enhance.JsonFieldProcessor;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveService;
import org.springframework.beans.BeansException;
import org.springframework.core.annotation.Order;

/**
 * 响应脱敏处理器。
 */
@Slf4j
@Order(100)
public class SensitiveJsonFieldProcessor implements JsonFieldProcessor {

    @Override
    public Object process(JsonFieldContext fieldContext, Object value, JsonEnhancementContext context) {
        Sensitive sensitive = fieldContext.getAnnotation(Sensitive.class);
        if (sensitive == null || !(value instanceof String text)) {
            return value;
        }
        try {
            SensitiveService sensitiveService = SpringUtils.getBean(SensitiveService.class);
            if (ObjectUtil.isNotNull(sensitiveService) && sensitiveService.isSensitive(sensitive.roleKey(), sensitive.perms())) {
                return sensitive.strategy().desensitizer().apply(text);
            }
            return text;
        } catch (BeansException e) {
            log.error("脱敏实现不存在, 采用默认处理 => {}", e.getMessage());
            return text;
        }
    }

}
