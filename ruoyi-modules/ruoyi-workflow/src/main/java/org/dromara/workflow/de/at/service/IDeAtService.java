package org.dromara.workflow.de.at.service;

import org.dromara.workflow.de.at.domain.bo.DeAtCreateBo;
import org.dromara.workflow.de.at.domain.bo.DeAtRunConfigBo;

/**
 * AT 域服务接口 —— P1 档 A：AT-01 / AT-04 共 2 项。
 *
 * <p>主导族 sj_*（SnailJob 自带）。本类落 sj_job（23 列），加 ext / job_group_name。
 *
 * @author AIP
 */
public interface IDeAtService {

    /** AT-01 自动化创建 */
    Long create(DeAtCreateBo bo);

    /** AT-04 并发 / 去重 / 重试 / 错过补跑 / 失败通知 */
    Long runConfig(DeAtRunConfigBo bo);
}