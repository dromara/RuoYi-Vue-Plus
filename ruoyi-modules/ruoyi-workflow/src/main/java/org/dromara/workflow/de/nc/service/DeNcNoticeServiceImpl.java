package org.dromara.workflow.de.nc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
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
 * <p><b>H-5 安全修复（2026-09-23）</b>：
 * <ul>
 *   <li>{@link #add(DeNcNoticeBo)} 改用显式字段赋值（白名单）替代
 *       {@code BeanUtils.copyProperties(bo, entity)}，避免前端 JSON 携带
 *       {@code createBy} / {@code createTime} 等审计字段伪造身份。</li>
 *   <li>{@code createBy} / {@code createTime} / {@code updateBy} / {@code updateTime}
 *       全部由服务端强制重写（{@link LoginHelper#getUsername()} + {@link LoginHelper#getUserId()} + {@code new Date()}）。</li>
 *   <li>{@link #edit(DeNcNoticeBo)} 补强：{@code updateBy} 必填（先前依赖 BO 不暴露该字段，未实测）。</li>
 *   <li>{@link #remove(Long[])} 加属主校验：仅 admin 可跨用户删；非 admin 仅删自己创建的公告。</li>
 * </ul>
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
        // H-5 修复：白名单显式赋值（替代 BeanUtils.copyProperties）
        DeSysNotice entity = new DeSysNotice();
        entity.setNoticeTitle(bo.getNoticeTitle());
        entity.setNoticeType(bo.getNoticeType());
        entity.setNoticeContent(bo.getNoticeContent());
        entity.setStatus(bo.getStatus() == null ? "0" : bo.getStatus());
        entity.setRemark(bo.getRemark());
        // H-5 修复：审计字段强制服务端重写（不可从前端伪造）
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setCreateBy(LoginHelper.getUserId());
        entity.setUpdateBy(LoginHelper.getUserId());
        deSysNoticeMapper.insert(entity);
        log.info("[nc.notice.add] noticeId={} title={} createBy={}",
            entity.getNoticeId(), entity.getNoticeTitle(), entity.getCreateBy());
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
        // H-5 修复：编辑前校验属主（非 admin 仅改自己创建的公告）
        if (!LoginHelper.isSuperAdmin()) {
            Long currentUserId = LoginHelper.getUserId();
            if (entity.getCreateBy() == null || !currentUserId.equals(entity.getCreateBy())) {
                throw new ServiceException("仅公告创建者或 admin 可编辑该公告");
            }
        }
        entity.setNoticeTitle(bo.getNoticeTitle());
        entity.setNoticeType(bo.getNoticeType());
        entity.setNoticeContent(bo.getNoticeContent());
        entity.setStatus(bo.getStatus() == null ? entity.getStatus() : bo.getStatus());
        entity.setRemark(bo.getRemark());
        entity.setUpdateTime(new Date());
        // H-5 修复：审计字段 update_by 强制服务端重写（不可从前端伪造）
        entity.setUpdateBy(LoginHelper.getUserId());
        int rows = deSysNoticeMapper.updateById(entity);
        log.info("[nc.notice.edit] noticeId={} rows={} updateBy={}",
            bo.getNoticeId(), rows, entity.getUpdateBy());
        return rows;
    }

    @Override
    public int remove(Long[] noticeIds) {
        if (noticeIds == null || noticeIds.length == 0) {
            throw new ServiceException("noticeIds 必填");
        }
        // H-5 修复：删除前校验属主（非 admin 仅删自己创建的公告）
        int rows;
        if (LoginHelper.isSuperAdmin()) {
            rows = deSysNoticeMapper.deleteByIds(java.util.Arrays.asList(noticeIds));
        } else {
            Long currentUserId = LoginHelper.getUserId();
            rows = deSysNoticeMapper.delete(
                new LambdaQueryWrapper<DeSysNotice>()
                    .in(DeSysNotice::getNoticeId, java.util.Arrays.asList(noticeIds))
                    .eq(DeSysNotice::getCreateBy, currentUserId));
            if (rows < noticeIds.length) {
                log.warn("[nc.notice.remove] 部分公告删除被拒（非 admin 仅删本人）："
                    + "requested={} deleted={}", noticeIds.length, rows);
            }
        }
        log.info("[nc.notice.remove] ids={} rows={} operator={}",
            (Object[]) noticeIds, rows, LoginHelper.getUserId());
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