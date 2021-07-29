package com.ruoyi.framework.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;

import java.io.IOException;

@JacksonStdImpl
public class BigNumberSerializer extends NumberSerializer {

	private static final long JS_NUM_MAX = 9007199254740992L;
	private static final long JS_NUM_MIN = -9007199254740992L;
	public static final BigNumberSerializer INSTANCE = new BigNumberSerializer(Number.class);

	public BigNumberSerializer(Class<? extends Number> rawType) {
		super(rawType);
	}

	@Override
	public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value.longValue() >= JS_NUM_MIN && value.longValue() <= JS_NUM_MAX) {
			super.serialize(value, gen, provider);
		} else {
			gen.writeString(value.toString());
		}
	}
}
