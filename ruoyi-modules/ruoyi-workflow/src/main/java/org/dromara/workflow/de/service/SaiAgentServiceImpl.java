package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_agent 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：{@link SaiAgent}（既有 domain POJO，24 列）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiAgentServiceImpl
        extends ServiceImpl<SaiAgentMapper, SaiAgent>
        implements ISaiAgentService {

    private static final String STUB_MSG = "sai_agent 接口存根，等待 owner 实现";

    @Override
    public Long add(SaiAgent bo) {
        log.warn("[de.sai-agent] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(SaiAgent bo) {
        log.warn("[de.sai-agent] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-agent] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public SaiAgent getInfo(Long id) {
        log.warn("[de.sai-agent] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiAgent> list(SaiAgent bo) {
        log.warn("[de.sai-agent] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<SaiAgent> page(SaiAgent bo) {
        log.warn("[de.sai-agent] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiAgent> listByFk(Long fkId) {
        log.warn("[de.sai-agent] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}