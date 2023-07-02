package org.dromara.common.social.utils;

import cn.hutool.core.util.ObjectUtil;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.*;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.social.config.properties.SocialLoginConfigProperties;
import org.dromara.common.social.config.properties.SocialProperties;

/**
 * 认证授权工具类
 *
 * @author thiszhc
 */
public class SocialUtils  {

    @SuppressWarnings("unchecked")
    public static AuthResponse<AuthUser> loginAuth(LoginBody loginBody, SocialProperties socialProperties) throws AuthException {
        AuthRequest authRequest = getAuthRequest(loginBody.getSource(), socialProperties);
        AuthCallback callback = new AuthCallback();
        callback.setCode(loginBody.getSocialCode());
        callback.setState(loginBody.getSocialState());
        return authRequest.login(callback);
    }

    public static AuthRequest getAuthRequest(String source, SocialProperties socialProperties) throws AuthException {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
         if (ObjectUtil.isNull(obj)) {
            throw new AuthException("不支持的第三方登录类型");
        }
        String clientId = obj.getClientId();
        String clientSecret = obj.getClientSecret();
        String redirectUri = obj.getRedirectUri();
        return switch (source.toLowerCase()) {
            case "dingtalk" -> new AuthDingTalkRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "baidu" -> new AuthBaiduRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "github" -> new AuthGithubRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "gitee" -> new AuthGiteeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "weibo" -> new AuthWeiboRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "coding" -> new AuthCodingRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "oschina" -> new AuthOschinaRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            // 支付宝在创建回调地址时，不允许使用localhost或者127.0.0.1，所以这儿的回调地址使用的局域网内的ip
            case "alipay" -> new AuthAlipayRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build(), socialProperties.getType().get("alipay").getAlipayPublicKey());
            case "qq" -> new AuthQqRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "wechat_open" -> new AuthWeChatOpenRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "taobao" -> new AuthTaobaoRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "douyin" -> new AuthDouyinRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "linkedin" -> new AuthLinkedinRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "microsoft" -> new AuthMicrosoftRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "renren" -> new AuthRenrenRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "stack_overflow" -> new AuthStackOverflowRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).stackOverflowKey("").build());
            case "huawei" -> new AuthHuaweiRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "wechat_enterprise" -> new AuthWeChatEnterpriseQrcodeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).agentId("").build());
            case "gitlab" -> new AuthGitlabRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "wechat_mp" -> new AuthWeChatMpRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "aliyun" -> new AuthAliyunRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret).redirectUri(redirectUri).build());
            default -> throw new AuthException("未获取到有效的Auth配置");
        };
    }
}

