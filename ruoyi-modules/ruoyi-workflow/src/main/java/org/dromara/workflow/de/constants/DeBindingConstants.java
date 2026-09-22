package org.dromara.workflow.de.constants;

/**
 * 数字员工绑定业务的常量定义。
 *
 * <p>本类是 flow_instance_biz_ext.business_code 列在 P0 接缝定桩阶段的
 * 白名单单一真源。N-03 关闭条件 = 「能检出不在白名单的 business_code」。
 *
 * <p>引用：
 * <ul>
 *   <li>26 号 §二 接缝 1 —— 数字员工绑实例的 business_code 写法</li>
 *   <li>28 号 §一 可写位白名单 —— business_code 是我方可写列</li>
 * </ul>
 *
 * @author AIP
 */
public final class DeBindingConstants {

    /**
     * 数字员工绑定的业务码（写入 flow_instance_biz_ext.business_code）。
     * 当前唯一合法值。后续若新增绑定类型（如「数据源绑定」），须在本类追加常量
     * 并修改 28 号 N-03 关闭条件。
     */
    public static final String BINDING_DIGITAL_EMPLOYEE = "digital_employee.binding";

    private DeBindingConstants() {
        // constants holder
    }
}