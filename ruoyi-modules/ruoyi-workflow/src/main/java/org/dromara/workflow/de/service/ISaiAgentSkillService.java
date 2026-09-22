package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentSkill;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent_skill 服务接口（存根）。
 *
 * <p><b>存根说明</b>：本接口为 OW-03 决议清单的接口层存根（7 端点），
 * 实现由 {@link SaiAgentSkillServiceImpl} 全部抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
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