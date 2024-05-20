package org.dromara.workflow.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 任务状态枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {
    /**
     * 撤销
     */
    CANCEL("cancel", "撤销"),
    /**
     * 通过
     */
    PASS("pass", "通过"),
    /**
     * 待审核
     */
    WAITING("waiting", "待审核"),
    /**
     * 作废
     */
    INVALID("invalid", "作废"),
    /**
     * 退回
     */
    BACK("back", "退回"),
    /**
     * 终止
     */
    TERMINATION("termination", "终止"),
    /**
     * 转办
     */
    TRANSFER("transfer", "转办"),
    /**
     * 委托
     */
    PENDING("pending", "委托"),
    /**
     * 抄送
     */
    COPY("copy", "抄送"),
    /**
     * 加签
     */
    SIGN("sign", "加签"),
    /**
     * 减签
     */
    SIGN_OFF("sign_off", "减签"),
    /**
     * 超时
     */
    TIMEOUT("timeout", "超时");

    /**
     * 状态
     */
    private final String status;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 任务业务状态
     *
     * @param status 状态
     */
    public static String findByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return StrUtil.EMPTY;
        }

        return Arrays.stream(TaskStatusEnum.values())
            .filter(statusEnum -> statusEnum.getStatus().equals(status))
            .findFirst()
            .map(TaskStatusEnum::getDesc)
            .orElse(StrUtil.EMPTY);
    }
}

