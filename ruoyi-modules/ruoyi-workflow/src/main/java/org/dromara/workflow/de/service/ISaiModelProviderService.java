package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiModelProvider;

import java.util.Collection;
import java.util.List;

/**
 * sai_model_provider 服务接口（Map 存根 → typed，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
public interface ISaiModelProviderService extends IService<SaiModelProvider> {

    Long add(SaiModelProvider bo);

    Boolean edit(SaiModelProvider bo);

    Boolean remove(Collection<Long> ids);

    SaiModelProvider getInfo(Long id);

    List<SaiModelProvider> list(SaiModelProvider bo);

    PageResult<SaiModelProvider> page(SaiModelProvider bo);

    List<SaiModelProvider> listByFk(Long fkId);
}
