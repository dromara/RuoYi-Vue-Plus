package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiMcpServer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sai_mcp_server 服务接口（存根）。
 *
 * <p><b>存根说明</b>：本接口为 OW-03 决议清单的接口层存根（7 端点），
 * 实现由 {@link SaiMcpServerServiceImpl} 全部抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：本表无专属 domain POJO（不实装），用 {@code SaiMcpServer}
 * 作为通用实体。
 *
 * @author AIP
 */
public interface ISaiMcpServerService extends IService<SaiMcpServer> {

    Long add(SaiMcpServer bo);

    Boolean edit(SaiMcpServer bo);

    Boolean remove(Collection<Long> ids);

    SaiMcpServer getInfo(Long id);

    List<SaiMcpServer> list(SaiMcpServer bo);

    PageResult<SaiMcpServer> page(SaiMcpServer bo);

    List<SaiMcpServer> listByFk(Long fkId);
}