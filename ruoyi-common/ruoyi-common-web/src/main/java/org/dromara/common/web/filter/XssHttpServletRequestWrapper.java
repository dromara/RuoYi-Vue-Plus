package org.dromara.common.web.filter;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 请求包装器，统一清洗参数与 JSON 请求体中的 HTML 标签内容。
 *
 * @author ruoyi
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 使用原始请求构造 XSS 包装器。
     *
     * @param request 原始请求
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 获取并清洗单个请求参数。
     *
     * @param name 参数名
     * @return 清洗后的参数值
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (value == null) {
            return null;
        }
        return HtmlUtil.cleanHtmlTag(value).trim();
    }

    /**
     * 获取并清洗整组请求参数。
     *
     * @return 清洗后的参数映射
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> valueMap = super.getParameterMap();
        if (MapUtil.isEmpty(valueMap)) {
            return valueMap;
        }
        // 避免某些容器不允许改参数的情况 copy一份重新改
        Map<String, String[]> map = new HashMap<>(valueMap.size());
        map.putAll(valueMap);
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String[] values = entry.getValue();
            if (values != null) {
                int length = values.length;
                String[] escapseValues = new String[length];
                for (int i = 0; i < length; i++) {
                    // 防xss攻击和过滤前后空格
                    escapseValues[i] = HtmlUtil.cleanHtmlTag(values[i]).trim();
                }
                map.put(entry.getKey(), escapseValues);
            }
        }
        return map;
    }

    /**
     * 获取并清洗指定参数的多值数组。
     *
     * @param name 参数名
     * @return 清洗后的参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (ArrayUtil.isEmpty(values)) {
            return values;
        }
        int length = values.length;
        String[] escapseValues = new String[length];
        for (int i = 0; i < length; i++) {
            // 防xss攻击和过滤前后空格
            escapseValues[i] = HtmlUtil.cleanHtmlTag(values[i]).trim();
        }
        return escapseValues;
    }

    /**
     * 获取输入流并在 JSON 场景下对请求体执行清洗。
     *
     * @return 清洗后的输入流
     * @throws IOException 读取请求体异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        // 非json类型，直接返回
        if (!isJsonRequest()) {
            return super.getInputStream();
        }

        // 为空，直接返回
        String json = StrUtil.str(IoUtil.readBytes(super.getInputStream(), false), StandardCharsets.UTF_8);
        if (StringUtils.isEmpty(json)) {
            return super.getInputStream();
        }

        // xss过滤
        json = HtmlUtil.cleanHtmlTag(json).trim();
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        final ByteArrayInputStream bis = IoUtil.toStream(jsonBytes);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public int available() throws IOException {
                return jsonBytes.length;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                return bis.read();
            }
        };
    }

    /**
     * 判断当前请求是否为 JSON 请求。
     *
     * @return true 表示 JSON 请求
     */
    public boolean isJsonRequest() {
        String header = super.getHeader(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }
}
