package org.dromara.common.auth.utils;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.request.*;

/**
 * 认证授权工具类
 *
 * @author ruoyi
 */
public class AuthUtils {
    public static AuthRequest getAuthRequest(String source, String clientId, String clientSecret, String redirectUri,
                                             AuthStateCache authStateCache)
    {
        AuthRequest authRequest = null;
        switch (source.toLowerCase()) {
            case "dingtalk" ->
                authRequest = new AuthDingTalkRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "baidu" ->
                authRequest = new AuthBaiduRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "github" ->
                authRequest = new AuthGithubRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "gitee" ->
                authRequest = new AuthGiteeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "weibo" ->
                authRequest = new AuthWeiboRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "coding" ->
                authRequest = new AuthCodingRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).codingGroupName("").build(), authStateCache);
            case "oschina" ->
                authRequest = new AuthOschinaRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "alipay" ->
                // 支付宝在创建回调地址时，不允许使用localhost或者127.0.0.1，所以这儿的回调地址使用的局域网内的ip
                authRequest = new AuthAlipayRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .alipayPublicKey("").redirectUri(redirectUri).build(), authStateCache);
            case "qq" ->
                authRequest = new AuthQqRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "wechat_open" -> authRequest = new AuthWeChatOpenRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build(), authStateCache);
            case "csdn" ->
                authRequest = new AuthCsdnRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "taobao" ->
                authRequest = new AuthTaobaoRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "douyin" ->
                authRequest = new AuthDouyinRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "linkedin" ->
                authRequest = new AuthLinkedinRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "microsoft" -> authRequest = new AuthMicrosoftRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build(), authStateCache);
            case "mi" ->
                authRequest = new AuthMiRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "toutiao" ->
                authRequest = new AuthToutiaoRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "teambition" -> authRequest = new AuthTeambitionRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build(), authStateCache);
            case "pinterest" -> authRequest = new AuthPinterestRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build(), authStateCache);
            case "renren" ->
                authRequest = new AuthRenrenRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "stack_overflow" -> authRequest = new AuthStackOverflowRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).stackOverflowKey("").build(),
                authStateCache);
            case "huawei" ->
                authRequest = new AuthHuaweiRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "wechat_enterprise" ->
                authRequest = new AuthWeChatEnterpriseRequest(AuthConfig.builder().clientId(clientId)
                    .clientSecret(clientSecret).redirectUri(redirectUri).agentId("").build(), authStateCache);
            case "kujiale" ->
                authRequest = new AuthKujialeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "gitlab" ->
                authRequest = new AuthGitlabRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "meituan" ->
                authRequest = new AuthMeituanRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "eleme" ->
                authRequest = new AuthElemeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "wechat_mp" ->
                authRequest = new AuthWeChatMpRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            case "aliyun" ->
                authRequest = new AuthAliyunRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build(), authStateCache);
            default -> {
            }
        }
        if (null == authRequest)
        {
            throw new AuthException("未获取到有效的Auth配置");
        }
        return authRequest;
    }
}

