package me.zhyd.oauth.request;

import com.alibaba.fastjson2.JSONObject;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.constant.Keys;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.utils.HttpUtils;
import me.zhyd.oauth.utils.StringUtils;
import me.zhyd.oauth.utils.UrlBuilder;

/**
 * <p>
 * 企业微信登录父类
 * </p>
 *
 * @author liguanhua (347826496(a)qq.com)
 * @since 1.15.9
 */
public abstract class AbstractAuthWeChatEnterpriseRequest extends AuthDefaultRequest {

    /**
     * 创建企业微信认证请求。
     *
     * @param config 授权配置
     * @param source 授权来源
     */
    public AbstractAuthWeChatEnterpriseRequest(AuthConfig config, AuthSource source) {
        super(config, source);
    }


    /**
     * 创建企业微信认证请求。
     *
     * @param config         授权配置
     * @param source         授权来源
     * @param authStateCache 授权状态缓存
     */
    public AbstractAuthWeChatEnterpriseRequest(AuthConfig config, AuthSource source, AuthStateCache authStateCache) {
        super(config, source, authStateCache);
    }

    /**
     * 获取企业微信访问令牌。
     *
     * @param authCallback 授权回调参数
     * @return 访问令牌
     */
    @Override
    public AuthToken getAccessToken(AuthCallback authCallback) {
        String response = doGetAuthorizationCode(accessTokenUrl(null));

        JSONObject object = this.checkResponse(response);

        return AuthToken.builder()
            .accessToken(object.getString(Keys.OAUTH2_ACCESS_TOKEN))
            .expireIn(object.getIntValue(Keys.OAUTH2_EXPIRES_IN))
            .code(authCallback.getCode())
            .build();
    }

    /**
     * 获取企业微信用户信息。
     *
     * @param authToken 访问令牌
     * @return 授权用户信息
     */
    @Override
    public AuthUser getUserInfo(AuthToken authToken) {
        String response = doGetUserInfo(authToken);
        JSONObject object = this.checkResponse(response);

        // 返回 OpenId 或其他，均代表非当前企业用户，不支持
        // https://github.com/justauth/JustAuth/issues/227 修复bug
        if (!object.containsKey("userid")) {
            throw new AuthException(AuthResponseStatus.UNIDENTIFIED_PLATFORM, source);
        }
        String userId = object.getString("userid");
        String userTicket = object.getString("user_ticket");
        JSONObject userDetail = getUserDetail(authToken.getAccessToken(), userId, userTicket);

        return AuthUser.builder()
            .rawUserInfo(userDetail)
            .username(userDetail.getString(Keys.NAME))
            .nickname(userDetail.getString("alias"))
            .avatar(userDetail.getString(Keys.AVATAR))
            .location(userDetail.getString(Keys.OAUTH2_SCOPE__ADDRESS))
            .email(userDetail.getString(Keys.OAUTH2_SCOPE__EMAIL))
            .uuid(userId)
            .gender(AuthUserGender.getWechatRealGender(userDetail.getString(Keys.GENDER)))
            .token(authToken)
            .source(source.toString())
            .build();
    }

    /**
     * 校验请求结果
     *
     * @param response 请求结果
     * @return 如果请求结果正常，则返回JSONObject
     */
    private JSONObject checkResponse(String response) {
        JSONObject object = JSONObject.parseObject(response);

        if (object.containsKey(Keys.VARIANT__ERRCODE) && object.getIntValue(Keys.VARIANT__ERRCODE) != 0) {
            throw new AuthException(object.getString(Keys.VARIANT__ERRMSG), source);
        }

        return object;
    }


    /**
     * 返回获取accessToken的url
     *
     * @param code 授权码
     * @return 返回获取accessToken的url
     */
    @Override
    protected String accessTokenUrl(String code) {
        return UrlBuilder.fromBaseUrl(source.accessToken())
            .queryParam("corpid", config.getClientId())
            .queryParam("corpsecret", config.getClientSecret())
            .build();
    }

    /**
     * 返回获取userInfo的url
     *
     * @param authToken 用户授权后的token
     * @return 返回获取userInfo的url
     */
    @Override
    protected String userInfoUrl(AuthToken authToken) {
        return UrlBuilder.fromBaseUrl(source.userInfo())
            .queryParam(Keys.OAUTH2_ACCESS_TOKEN, authToken.getAccessToken())
            .queryParam(Keys.OAUTH2_CODE, authToken.getCode())
            .build();
    }

    /**
     * 用户详情
     *
     * @param accessToken accessToken
     * @param userId      企业内用户id
     * @param userTicket  成员票据，用于获取用户信息或敏感信息
     * @return 用户详情
     */
    private JSONObject getUserDetail(String accessToken, String userId, String userTicket) {
        // 用户基础信息
        String userInfoUrl = UrlBuilder.fromBaseUrl("https://qyapi.weixin.qq.com/cgi-bin/user/get")
            .queryParam(Keys.OAUTH2_ACCESS_TOKEN, accessToken)
            .queryParam(Keys.USERID, userId)
            .build();
        String userInfoResponse = new HttpUtils(config.getHttpConfig()).get(userInfoUrl).getBody();
        JSONObject userInfo = checkResponse(userInfoResponse);

        // 用户敏感信息
        if (StringUtils.isNotEmpty(userTicket)) {
            String userDetailUrl = UrlBuilder.fromBaseUrl("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail")
                .queryParam(Keys.OAUTH2_ACCESS_TOKEN, accessToken)
                .build();
            JSONObject param = new JSONObject();
            param.put("user_ticket", userTicket);
            String userDetailResponse = new HttpUtils(config.getHttpConfig()).post(userDetailUrl, param.toJSONString()).getBody();
            JSONObject userDetail = checkResponse(userDetailResponse);

            userInfo.putAll(userDetail);
        }
        return userInfo;
    }

}
