package org.dromara.workflow.de.nc.service;

import org.dromara.workflow.de.nc.domain.bo.DeNcScopeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcExecModeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcDiffBo;

/**
 * 节点执行配置 NC 域服务接口 —— P1 档 A：NC-05 / NC-06 / NC-08 共 3 项。
 *
 * @author AIP
 */
public interface IDeNcService {

    /** NC-05 保存范围（仅本节点 / 个人偏好 / 共享模板提案） */
    Long saveScope(DeNcScopeBo bo);

    /** NC-06 执行模式（人工 / 协助 / 自动） */
    Long setExecMode(DeNcExecModeBo bo);

    /** NC-08 更换执行者 / 资源时显示差异与运行中限制 */
    Object showDiff(DeNcDiffBo bo);
}