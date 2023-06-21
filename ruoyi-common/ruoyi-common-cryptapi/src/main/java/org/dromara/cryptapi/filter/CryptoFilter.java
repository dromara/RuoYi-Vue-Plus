package org.dromara.cryptapi.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.cryptapi.core.EncryptContext;
import org.dromara.cryptapi.core.RsaEncryptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Objects;


/**
 * Crypto 过滤器
 *
 * @author wdhcr
 */
public class CryptoFilter implements Filter {

    public static final String CRYPTO_PUBLIC_KEY = "publicKey";
    public static final String CRYPTO_PRIVATE_KEY = "privateKey";
    public static final String CRYPTO_HEADER_FLAG = "headerFlag";
    private RsaEncryptor rsaEncryptor;
    private String headerFlag;


    @Override
    public void init(FilterConfig filterConfig) {
        EncryptContext encryptContext = new EncryptContext();
        encryptContext.setPublicKey(filterConfig.getInitParameter(CryptoFilter.CRYPTO_PUBLIC_KEY));
        encryptContext.setPrivateKey(filterConfig.getInitParameter(CryptoFilter.CRYPTO_PRIVATE_KEY));
        headerFlag = filterConfig.getInitParameter(CryptoFilter.CRYPTO_HEADER_FLAG);
        rsaEncryptor = new RsaEncryptor(encryptContext);
    }

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        ServletRequest requestWrapper = null;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)
            && (HttpMethod.PUT.matches(httpServletRequest.getMethod()) || HttpMethod.POST.matches(httpServletRequest.getMethod()))) {
            requestWrapper = new DecryptRequestBodyWrapper(httpServletRequest, rsaEncryptor, headerFlag);
        }
        chain.doFilter(Objects.requireNonNullElse(requestWrapper, request), response);
    }

    @Override
    public void destroy() {

    }
}
