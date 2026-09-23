package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiMcpServer;
import org.dromara.workflow.de.mapper.SaiMcpServerMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sai_mcp_server 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiMcpServerServiceImpl
        extends ServiceImpl<SaiMcpServerMapper, SaiMcpServer>
        implements ISaiMcpServerService {

    private static final String STUB_MSG = "sai_mcp_server 接口存根，等待 owner 实现";

    @Override
    public Long add(SaiMcpServer bo) {
        log.warn("[de.sai-mcp-server] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(SaiMcpServer bo) {
        log.warn("[de.sai-mcp-server] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-mcp-server] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public SaiMcpServer getInfo(Long id) {
        log.warn("[de.sai-mcp-server] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiMcpServer> list(SaiMcpServer bo) {
        log.warn("[de.sai-mcp-server] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<SaiMcpServer> page(SaiMcpServer bo) {
        log.warn("[de.sai-mcp-server] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiMcpServer> listByFk(Long fkId) {
        log.warn("[de.sai-mcp-server] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}