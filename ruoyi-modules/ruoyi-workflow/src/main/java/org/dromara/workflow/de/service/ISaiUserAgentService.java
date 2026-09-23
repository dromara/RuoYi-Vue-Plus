package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiUserAgent;

import java.util.Collection;
import java.util.List;

/**
 * sai_user_agent 服务接口（存根）。
 *
 * <p><b>存根说明</b>：本接口为 OW-03 决议清单的接口层存根（7 端点），
 * 实现由 {@link SaiUserAgentServiceImpl} 全部抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
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