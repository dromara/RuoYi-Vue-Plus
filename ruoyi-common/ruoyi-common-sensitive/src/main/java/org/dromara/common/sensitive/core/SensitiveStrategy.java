package org.dromara.common.sensitive.core;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.DesensitizedUtil;
import lombok.AllArgsConstructor;
import org.dromara.common.core.utils.DesensitizedUtils;

import java.util.function.Function;

/**
 * 脱敏策略
 *
 * @author Yjoioooo
 * @version 3.6.0
 */
@AllArgsConstructor
public enum SensitiveStrategy {

    /**
     * 身份证脱敏
     */
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 3, 4)),

    /**
     * 手机号脱敏
     */
    PHONE(DesensitizedUtil::mobilePhone),

    /**
     * 地址脱敏
     */
    ADDRESS(s -> DesensitizedUtil.address(s, 8)),

    /**
     * 邮箱脱敏
     */
    EMAIL(DesensitizedUtil::email),

    /**
     * 银行卡
     */
    BANK_CARD(DesensitizedUtil::bankCard),

    /**
     * 中文名
     */
    CHINESE_NAME(DesensitizedUtil::chineseName),

    /**
     * 固定电话
     */
    FIXED_PHONE(DesensitizedUtil::fixedPhone),

    /**
     * 用户ID
     */
    USER_ID(s -> Convert.toStr(DesensitizedUtil.userId())),

    /**
     * 密码
     */
    PASSWORD(DesensitizedUtil::password),

    /**
     * ipv4
     */
    IPV4(DesensitizedUtil::ipv4),

    /**
     * ipv6
     */
    IPV6(DesensitizedUtil::ipv6),

    /**
     * 中国大陆车牌，包含普通车辆、新能源车辆
     */
    CAR_LICENSE(DesensitizedUtil::carLicense),

    /**
     * 只显示第一个字符
     */
    FIRST_MASK(DesensitizedUtil::firstMask),

    /**
     * 通用字符串脱敏
     * 可配置前后可见长度和中间掩码长度
     * 默认示例：前4位可见，后4位可见，中间固定4个*
     */
    STRING_MASK(s -> DesensitizedUtils.mask(s, 4, 4, 4)),

    /**
     * 高安全级别脱敏（Token / 私钥）：前2位可见，后2位可见，中间全部掩码
     */
    MASK_HIGH_SECURITY(s -> DesensitizedUtils.maskHighSecurity(s, 2, 2)),

    /**
     * 清空为""
     */
    CLEAR(s -> DesensitizedUtil.clear()),

    /**
     * 清空为null
     */
    CLEAR_TO_NULL(s -> DesensitizedUtil.clearToNull());

    //可自行添加其他脱敏策略

    private final Function<String, String> desensitizer;

    public Function<String, String> desensitizer() {
        return desensitizer;
    }
}
