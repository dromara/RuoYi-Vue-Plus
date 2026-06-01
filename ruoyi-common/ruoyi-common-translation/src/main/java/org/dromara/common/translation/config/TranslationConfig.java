package org.dromara.common.translation.config;

import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.common.translation.core.handler.TranslationJsonFieldProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 翻译模块配置类
 *
 * @author Lion Li
 */
@AutoConfiguration
public class TranslationConfig {

    /**
     * 创建翻译 JSON 字段处理器。
     *
     * @param list 翻译实现集合
     * @return 翻译 JSON 字段处理器
     */
    @Bean
    public TranslationJsonFieldProcessor translationJsonFieldProcessor(List<TranslationInterface<?>> list) {
        return new TranslationJsonFieldProcessor(list);
    }

}
