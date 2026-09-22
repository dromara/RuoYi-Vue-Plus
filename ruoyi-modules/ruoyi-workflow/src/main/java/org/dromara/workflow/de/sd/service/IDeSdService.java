package org.dromara.workflow.de.sd.service;

import java.util.Map;

/**
 * SD 域服务接口 —— P1 档 A：SD-02 / SD-03 / SD-04 / SD-05 / SD-06 共 5 项。
 *
 * @author AIP
 */
public interface IDeSdService {

    /** SD-02 属性区按职责 / 输入 / 交付 / AI 能力 / 审核 / 异常组织 */
    Map<String, Object> attributes(Long sopId);

    /** SD-03 显式配置员工可选择 / 可追加 / 必须保留的资源 */
    Map<String, Object> resourceRules(Long sopId);

    /** SD-04 员工视角预览 + 人员解析 / 资料 / 能力缺口预检 */
    Map<String, Object> preview(Long sopId, Long userId);

    /** SD-05 分支 / 并行 / 等待 / 超时 / 返工 / 子流程 / 终止补偿 */
    Map<String, Object> graph(Long sopId);

    /** SD-06 流程版本 / Diff / 发布 / 回退 / 在途实例影响分析 */
    Map<String, Object> versions(Long sopId);
}