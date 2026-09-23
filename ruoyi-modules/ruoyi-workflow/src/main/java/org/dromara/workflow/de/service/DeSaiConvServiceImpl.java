package org.dromara.workflow.de.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.de.domain.SaiAgentConversation;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.domain.bo.SaiAgentConversationBo;
import org.dromara.workflow.de.domain.vo.SaiAgentConversationVo;
import org.dromara.workflow.de.mapper.SaiAgentConversationMapper;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 智能体对话服务实现（sai_agent_conversation + sai_agent_conversation_record）。
 *
 * <p>主表 {@code sai_agent_conversation} 提供 CRUD + 分页 + 按 agent_id 查询；
 * 关联只读表 {@code sai_agent_conversation_record} 提供按 record_id 单条查询。
 *
 * <p><b>AU-09 应用层派生（D-002 已登记）保留说明</b>：
 * {@link SaiAgentConversationMapper} 类 Javadoc 标注本 mapper「只读」—— 该口径指
 * {@code DeBindingServiceImpl.getBinding(Long)} 的派生读路径
 * （{@code create_dt DESC LIMIT 1} 取最近一条 title 回填 {@code DeBindingVo.title}）。
 * 本 controller / service 暴露的标准 CRUD 端点是<b>独立读写接口</b>，与该派生读
 * 不相互替代 —— 前者用于会话管理 UI，后者用于绑定回填。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSaiConvServiceImpl implements IDeSaiConvService {

    /**
     * ★ S-05 修复（2026-09-22 安全审计）：本模块与 de-conversation 平行写同一张
     * sai_agent_conversation 表，且历史上 userId 由客户端提交、edit/remove/get 无属主校验
     * ——一旦 saiconv 权限被授给普通角色即成活的跨用户 IDOR（CWE-639）。
     * 修复模型 = **超管全通（保留管理用途）· 非超管强制属主**：
     * add 的 userId 一律取当前登录人（客户端值作废）；edit/remove/get 逐行验属主且
     * 禁止转移属主；list/listByAgentId 对非超管强制按本人过滤（覆盖客户端过滤值）。
     */
    private final SaiAgentConversationMapper saiAgentConversationMapper;
    private final DeAccessService deAccessService;
    private final SaiAgentConversationRecordMapper saiAgentConversationRecordMapper;

    @Override
    public Long add(SaiAgentConversationBo bo) {
        if (bo == null) {
            throw new IllegalArgumentException("SaiAgentConversationBo 不能为空");
        }
        SaiAgentConversation entity = BeanUtil.copyProperties(bo, SaiAgentConversation.class);
        // S-05：属主一律 = 创建者，客户端提交的 userId 作废（防冒名落库）
        entity.setUserId(LoginHelper.getUserId());
        saiAgentConversationMapper.insert(entity);
        log.info("[de.sai-conv] add id={} agentId={} userId={} conversationId={}",
            entity.getId(), entity.getAgentId(), entity.getUserId(), entity.getConversationId());
        return entity.getId();
    }

    @Override
    public Boolean edit(SaiAgentConversationBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new IllegalArgumentException("SaiAgentConversationBo.id 不能为空");
        }
        SaiAgentConversation existing = saiAgentConversationMapper.selectById(bo.getId());
        checkOwnedOrAdmin(existing, "edit");
        SaiAgentConversation entity = BeanUtil.copyProperties(bo, SaiAgentConversation.class);
        // S-05：属主不可被更新转移
        entity.setUserId(existing.getUserId());
        int rows = saiAgentConversationMapper.updateById(entity);
        log.info("[de.sai-conv] edit id={} rows={}", bo.getId(), rows);
        return rows > 0;
    }

    @Override
    public Boolean remove(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            checkOwnedOrAdmin(saiAgentConversationMapper.selectById(id), "remove");
        }
        int rows = saiAgentConversationMapper.deleteByIds(ids);
        log.info("[de.sai-conv] remove ids={} rows={}", ids, rows);
        return rows > 0;
    }

    @Override
    public SaiAgentConversationVo getById(Long id) {
        if (id == null) {
            return null;
        }
        SaiAgentConversation row = saiAgentConversationMapper.selectById(id);
        if (row == null) {
            return null;
        }
        checkOwnedOrAdmin(row, "get");
        return BeanUtil.copyProperties(row, SaiAgentConversationVo.class);
    }

    @Override
    public PageResult<SaiAgentConversationVo> list(SaiAgentConversationBo bo) {
        if (bo == null) {
            bo = new SaiAgentConversationBo();
        }
        LambdaQueryWrapper<SaiAgentConversation> wrapper = new LambdaQueryWrapper<>();
        if (bo.getAgentId() != null) {
            wrapper.eq(SaiAgentConversation::getAgentId, bo.getAgentId());
        }
        if (!LoginHelper.isSuperAdmin()) {
            // S-05：非超管一律按本人过滤（客户端 userId 过滤值作废）
            wrapper.eq(SaiAgentConversation::getUserId, LoginHelper.getUserId());
        } else if (bo.getUserId() != null) {
            wrapper.eq(SaiAgentConversation::getUserId, bo.getUserId());
        }
        if (StrUtil.isNotBlank(bo.getConversationId())) {
            wrapper.eq(SaiAgentConversation::getConversationId, bo.getConversationId());
        }
        if (StrUtil.isNotBlank(bo.getTitle())) {
            wrapper.like(SaiAgentConversation::getTitle, bo.getTitle());
        }
        wrapper.orderByDesc(SaiAgentConversation::getCreateDt);

        IPage<SaiAgentConversation> page = saiAgentConversationMapper.selectPage(bo.build(), wrapper);
        List<SaiAgentConversationVo> rows = new ArrayList<>();
        if (page.getRecords() != null) {
            for (SaiAgentConversation r : page.getRecords()) {
                rows.add(BeanUtil.copyProperties(r, SaiAgentConversationVo.class));
            }
        }
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public List<SaiAgentConversationVo> listByAgentId(Long agentId) {
        if (agentId == null) {
            return new ArrayList<>();
        }
        List<SaiAgentConversation> rows = saiAgentConversationMapper.selectList(
            new LambdaQueryWrapper<SaiAgentConversation>()
                .eq(SaiAgentConversation::getAgentId, agentId)
                .orderByDesc(SaiAgentConversation::getCreateDt)
        );
        List<SaiAgentConversationVo> out = new ArrayList<>();
        if (rows != null) {
            for (SaiAgentConversation r : rows) {
                out.add(BeanUtil.copyProperties(r, SaiAgentConversationVo.class));
            }
        }
        return out;
    }

    @Override
    public SaiAgentConversationRecord getRecordById(Long recordId) {
        if (recordId == null) {
            return null;
        }
        SaiAgentConversationRecord record = saiAgentConversationRecordMapper.selectById(recordId);
        if (record == null) {
            return null;
        }
        // ★ AU-03 方案 A（2026-09-22 顺序推进批③）：S-05 属主 **OR** T-3 双条件
        //   （任务关系 + 实例内）—— 经 DeAccessService 统一判定；超管仍全通。
        if (!LoginHelper.isSuperAdmin() && !deAccessService.canReadConversationRecord(record)) {
            throw new ServiceException("无权读取该对话记录（属主/T-3 双条件均不满足）");
        }
        return record;
    }

    /**
     * S-05 属主校验：null 行放行（由调用方的 0 行语义处理）；超管放行；
     * 非超管且行属主 != 当前登录人 ⇒ 拒绝。
     */
    private void checkOwnedOrAdmin(SaiAgentConversation row, String action) {
        if (row == null || LoginHelper.isSuperAdmin()) {
            return;
        }
        Long current = LoginHelper.getUserId();
        if (row.getUserId() == null || !row.getUserId().equals(current)) {
            throw new ServiceException("无权" + action + "他人名下的对话（S-05 属主校验）");
        }
    }
}