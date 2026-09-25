package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiUserAgent;

import java.util.Collection;
import java.util.List;

/**
 * sai_user_agent 服务接口（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：{@link SaiUserAgent}（既有 domain POJO，5 列）。
 *
 * @author AIP
 */
public interface ISaiUserAgentService extends IService<SaiUserAgent> {

    Long add(SaiUserAgent bo);

    Boolean edit(SaiUserAgent bo);

    Boolean remove(Collection<Long> ids);

    SaiUserAgent getInfo(Long id);

    List<SaiUserAgent> list(SaiUserAgent bo);

    PageResult<SaiUserAgent> page(SaiUserAgent bo);

    /** D-010 实化重载：带分页参数（/de/sai-user-agent/page 的 pageNum/pageSize）。 */
    PageResult<SaiUserAgent> page(SaiUserAgent bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery);

    List<SaiUserAgent> listByFk(Long fkId);
}