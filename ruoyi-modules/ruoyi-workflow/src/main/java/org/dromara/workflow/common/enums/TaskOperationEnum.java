package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务操作类型枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum TaskOperationEnum {

    /**
     * 委派
     */
    DELEGATE_TASK("delegateTask", "委派"),

    /**
     * 转办
     */
    TRANSFER_TASK("transferTask", "转办"),

    /**
     * 加签
     */
    ADD_SIGNATURE("addSignature", "加签"),

    /**
     * 减签
     */
    REDUCTION_SIGNATURE("reductionSignature", "减签");

    private final String code;
    private final String desc;

    private static final Map<String, TaskOperationEnum> CODE_MAP = Arrays.stream(values())
        .collect(Collectors.toConcurrentMap(TaskOperationEnum::getCode, Function.identity()));

    /**
     * 根据 code 获取枚举
     */
    public static TaskOperationEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }

}
