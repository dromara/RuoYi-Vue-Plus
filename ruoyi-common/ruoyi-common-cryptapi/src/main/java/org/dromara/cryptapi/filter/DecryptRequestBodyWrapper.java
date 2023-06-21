package org.dromara.cryptapi.filter;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.exception.base.BaseException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.cryptapi.core.AesEncryptor;
import org.dromara.cryptapi.core.EncryptContext;
import org.dromara.cryptapi.core.RsaEncryptor;
import org.dromara.cryptapi.enums.EncodeType;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 解密请求参数工具类
 *
 * @author wdhcr
 */
public class DecryptRequestBodyWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public DecryptRequestBodyWrapper(HttpServletRequest request, RsaEncryptor rsaEncryptor, String headerFlag) throws IOException {
        super(request);
        String requestRsa = request.getHeader(headerFlag);
        if (StringUtils.isEmpty(requestRsa)) {
            throw new BaseException("加密AES的动态密码不能为空");
        }
        String decryptAes = new String(Base64.decode(rsaEncryptor.decrypt(requestRsa)));
        request.setCharacterEncoding(Constants.UTF8);
        byte[] readBytes = IoUtil.readBytes(request.getInputStream(), false);
        String requestBody = StringUtils.toEncodedString(readBytes, StandardCharsets.UTF_8);
        EncryptContext encryptContext = new EncryptContext();
        encryptContext.setPassword(decryptAes);
        encryptContext.setEncode(EncodeType.BASE64);
        AesEncryptor aesEncryptor = new AesEncryptor(encryptContext);
        String decryptBody = aesEncryptor.decrypt(requestBody);
        body = decryptBody.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }


    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }


    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public int available() {
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
