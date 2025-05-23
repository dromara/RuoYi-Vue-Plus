package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务分配人枚举
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum TaskAssigneeEnum {

    /**
     * 用户
     */
    USER("用户", ""),

    /**
     * 角色
     */
    ROLE("角色", "role:"),

    /**
     * 部门
     */
    DEPT("部门", "dept:"),

    /**
     * 岗位
     */
    POST("岗位", "post:");

    private final String desc;
    private final String code;

    /**
     * 根据描述获取对应的枚举类型
     * <p>
     * 通过传入描述，查找并返回匹配的枚举项。如果未找到匹配项，会抛出 {@link ServiceException}。
     * </p>
     *
     * @param desc 描述，用于匹配对应的枚举项
     * @return TaskAssigneeEnum 返回对应的枚举类型
     * @throws ServiceException 如果未找到匹配的枚举项
     */
    public static TaskAssigneeEnum fromDesc(String desc) {
        for (TaskAssigneeEnum type : values()) {
            if (type.getDesc().equals(desc)) {
                return type;
            }
        }
        throw new ServiceException("未知的办理人类型: " + desc);
    }

    /**
     * 根据代码获取对应的枚举类型
     * <p>
     * 通过传入代码，查找并返回匹配的枚举项。如果未找到匹配项，会抛出 {@link ServiceException}。
     * </p>
     *
     * @param code 代码，用于匹配对应的枚举项
     * @return TaskAssigneeEnum 返回对应的枚举类型
     * @throws IllegalArgumentException 如果未找到匹配的枚举项
     */
    public static TaskAssigneeEnum fromCode(String code) {
        for (TaskAssigneeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new ServiceException("未知的办理人类型代码: " + code);
    }

    /**
     * 获取所有办理人类型的描述列表
     * <p>
     * 获取当前枚举类所有项的描述字段列表，通常用于展示选择项。
     * </p>
     *
     * @return List<String> 返回所有办理人类型的描述列表
     */
    public static List<String> getAssigneeTypeList() {
        return Arrays.stream(values())
            .map(TaskAssigneeEnum::getDesc)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有办理人类型的代码列表
     * <p>
     * 获取当前枚举类所有项的代码字段列表，通常用于程序内部逻辑的判断。
     * </p>
     *
     * @return List<String> 返回所有办理人类型的代码列表
     */
    public static List<String> getAssigneeCodeList() {
        return Arrays.stream(values())
            .map(TaskAssigneeEnum::getCode)
            .collect(Collectors.toList());
    }
}

