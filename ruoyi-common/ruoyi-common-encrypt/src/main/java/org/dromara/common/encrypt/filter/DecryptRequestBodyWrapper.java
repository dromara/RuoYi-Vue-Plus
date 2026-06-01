package org.dromara.common.encrypt.filter;

import cn.hutool.core.io.IoUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 解密请求参数工具类
 *
 * @author wdhcr
 */
public class DecryptRequestBodyWrapper extends HttpServletRequestWrapper {

    /**
     * 请求体字节数据。
     */
    private final byte[] body;

    /**
     * 解密请求体并缓存为可重复读取的 JSON 请求体。
     *
     * @param request 原始请求
     * @param privateKey RSA 私钥
     * @param headerFlag 加密密钥请求头
     * @throws IOException 读取请求体异常
     */
    public DecryptRequestBodyWrapper(HttpServletRequest request, String privateKey, String headerFlag) throws IOException {
        super(request);
        // 获取 AES 密码 采用 RSA 加密
        String headerRsa = request.getHeader(headerFlag);
        String decryptAes = EncryptUtils.decryptByRsa(headerRsa, privateKey);
        // 解密 AES 密码
        String aesPassword = EncryptUtils.decryptByBase64(decryptAes);
        request.setCharacterEncoding(Constants.UTF8);
        byte[] readBytes = IoUtil.readBytes(request.getInputStream(), false);
        String requestBody = new String(readBytes, StandardCharsets.UTF_8);
        // 解密 body 采用 AES 加密
        String decryptBody = EncryptUtils.decryptByAes(requestBody, aesPassword);
        body = decryptBody.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 基于解密后的请求体创建字符读取器。
     *
     * @return 字符读取器
     */
    @Override
    public BufferedReader getReader() {
        Charset charset = Charset.forName(getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }


    /**
     * 返回解密后请求体长度。
     *
     * @return 请求体长度
     */
    @Override
    public int getContentLength() {
        return body.length;
    }

    /**
     * 返回解密后请求体长度。
     *
     * @return 请求体长度
     */
    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /**
     * 返回解密后的请求体类型。
     *
     * @return JSON 内容类型
     */
    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }


    /**
     * 返回基于解密请求体的输入流。
     *
     * @return 解密请求体输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            /**
             * 读取解密请求体下一个字节。
             *
             * @return 下一个字节
             */
            @Override
            public int read() {
                return bais.read();
            }

            /**
             * 返回解密请求体剩余可读字节数。
             *
             * @return 剩余字节数
             */
            @Override
            public int available() {
                return bais.available();
            }

            /**
             * 判断解密请求体是否读取完毕。
             *
             * @return 是否读取完毕
             */
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            /**
             * 判断解密请求体输入流是否可读。
             *
             * @return 固定为 true
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 设置异步读取监听器。
             *
             * @param readListener 读取监听器
             */
            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
