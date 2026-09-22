package org.dromara.workflow.de.constants;

/**
 * P1 档 A 49 项业务需求实施期的常量集中地。
 *
 * <p>本类是「de/ 包内、不动底座、不新造表」前提下，49 项 P1 档 A 需求
 * 落地时的状态枚举 + 业务类型常量的单一真源。
 *
 * @author AIP
 */
public final class DeP1Constants {

    // ===== 工作流状态（取自底座 ry_workflow.sql:75 flow_instance.flow_status 注释 + 业务实际用法）=====
    /** flow_instance.flow_status：草稿 */
    public static final String FLOW_STATUS_DRAFT = "0";
    /** flow_instance.flow_status：审批中 */
    public static final String FLOW_STATUS_RUNNING = "1";
    /** flow_instance.flow_status：已完成 */
    public static final String FLOW_STATUS_FINISHED = "2";
    /** flow_instance.flow_status：已退回 */
    public static final String FLOW_STATUS_REJECTED = "3";
    /** flow_instance.flow_status：已撤销 */
    public static final String FLOW_STATUS_CANCELED = "4";

    // ===== 任务类型（与 de.ctx.task.step 四值之一并列、底座 flow_task.node_type 取值）=====
    /** flow_task.node_type：开始节点 */
    public static final int NODE_TYPE_START = 0;
    /** flow_task.node_type：审批节点 */
    public static final int NODE_TYPE_BETWEEN = 1;
    /** flow_task.node_type：结束节点 */
    public static final int NODE_TYPE_END = 2;

    // ===== flow_user.type 取值（与 28 号 §二接缝 6 一致）=====
    /** flow_user.type：审批人 */
    public static final String USER_TYPE_APPROVER = "1";
    /** flow_user.type：转办人 */
    public static final String USER_TYPE_TRANSFER = "2";
    /** flow_user.type：委托人 */
    public static final String USER_TYPE_DELEGATE = "3";
    /** flow_user.type：抄送人 */
    public static final String USER_TYPE_COPY = "4";

    // ===== 业务模块标识（落 flow_instance_biz_ext.business_code 白名单）=====
    /** 数字员工绑定 */
    public static final String BIZ_CODE_DIGITAL_EMPLOYEE = "digital_employee.binding";

    // ===== P1 档 A 49 项 域标识（落点说明用，不入库）=====
    public static final String DOMAIN_WD = "WD";
    public static final String DOMAIN_HR = "HR";
    public static final String DOMAIN_NC = "NC";
    public static final String DOMAIN_AG = "AG";
    public static final String DOMAIN_SK = "SK";
    public static final String DOMAIN_CN = "CN";
    public static final String DOMAIN_KB = "KB";
    public static final String DOMAIN_WS = "WS";
    public static final String DOMAIN_TM = "TM";
    public static final String DOMAIN_FL = "FL";
    public static final String DOMAIN_AT = "AT";
    public static final String DOMAIN_IM = "IM";
    public static final String DOMAIN_RS = "RS";
    public static final String DOMAIN_SD = "SD";
    public static final String DOMAIN_AD = "AD";
    public static final String DOMAIN_HC = "HC";
    public static final String DOMAIN_SY = "SY";

    // ===== P1 档 A 49 项（21 域 × 各 N 项，索引 = 业务查询 key）=====
    public static final String[] P1_A_IDS = {
        "WD-03", "WD-05", "WD-06",                                  // WD × 3
        "HR-03", "HR-05", "HR-06",                                  // HR × 3
        "NC-05", "NC-06", "NC-08",                                  // NC × 3
        "AG-04", "AG-05",                                           // AG × 2
        "SK-01", "SK-02", "SK-06",                                  // SK × 3
        "CN-03",                                                    // CN × 1
        "KB-01",                                                    // KB × 1
        "WS-01", "WS-02", "WS-04",                                  // WS × 3
        "TM-03", "TM-06",                                           // TM × 2
        "FL-01", "FL-03", "FL-04", "FL-05",                         // FL × 4
        "AT-01", "AT-04",                                           // AT × 2
        "IM-04", "IM-06",                                           // IM × 2
        "RS-05", "RS-06", "RS-07",                                  // RS × 3
        "SD-02", "SD-03", "SD-04", "SD-05", "SD-06",                // SD × 5
        "AD-03", "AD-04", "AD-07",                                  // AD × 3
        "HC-04", "HC-05", "HC-06", "HC-08",                         // HC × 4
        "SY-05", "SY-06", "SY-07", "SY-08", "SY-09"                 // SY × 5
    };

    /** P1 档 A 总数 = 49（本常量集中单一真源；自校验 = 数组 length == 49） */
    public static final int P1_A_TOTAL = P1_A_IDS.length;

    private DeP1Constants() {
        // constants holder
    }
}