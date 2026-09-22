package org.dromara.workflow.de.constants;

/**
 * 数字员工绑定在 flow_instance.variable 里的命名空间与必填键定义。
 *
 * <p>本类是 N-04 关闭条件 = 「能检出 flow_instance.variable 不可解析或缺键」的
 * 应用层校验单一真源。形状的完整定义见 27 号 §5.2，本类只持项目自定命名空间 + 必填键名。
 *
 * @author AIP
 */
public final class DeVariableConstants {

    /** flow_instance.variable 的顶层唯一允许的键（避开底座 8 个引擎键） */
    public static final String VARIABLE_KEY = "de.ctx";

    /** schema 版本；缺 v 即视为不可解析 */
    public static final int SCHEMA_VERSION = 1;

    /** binding 子对象必含键 */
    public static final String KEY_BINDING_AGENT_ID = "binding.agent_id";
    public static final String KEY_BINDING_BUSINESS_CODE = "binding.business_code";
    public static final String KEY_BINDING_BUSINESS_ID = "binding.business_id";

    /** task 子对象必含键 */
    public static final String KEY_TASK_STEP = "task.step";

    /** task.step 枚举（四值，与 27 号 §5.2 真值一致） */
    public static final String STEP_PENDING = "pending";
    public static final String STEP_RUNNING = "running";
    public static final String STEP_DONE = "done";
    public static final String STEP_FAILED = "failed";

    /** business_code 白名单（与 DeBindingConstants 共享同一值） */
    public static final String BUSINESS_CODE_DIGITAL_EMPLOYEE = "digital_employee.binding";

    /**
     * flow_instance.variable 的硬约束 2（27 号 §5.2）：65536 字符上限。
     * H1 P1 #20 (M#4)：超过该上限即拒收，避免底层 varchar 截断/异常。
     */
    public static final int MAX_VARIABLE_LENGTH = 65536;

    private DeVariableConstants() {
        // constants holder
    }
}