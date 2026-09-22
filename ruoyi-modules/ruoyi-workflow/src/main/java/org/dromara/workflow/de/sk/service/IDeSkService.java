package org.dromara.workflow.de.sk.service;

import org.dromara.workflow.de.sk.domain.vo.DeSkCardVo;

import java.util.List;
import java.util.Map;

/**
 * SK 域服务接口 —— P1 档 A：SK-01 / SK-02 / SK-06 共 3 项。
 *
 * @author AIP
 */
public interface IDeSkService {

    /** SK-01 技能目录 / 详情 / 我的草稿 / 节点选择器 */
    List<Map<String, Object>> catalog(Long userId);

    /** SK-02 技能卡 */
    DeSkCardVo card(Long skillId);

    /** SK-06 技能更新 / 回退 / 停用 时展示依赖 */
    Map<String, Object> dependencies(Long skillId);
}