package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 按钮权限视图对象。
 *
 * @author may
 * @date 2025-02-28
 */
@Data
public class ButtonPermissionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 唯一编码
     */
    private String code;

    /**
     * 选项值
     */
    private String value;

    /**
     * 是否显示
     */
    private Boolean show;

    /**
     * 无参构造方法。
     */
    public ButtonPermissionVo() {
    }

    /**
     * 使用按钮编码和显示状态构造按钮权限对象。
     *
     * @param code 按钮编码
     * @param show 是否显示
     */
    public ButtonPermissionVo(String code, Boolean show) {
        this.code = code;
        this.show = show;
    }

}
