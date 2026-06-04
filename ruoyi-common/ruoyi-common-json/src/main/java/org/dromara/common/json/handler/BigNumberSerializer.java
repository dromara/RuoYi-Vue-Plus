package org.dromara.common.json.handler;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.ser.jdk.NumberSerializer;

/**
 * 超出 JS 最大最小值 处理
 *
 * @author Lion Li
 */
@JacksonStdImpl
public class BigNumberSerializer extends NumberSerializer {

    /**
     * 提供实例
     */
    public static final BigNumberSerializer INSTANCE = new BigNumberSerializer(Number.class);
    /**
     * 根据 JS Number.MAX_SAFE_INTEGER 与 Number.MIN_SAFE_INTEGER 得来
     */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    /**
     * JavaScript 最小安全整数。
     */
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    /**
     * 构造大数字序列化器。
     *
     * @param rawType 数字类型
     */
    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    /**
     * 序列化数字，超出 JS 安全整数范围时输出字符串。
     *
     * @param value    数字值
     * @param gen      JSON 生成器
     * @param provider 序列化上下文
     */
    @Override
    public void serialize(Number value, JsonGenerator gen, SerializationContext provider) {
        // 超出范围 序列化为字符串
        if (value.longValue() >= MIN_SAFE_INTEGER && value.longValue() <= MAX_SAFE_INTEGER) {
            super.serialize(value, gen, provider);
        } else {
            gen.writeString(value.toString());
        }
    }
}
