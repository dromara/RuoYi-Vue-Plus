package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * sai_user_agent 服务实现（D-010 顺序推进批②实化：存根 → 真实 CRUD）。
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

    // ★ D-010 顺序推进批②实化（2026-09-22）：订阅关系 CRUD——de-conversation 的
    //   validateSubscription 已消费本 mapper（读路径真）；此处补齐写/分页路径。

    @Override
    public Long add(SaiUserAgent bo) {
        save(bo);
        log.info("[de.sai-user-agent] add id={} userId={} agentId={}", bo.getId(), bo.getUserId(), bo.getAgentId());
        return bo.getId();
    }

    @Override
    public Boolean edit(SaiUserAgent bo) {
        return updateById(bo);
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    public SaiUserAgent getInfo(Long id) {
        return getById(id);
    }

    @Override
    public List<SaiUserAgent> list(SaiUserAgent bo) {
        return list(buildWrapper(bo));
    }

    @Override
    public PageResult<SaiUserAgent> page(SaiUserAgent bo) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public PageResult<SaiUserAgent> page(SaiUserAgent bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        var pg = page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageQuery.getPageNum(), pageQuery.getPageSize()), buildWrapper(bo));
        return PageResult.build(pg.getRecords(), pg.getTotal());
    }

    @Override
    public List<SaiUserAgent> listByFk(Long fkId) {
        // fk 维度 = agentId（订阅按 agent 反查用户集）
        return list(new LambdaQueryWrapper<SaiUserAgent>().eq(SaiUserAgent::getAgentId, fkId));
    }

    private LambdaQueryWrapper<SaiUserAgent> buildWrapper(SaiUserAgent bo) {
        LambdaQueryWrapper<SaiUserAgent> qw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (bo.getUserId() != null) {
                qw.eq(SaiUserAgent::getUserId, bo.getUserId());
            }
            if (bo.getAgentId() != null) {
                qw.eq(SaiUserAgent::getAgentId, bo.getAgentId());
            }
        }
        return qw.orderByDesc(SaiUserAgent::getId);
    }
}
