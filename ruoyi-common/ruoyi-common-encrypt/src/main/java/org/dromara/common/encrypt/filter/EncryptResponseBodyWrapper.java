package org.dromara.common.encrypt.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.dromara.common.encrypt.utils.EncryptUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密响应参数包装类
 *
 * @author Michelle.Chung
 */
public class EncryptResponseBodyWrapper extends HttpServletResponseWrapper {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ByteArrayOutputStream byteArrayOutputStream;
    private final ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;
    private Charset charset;

    /**
     * 构造加密响应包装器。
     *
     * @param response 原始响应
     * @throws IOException 创建输出流异常
     */
    public EncryptResponseBodyWrapper(HttpServletResponse response) throws IOException {
        super(response);
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.servletOutputStream = this.getOutputStream();
        this.charset = resolveCharset(response);
    }

    /**
     * 返回缓存响应内容的字符输出器。
     *
     * @return 字符输出器
     */
    @Override
    public PrintWriter getWriter() {
        if (printWriter == null) {
            charset = resolveCharset((HttpServletResponse) getResponse());
            printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, charset));
        }
        return printWriter;
    }

    /**
     * 刷新缓存的响应输出流和字符输出器。
     *
     * @throws IOException IO 异常
     */
    @Override
    public void flushBuffer() throws IOException {
        if (servletOutputStream != null) {
            servletOutputStream.flush();
        }
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    /**
     * 重置已缓存的响应内容。
     */
    @Override
    public void reset() {
        byteArrayOutputStream.reset();
    }

    /**
     * 重置已缓存的响应缓冲区。
     */
    @Override
    public void resetBuffer() {
        byteArrayOutputStream.reset();
    }

    /**
     * 获取已缓存的响应字节。
     *
     * @return 响应字节数组
     * @throws IOException 刷新响应缓冲异常
     */
    public byte[] getResponseData() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 获取已缓存的响应内容。
     *
     * @return 响应文本
     * @throws IOException 刷新响应缓冲异常
     */
    public String getContent() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toString(charset);
    }

    /**
     * 获取加密内容
     *
     * @param servletResponse response
     * @param publicKey       RSA公钥 (用于加密 AES 秘钥)
     * @param headerFlag      请求头标志
     * @return 加密内容
     * @throws IOException
     */
    public String getEncryptContent(HttpServletResponse servletResponse, String publicKey, String headerFlag) throws IOException {
        // 生成秘钥
        String aesPassword = generateAesPassword();
        // 秘钥使用 Base64 编码
        String encryptAes = EncryptUtils.encryptByBase64(aesPassword);
        // Rsa 公钥加密 Base64 编码
        String encryptPassword = EncryptUtils.encryptByRsa(encryptAes, publicKey);

        // 设置响应头
        // vue版本需要设置
        servletResponse.addHeader("Access-Control-Expose-Headers", headerFlag);
        servletResponse.setHeader(headerFlag, encryptPassword);
        servletResponse.setCharacterEncoding(charset.name());


        // 获取原始内容
        String originalBody = this.getContent();
        // 对内容进行加密
        String encryptContent = EncryptUtils.encryptByAes(originalBody, aesPassword);
        servletResponse.setContentLengthLong(encryptContent.getBytes(charset).length);
        return encryptContent;
    }

    /**
     * 返回缓存响应内容的二进制输出流。
     *
     * @return 响应输出流
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            /**
             * 判断响应输出流是否可写。
             *
             * @return 固定为 true
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 设置异步写入监听器。
             *
             * @param writeListener 写入监听器
             */
            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            /**
             * 写入单个字节到响应缓存。
             *
             * @param b 待写入字节
             * @throws IOException IO 异常
             */
            @Override
            public void write(int b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            /**
             * 写入字节数组到响应缓存。
             *
             * @param b 待写入字节数组
             * @throws IOException IO 异常
             */
            @Override
            public void write(byte[] b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            /**
             * 写入字节数组片段到响应缓存。
             *
             * @param b 待写入字节数组
             * @param off 起始偏移量
             * @param len 写入长度
             * @throws IOException IO 异常
             */
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byteArrayOutputStream.write(b, off, len);
            }
        };
    }

    /**
     * 解析响应字符集，未设置时默认使用 UTF-8。
     *
     * @param response 原始响应
     * @return 响应字符集
     */
    private Charset resolveCharset(HttpServletResponse response) {
        String characterEncoding = response.getCharacterEncoding();
        if (characterEncoding == null) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(characterEncoding);
    }

    /**
     * 生成响应内容 AES 加密密钥。
     *
     * @return AES 密钥
     */
    private String generateAesPassword() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

}
