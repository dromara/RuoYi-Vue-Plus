package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 按钮权限
 *
 * @author may
 * @date 2025-02-28
 */
@Data
public class ButtonPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 枚举路径
     */
    private String code;

    /**
     * 按钮编码
     */
    private String value;

    /**
     * 是否显示
     */
    private boolean show;
}
