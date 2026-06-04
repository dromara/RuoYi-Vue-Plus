package org.dromara.common.web.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * 基于请求头解析国际化区域信息的语言解析器。
 *
 * @author Lion Li
 */
public class I18nLocaleResolver implements LocaleResolver {

    /**
     * 从请求头 {@code content-language} 中解析本次请求的区域信息，缺省时回退到系统默认区域。
     *
     * @param httpServletRequest 当前请求
     * @return 当前请求对应的区域设置
     */
    @Override
    public Locale resolveLocale(HttpServletRequest httpServletRequest) {
        String language = httpServletRequest.getHeader("content-language");
        Locale locale = Locale.getDefault();
        if (language != null && !language.isEmpty()) {
            locale = Locale.forLanguageTag(language.replace('_', '-'));
        }
        return locale;
    }

    /**
     * 当前项目不在服务端主动切换区域信息，因此保留空实现。
     *
     * @param httpServletRequest  当前请求
     * @param httpServletResponse 当前响应
     * @param locale              目标区域
     */
    @Override
    public void setLocale(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Locale locale) {

    }
}
