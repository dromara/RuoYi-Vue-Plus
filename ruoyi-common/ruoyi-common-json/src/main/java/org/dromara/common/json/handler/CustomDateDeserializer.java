package org.dromara.common.json.handler;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.dromara.common.core.utils.ObjectUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Date;

/**
 * 自定义 Date 类型反序列化处理器（支持多种格式）
 *
 * @author AprilWind
 */
public class CustomDateDeserializer extends ValueDeserializer<Date> {

    /**
     * 反序列化逻辑：将字符串转换为 Date 对象
     *
     * @param p    JSON 解析器，用于获取字符串值
     * @param ctxt 上下文环境（可用于获取更多配置）
     * @return 转换后的 Date 对象，若为空字符串返回 null
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) {
        DateTime parse = DateUtil.parse(p.getString());
        if (ObjectUtils.isNull(parse)) {
            return null;
        }
        return parse.toJdkDate();
    }

}
