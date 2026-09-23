package org.dromara.workflow.de.nc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.domain.DeSysNotice;
import org.dromara.workflow.de.mapper.DeSysNoticeMapper;
import org.dromara.workflow.de.nc.domain.bo.DeNcNoticeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcNoticeQueryBo;
import org.dromara.workflow.de.nc.domain.vo.DeNcNoticeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * NC-04 通知中心 CRUD 服务实现（2026-09-23）。
 *
 * <p><b>写入路径</b>：本类直接 INSERT/UPDATE/DELETE sys_notice（通过本地镜像
 * {@link DeSysNoticeMapper}），不走 ruoyi-system 的 {@code ISysNoticeService}。
 * 这是因为 ruoyi-workflow 只依赖 ruoyi-api，拿不到 ruoyi-system 的 service。
 * 镜像与底座 controller 双轨写入同一张表，数据天然一致。
 *
 * <p><b>判别力</b>：新增/更新/删除后通过 SELECT 实测行数；列表通过 status 过滤
 * 与模糊查询验证。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeNcNoticeServiceImpl implements IDeNcNoticeService {

    private final DeSysNoticeMapper deSysNoticeMapper;

    @Override
    public IPage<DeNcNoticeVo> page(DeNcNoticeQueryBo query, Long pageNum, Long pageSize) {
        Page<DeSysNotice> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        Page<DeSysNotice> result = deSysNoticeMapper.selectPage(page, buildWrapper(query));
        return result.convert(this::toVo);
    }

    @Override
    public List<DeNcNoticeVo> list(DeNcNoticeQueryBo query) {
        List<DeSysNotice> rows = deSysNoticeMapper.selectList(buildWrapper(query));
        List<DeNcNoticeVo> out = new ArrayList<>(rows.size());
        for (DeSysNotice row : rows) {
            out.add(toVo(row));
        }
        return out;
    }

    @Override
    public DeNcNoticeVo getInfo(Long noticeId) {
        if (noticeId == null) {
            throw new ServiceException("noticeId 必填");
        }
        DeSysNotice row = deSysNoticeMapper.selectById(noticeId);
        return row == null ? null : toVo(row);
    }

    @Override
    public Long add(DeNcNoticeBo bo) {
        if (bo.getNoticeTitle() == null || bo.getNoticeTitle().isBlank()) {
            throw new ServiceException("noticeTitle 必填");
        }
        DeSysNotice entity = new DeSysNotice();
        BeanUtils.copyProperties(bo, entity);
        if (entity.getStatus() == null) {
            entity.setStatus("0");
        }
        entity.setCreateTime(new Date());
        entity.setUpdateTime(entity.getCreateTime());
        deSysNoticeMapper.insert(entity);
        log.info("[nc.notice.add] noticeId={} title={}", entity.getNoticeId(), entity.getNoticeTitle());
        return entity.getNoticeId();
    }

    @Override
    public int edit(DeNcNoticeBo bo) {
        if (bo.getNoticeId() == null) {
            throw new ServiceException("noticeId 必填");
        }
        DeSysNotice entity = deSysNoticeMapper.selectById(bo.getNoticeId());
        if (entity == null) {
            throw new ServiceException("公告不存在");
        }
        entity.setNoticeTitle(bo.getNoticeTitle());
        entity.setNoticeType(bo.getNoticeType());
        entity.setNoticeContent(bo.getNoticeContent());
        entity.setStatus(bo.getStatus() == null ? entity.getStatus() : bo.getStatus());
        entity.setRemark(bo.getRemark());
        entity.setUpdateTime(new Date());
        int rows = deSysNoticeMapper.updateById(entity);
        log.info("[nc.notice.edit] noticeId={} rows={}", bo.getNoticeId(), rows);
        return rows;
    }

    @Override
    public int remove(Long[] noticeIds) {
        if (noticeIds == null || noticeIds.length == 0) {
            throw new ServiceException("noticeIds 必填");
        }
        int rows = deSysNoticeMapper.deleteByIds(java.util.Arrays.asList(noticeIds));
        log.info("[nc.notice.remove] ids={} rows={}", (Object[]) noticeIds, rows);
        return rows;
    }

    // ===== 内部辅助 =====

    private LambdaQueryWrapper<DeSysNotice> buildWrapper(DeNcNoticeQueryBo query) {
        LambdaQueryWrapper<DeSysNotice> w = new LambdaQueryWrapper<>();
        if (query == null) {
            return w.orderByDesc(DeSysNotice::getCreateTime);
        }
        if (query.getNoticeTitle() != null && !query.getNoticeTitle().isBlank()) {
            w.like(DeSysNotice::getNoticeTitle, query.getNoticeTitle());
        }
        if (query.getNoticeType() != null && !query.getNoticeType().isBlank()) {
            w.eq(DeSysNotice::getNoticeType, query.getNoticeType());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            w.eq(DeSysNotice::getStatus, query.getStatus());
        }
        if (query.getCreateBy() != null) {
            w.eq(DeSysNotice::getCreateBy, query.getCreateBy());
        }
        w.orderByDesc(DeSysNotice::getCreateTime);
        return w;
    }

    private DeNcNoticeVo toVo(DeSysNotice row) {
        DeNcNoticeVo vo = new DeNcNoticeVo();
        BeanUtils.copyProperties(row, vo);
        return vo;
    }
}