package com.ruoyi.common.jackson;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ruoyi.common.annotation.DictDataMapper;
import com.ruoyi.common.core.service.DictService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

import java.io.IOException;
import java.util.Objects;

/**
 * 字典数据json序列化工具
 *
 * @author itino
 */
@Slf4j
public class DictDataJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private String dictType;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            DictService dictService = SpringUtils.getBean(DictService.class);
            if (ObjectUtil.isNotNull(dictService)) {
                String label = dictService.getDictLabel(dictType, value);
                gen.writeString(StringUtils.isNotBlank(label) ? label : value);
            } else {
                gen.writeString(value);
            }
        } catch (BeansException e) {
            log.error("字典数据未查到, 采用默认处理 => {}", e.getMessage());
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        DictDataMapper anno = property.getAnnotation(DictDataMapper.class);
        if (Objects.nonNull(anno) && StrUtil.isNotBlank(anno.dictType())) {
            this.dictType = anno.dictType();
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
