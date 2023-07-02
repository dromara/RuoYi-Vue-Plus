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
    public static AuthResponse<AuthUser> loginAuth(LoginBody loginBody, SocialProperties socialProperties) throws AuthException {
        AuthRequest authRequest = getAuthRequest(loginBody.getSource(), socialProperties);
        AuthCallback callback = new AuthCallback();
        callback.setCode(loginBody.getSocialCode());
        callback.setState(loginBody.getSocialState());
        return authRequest.login(callback);
    }

    public static AuthRequest getAuthRequest(String source,SocialProperties socialProperties) throws AuthException {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
         if (ObjectUtil.isNull(obj)) {
            throw new AuthException("不支持的第三方登录类型");
        }
        String clientId = obj.getClientId();
        String clientSecret = obj.getClientSecret();
        String redirectUri = obj.getRedirectUri();
        AuthRequest authRequest = null;
        switch (source.toLowerCase()) {
            case "dingtalk" ->
                authRequest = new AuthDingTalkRequest(AuthConfig.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .redirectUri(redirectUri)
                    .build());
            case "baidu" ->
                authRequest = new AuthBaiduRequest(AuthConfig.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .redirectUri(redirectUri)
                    .build());
            case "github" ->
                authRequest = new AuthGithubRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "gitee" ->
                authRequest = new AuthGiteeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "weibo" ->
                authRequest = new AuthWeiboRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "coding" ->
                authRequest = new AuthCodingRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "oschina" ->
                authRequest = new AuthOschinaRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "alipay" ->
                // 支付宝在创建回调地址时，不允许使用localhost或者127.0.0.1，所以这儿的回调地址使用的局域网内的ip
                authRequest = new AuthAlipayRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "qq" ->
                authRequest = new AuthQqRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "wechat_open" -> authRequest = new AuthWeChatOpenRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "csdn" ->
                //注意,经咨询CSDN官方客服得知，CSDN的授权开放平台已经下线。如果以前申请过的应用，可以继续使用，但是不再支持申请新的应用。
                // so, 本项目中的CSDN登录只能针对少部分用户使用了
                authRequest = new AuthCsdnRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "taobao" ->
                authRequest = new AuthTaobaoRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "douyin" ->
                authRequest = new AuthDouyinRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "linkedin" ->
                authRequest = new AuthLinkedinRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "microsoft" -> authRequest = new AuthMicrosoftRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "mi" ->
                authRequest = new AuthMiRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "toutiao" ->
                authRequest = new AuthToutiaoRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "teambition" -> authRequest = new AuthTeambitionRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "pinterest" -> authRequest = new AuthPinterestRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).build());
            case "renren" ->
                authRequest = new AuthRenrenRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "stack_overflow" -> authRequest = new AuthStackOverflowRequest(AuthConfig.builder().clientId(clientId)
                .clientSecret(clientSecret).redirectUri(redirectUri).stackOverflowKey("").build());
            case "huawei" ->
                authRequest = new AuthHuaweiRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "wechat_enterprise" ->
                authRequest = new AuthWeChatEnterpriseQrcodeRequest(AuthConfig.builder().clientId(clientId)
                    .clientSecret(clientSecret).redirectUri(redirectUri).agentId("").build());
            case "kujiale" ->
                authRequest = new AuthKujialeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "gitlab" ->
                authRequest = new AuthGitlabRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "meituan" ->
                authRequest = new AuthMeituanRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "eleme" ->
                authRequest = new AuthElemeRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "wechat_mp" ->
                authRequest = new AuthWeChatMpRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            case "aliyun" ->
                authRequest = new AuthAliyunRequest(AuthConfig.builder().clientId(clientId).clientSecret(clientSecret)
                    .redirectUri(redirectUri).build());
            default -> {
            }
        }
        if (null == authRequest) {
            throw new AuthException("未获取到有效的Auth配置");
        }
        return authRequest;
    }
}

