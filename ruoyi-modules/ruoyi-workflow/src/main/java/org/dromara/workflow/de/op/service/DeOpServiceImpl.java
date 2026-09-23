package org.dromara.workflow.de.op.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.de.domain.SaiAgentUsageStat;
import org.dromara.workflow.de.mapper.SaiAgentUsageStatMapper;
import org.dromara.workflow.de.op.domain.vo.DeOpAlertVo;
import org.dromara.workflow.de.op.domain.vo.DeOpConfigVo;
import org.dromara.workflow.de.op.domain.vo.DeOpMetricsVo;
import org.dromara.workflow.de.op.domain.vo.DeOpMetricsVo.DeOpAgentUsageVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 运营与调度 OP 域服务实现（typed CRUD：ServiceImpl&lt;Mapper, Entity&gt;）。
 *
 * <p>实体 {@link SaiAgentUsageStat}，数据落点 sai_agent_usage_stat
 * （stat_date 窗口聚合；uk_agent_user_date 保证 agent × user × 日单行）。
 *
 * @author AIP
 */
@Slf4j
@Service
public class DeOpServiceImpl
        extends ServiceImpl<SaiAgentUsageStatMapper, SaiAgentUsageStat>
        implements IDeOpService {

    private static final long MILLIS_PER_DAY = 86_400_000L;

    @Override
    public DeOpMetricsVo metrics(Long agentId, Long userId, Integer days) {
        int windowDays = normalizeDays(days);
        Date windowStart = windowStart(windowDays);
        LambdaQueryWrapper<SaiAgentUsageStat> qw = new LambdaQueryWrapper<SaiAgentUsageStat>()
            .ge(SaiAgentUsageStat::getStatDate, windowStart);
        if (agentId != null) {
            qw.eq(SaiAgentUsageStat::getAgentId, agentId);
        }
        if (userId != null) {
            qw.eq(SaiAgentUsageStat::getUserId, userId);
        }
        List<SaiAgentUsageStat> rows = list(qw);

        // 空数据明确无数据（OP-04）：零值 + 空分解，不以运行数冒充业务成果
        Map<Long, int[]> byAgent = new TreeMap<>();
        long totalMessages = 0;
        long totalConversations = 0;
        for (SaiAgentUsageStat r : rows) {
            int messages = r.getMessageCount() != null ? r.getMessageCount() : 0;
            int conversations = r.getConversationCount() != null ? r.getConversationCount() : 0;
            totalMessages += messages;
            totalConversations += conversations;
            int[] acc = byAgent.computeIfAbsent(r.getAgentId(), k -> new int[2]);
            acc[0] += messages;
            acc[1] += conversations;
        }
        DeOpMetricsVo vo = new DeOpMetricsVo();
        vo.setWindowDays(windowDays);
        vo.setWindowStart(windowStart);
        vo.setAgentCount(byAgent.size());
        vo.setTotalMessages(totalMessages);
        vo.setTotalConversations(totalConversations);
        List<DeOpAgentUsageVo> agents = new ArrayList<>(byAgent.size());
        for (Map.Entry<Long, int[]> e : byAgent.entrySet()) {
            DeOpAgentUsageVo a = new DeOpAgentUsageVo();
            a.setAgentId(e.getKey());
            a.setMessageCount(e.getValue()[0]);
            a.setConversationCount(e.getValue()[1]);
            agents.add(a);
        }
        vo.setByAgent(agents);
        return vo;
    }

    @Override
    public List<DeOpAlertVo> alerts(Integer days) {
        int windowDays = normalizeDays(days);
        Date currStart = windowStart(windowDays);
        Date prevStart = new Date(currStart.getTime() - windowDays * MILLIS_PER_DAY);

        // 当前窗口 [currStart, now)，上一窗口 [prevStart, currStart)
        Map<Long, int[]> curr = usageByAgentSince(currStart);
        Map<Long, int[]> prev = usageByAgentBetween(prevStart, currStart);

        List<DeOpAlertVo> out = new ArrayList<>();
        // 1) usage-drop：上一窗口有消息、当前窗口零消息
        for (Map.Entry<Long, int[]> e : prev.entrySet()) {
            int[] c = curr.get(e.getKey());
            boolean currZero = c == null || c[0] == 0;
            if (e.getValue()[0] > 0 && currZero) {
                out.add(buildAlert(e.getKey(), "usage-drop", "high",
                    e.getValue()[0], c == null ? 0 : c[0]));
            }
        }
        // 2) conversation-stalled：当前窗口有消息、零对话轮次
        for (Map.Entry<Long, int[]> e : curr.entrySet()) {
            if (e.getValue()[0] > 0 && e.getValue()[1] == 0) {
                out.add(buildAlert(e.getKey(), "conversation-stalled", "medium",
                    prev.getOrDefault(e.getKey(), new int[2])[0], e.getValue()[0]));
            }
        }
        return out;
    }

    @Override
    public DeOpConfigVo config() {
        DeOpConfigVo vo = new DeOpConfigVo();
        vo.setDefaultWindowDays(7);
        vo.setMetricDimensions(List.of("agent", "user", "stat_date"));
        vo.setTotalRows(count());

        SaiAgentUsageStat min = getOne(
            new LambdaQueryWrapper<SaiAgentUsageStat>()
                .orderByAsc(SaiAgentUsageStat::getStatDate)
                .last("LIMIT 1"));
        SaiAgentUsageStat max = getOne(
            new LambdaQueryWrapper<SaiAgentUsageStat>()
                .orderByDesc(SaiAgentUsageStat::getStatDate)
                .last("LIMIT 1"));
        vo.setMinStatDate(min != null ? min.getStatDate() : null);
        vo.setMaxStatDate(max != null ? max.getStatDate() : null);
        return vo;
    }

    /** 当前窗口聚合：[from, now)。 */
    private Map<Long, int[]> usageByAgentSince(Date from) {
        return usageByAgentBetween(from, null);
    }

    /** 窗口聚合：[from, to)，to 为 null 时开区间到当前。 */
    private Map<Long, int[]> usageByAgentBetween(Date from, Date to) {
        LambdaQueryWrapper<SaiAgentUsageStat> qw = new LambdaQueryWrapper<SaiAgentUsageStat>()
            .ge(SaiAgentUsageStat::getStatDate, from);
        if (to != null) {
            qw.lt(SaiAgentUsageStat::getStatDate, to);
        }
        List<SaiAgentUsageStat> rows = list(qw);
        Map<Long, int[]> byAgent = new TreeMap<>();
        for (SaiAgentUsageStat r : rows) {
            int messages = r.getMessageCount() != null ? r.getMessageCount() : 0;
            int conversations = r.getConversationCount() != null ? r.getConversationCount() : 0;
            int[] acc = byAgent.computeIfAbsent(r.getAgentId(), k -> new int[2]);
            acc[0] += messages;
            acc[1] += conversations;
        }
        return byAgent;
    }

    private DeOpAlertVo buildAlert(Long agentId, String type, String severity,
                                   int prevMessages, int currMessages) {
        DeOpAlertVo alert = new DeOpAlertVo();
        alert.setAgentId(agentId);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setPrevWindowMessages(prevMessages);
        alert.setCurrWindowMessages(currMessages);
        return alert;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return 7;
        }
        return Math.min(days, 365);
    }

    private Date windowStart(int days) {
        return new Date(System.currentTimeMillis() - days * MILLIS_PER_DAY);
    }
}
