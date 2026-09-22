package org.dromara.workflow.de.fl.service;

import org.dromara.workflow.de.fl.domain.bo.DeFlExecuteBo;

import java.util.List;
import java.util.Map;

/**
 * FL 域服务接口 —— P1 档 A：FL-01 / FL-03 / FL-04 / FL-05 共 4 项。
 *
 * @author AIP
 */
public interface IDeFlService {

    /** FL-01 节点可选择的执行方案目录 */
    List<Map<String, Object>> catalog(Long nodeId);

    /** FL-03 结构化输入 / 串行 / 并行 / 条件分支 / 汇总 / 人工确认 */
    Long execute(DeFlExecuteBo bo);

    /** FL-04 方案版本（草稿 / 发布 / 差异 / 回退 / 依赖检查） */
    List<Map<String, Object>> versions(Long nodeId);

    /** FL-05 节点执行轨迹 / 阶段产物 / 重试 / 部分失败 */
    Map<String, Object> trace(Long taskId);
}