package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgent;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent 服务接口（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：{@link SaiAgent}（既有 domain POJO，24 列）。
 *
 * @author AIP
 */
public interface ISaiAgentService extends IService<SaiAgent> {

    Long add(SaiAgent bo);

    Boolean edit(SaiAgent bo);

    Boolean remove(Collection<Long> ids);

    SaiAgent getInfo(Long id);

    List<SaiAgent> list(SaiAgent bo);

    PageResult<SaiAgent> page(SaiAgent bo);

    /**
     * D-010 实化重载：带分页参数的查询（前端 /de/sai-agent/page 的 pageNum/pageSize）。
     */
    PageResult<SaiAgent> page(SaiAgent bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery);

    List<SaiAgent> listByFk(Long fkId);
}