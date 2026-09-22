package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sai_model_config 服务接口（存根）。
 *
 * <p><b>存根说明</b>：本接口为 OW-03 决议清单的接口层存根（7 端点），
 * 实现由 {@link SaiModelConfigServiceImpl} 全部抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：本表无专属 domain POJO（不实装），用 {@code Map<String, Object>}
 * 作为通用实体。
 *
 * @author AIP
 */
public interface ISaiModelConfigService extends IService<Map<String, Object>> {

    Long add(Map<String, Object> bo);

    Boolean edit(Map<String, Object> bo);

    Boolean remove(Collection<Long> ids);

    Map<String, Object> getInfo(Long id);

    List<Map<String, Object>> list(Map<String, Object> bo);

    PageResult<Map<String, Object>> page(Map<String, Object> bo);

    List<Map<String, Object>> listByFk(Long fkId);
}