package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * sai_user_agent 服务实现（存根）。
 *
 * <p><b>存根说明</b>：本类为 OW-03 决议清单的接口层存根（7 端点），
 * 全部方法体抛 {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：{@link SaiUserAgent}（既有 domain POJO，5 列）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiUserAgentServiceImpl
        extends ServiceImpl<SaiUserAgentMapper, SaiUserAgent>
        implements ISaiUserAgentService {

    private static final String STUB_MSG = "sai_user_agent 接口存根，等待 owner 实现";

    @Override
    public Long add(SaiUserAgent bo) {
        log.warn("[de.sai-user-agent] add 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean edit(SaiUserAgent bo) {
        log.warn("[de.sai-user-agent] edit 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        log.warn("[de.sai-user-agent] remove 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public SaiUserAgent getInfo(Long id) {
        log.warn("[de.sai-user-agent] getInfo 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiUserAgent> list(SaiUserAgent bo) {
        log.warn("[de.sai-user-agent] list 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public PageResult<SaiUserAgent> page(SaiUserAgent bo) {
        log.warn("[de.sai-user-agent] page 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }

    @Override
    public List<SaiUserAgent> listByFk(Long fkId) {
        log.warn("[de.sai-user-agent] listByFk 命中存根");
        throw new UnsupportedOperationException(STUB_MSG);
    }
}