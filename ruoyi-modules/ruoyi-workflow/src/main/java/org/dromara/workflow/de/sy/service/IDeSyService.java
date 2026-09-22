package org.dromara.workflow.de.sy.service;

import org.dromara.workflow.de.sy.domain.bo.DeSyCorrectionBo;

import java.util.Map;

/**
 * SY 域服务接口 —— P1 档 A：SY-05 / SY-06 / SY-07 / SY-08 / SY-09 共 5 项。
 *
 * @author AIP
 */
public interface IDeSyService {

    /** SY-05 区分已送达 / 已阅读 / 贡献已接受 / 主任务达标 / 业务交接完成 */
    Map<String, Object> ackState(Long taskId);

    /** SY-06 人类纠正和摘要冲突产生新事实版本 */
    Long correctionVersion(DeSyCorrectionBo bo);

    /** SY-07 断线 / 暂停 / 换 Agent / 重试 / 执行侧重启后从检查点恢复 */
    Map<String, Object> checkpointRecover(Long taskId);

    /** SY-08 迟到贡献 / 已撤回资料 / 已提交节点变更 的明确处置 */
    Map<String, Object> lateArrivals(Long taskId);

    /** SY-09 下游验证交接包 / 贡献版本 / 未决项 / 必需上下文可读性 */
    Map<String, Object> handoverPackageValidate(Long taskId);
}