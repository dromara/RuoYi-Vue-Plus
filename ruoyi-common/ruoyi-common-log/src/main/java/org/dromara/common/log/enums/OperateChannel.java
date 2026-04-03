package org.dromara.common.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作渠道
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum OperateChannel {

    /**
     * 管理后台
     */
    WEB(0, "管理后台"),

    /**
     * 移动端APP
     */
    APP(1, "移动端APP"),

    /**
     * 小程序
     */
    MINI_APP(2, "小程序"),

    /**
     * 开放接口
     */
    OPEN_API(3, "开放接口"),

    /**
     * 定时任务
     */
    TASK(4, "定时任务"),

    /**
     * 第三方调用
     */
    THIRD(5, "第三方调用");

    /**
     * 操作渠道编码
     */
    private final Integer code;

    /**
     * 操作渠道描述
     */
    private final String desc;
}
