package org.dromara.common.web.filter;

import cn.hutool.core.io.IoUtil;
import org.dromara.common.core.constant.Constants;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 构建可重复读取输入流的请求包装器，缓存请求体以支持多次读取。
 *
 * @author ruoyi
 */
public class RepeatedlyRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] body;

    /**
     * 读取原始请求体并缓存到内存，统一设置请求与响应编码。
     *
     * @param request 原始请求
     * @param response 当前响应
     * @throws IOException 读取请求体异常
     */
    public RepeatedlyRequestWrapper(HttpServletRequest request, ServletResponse response) throws IOException {
        super(request);
        request.setCharacterEncoding(Constants.UTF8);
        response.setCharacterEncoding(Constants.UTF8);

        body = IoUtil.readBytes(request.getInputStream(), false);
    }

    /**
     * 基于缓存的请求体构造字符读取器。
     *
     * @return 可重复读取的字符流
     * @throws IOException IO 异常
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    /**
     * 返回基于缓存请求体重新生成的输入流。
     *
     * @return 可重复读取的输入流
     * @throws IOException IO 异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public int available() throws IOException {
                return body.length;
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
