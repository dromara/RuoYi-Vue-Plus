package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentSkill;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_skill 服务接口（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：{@link SaiAgentSkill}（既有 domain POJO，BR-05 校验需要的 3 列）。
 *
 * @author AIP
 */
public interface ISaiAgentSkillService extends IService<SaiAgentSkill> {

    Long add(SaiAgentSkill bo);

    Boolean edit(SaiAgentSkill bo);

    Boolean remove(Collection<Long> ids);

    SaiAgentSkill getInfo(Long id);

    List<SaiAgentSkill> list(SaiAgentSkill bo);

    PageResult<SaiAgentSkill> page(SaiAgentSkill bo);

    List<SaiAgentSkill> listByFk(Long fkId);
}