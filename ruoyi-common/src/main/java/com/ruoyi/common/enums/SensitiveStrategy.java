package com.ruoyi.common.enums;

import cn.hutool.core.util.DesensitizedUtil;
import java.util.function.Function;

/**
 * 脱敏策略
 *  @author Yjoioooo
 */
public enum SensitiveStrategy {

    /** 身份证脱敏 */
    ID_CARD(s -> DesensitizedUtil.idCardNum(s, 3, 4)),

    /** 手机号脱敏 */
    PHONE(DesensitizedUtil::mobilePhone),

    /**  地址脱敏 */
    ADDRESS(s -> DesensitizedUtil.address(s, 8));

    //可自行添加其他脱敏策略

    private final Function<String, String> desensitizer;

    SensitiveStrategy(Function<String, String> desensitizer) {
        this.desensitizer = desensitizer;
    }

    public Function<String, String> desensitizer() {
        return desensitizer;
    }
}
