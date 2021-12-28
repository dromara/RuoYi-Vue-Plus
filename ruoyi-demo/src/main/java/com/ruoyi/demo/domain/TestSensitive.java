package com.ruoyi.demo.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.enums.SensitiveStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 测试单表对象 test_demo
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class TestSensitive extends BaseEntity {

    private static final long serialVersionUID=1L;

    /** 身份证 */
    @Sensitive(strategy = SensitiveStrategy.ID_CARD)
    private String idCard;

    /** 电话 */
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phone;

    /** 地址 */
    @Sensitive(strategy = SensitiveStrategy.ADDRESS)
    private String address;
}
