package com.ruoyi.common.constant;

/**
 * 通用常量信息
 *
 * @author ruoyi
 */
public interface Constants {

    /**
     * UTF-8 字符集
     */
    String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    String GBK = "GBK";

    /**
     * http请求
     */
    String HTTP = "http://";

    /**
     * https请求
     */
    String HTTPS = "https://";

    /**
     * 通用成功标识
     */
    String SUCCESS = "0";

    /**
     * 通用失败标识
     */
    String FAIL = "1";

    /**
     * 登录成功
     */
    String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    String LOGOUT = "Logout";

    /**
     * 注册
     */
    String REGISTER = "Register";

    /**
     * 登录失败
     */
    String LOGIN_FAIL = "Error";

    /**
     * 验证码 redis key
     */
    String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 登录用户 redis key
     */
    String LOGIN_TOKEN_KEY = "Authorization:login:token:";

    /**
     * 在线用户 redis key
     */
    String ONLINE_TOKEN_KEY = "online_tokens:";

    /**
     * 防重提交 redis key
     */
    String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 验证码有效期（分钟）
     */
    Integer CAPTCHA_EXPIRATION = 2;

    /**
     * 登陆错误 redis key
     */
    String LOGIN_ERROR = "login_error:";

    /**
     * 登录错误次数
     */
    Integer LOGIN_ERROR_NUMBER = 5;

    /**
     * 登录错误限制时间(分钟)
     */
    Integer LOGIN_ERROR_LIMIT_TIME = 10;

    /**
     * 令牌
     */
    String TOKEN = "token";

    /**
     * 令牌前缀
     */
    String LOGIN_USER_KEY = "login_user_key";

    /**
     * 参数管理 cache key
     */
    String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    String SYS_DICT_KEY = "sys_dict:";

}

