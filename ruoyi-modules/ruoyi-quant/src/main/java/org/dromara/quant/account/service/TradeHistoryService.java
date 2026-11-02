package org.dromara.quant.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.account.TradeHistoryAssembler;
import org.dromara.quant.account.domain.vo.TradeHistoryFill;
import org.dromara.quant.account.domain.vo.TradeHistoryPageVo;
import org.dromara.quant.account.mapper.TradeHistoryMapper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 开平仓记录查询服务，仅访问本地账本，不参与策略决策和交易所下单。
 * 时间范围按北京时间的开仓日期筛选，完整成交周期汇总之后再分页。
 */
@Service
@RequiredArgsConstructor
public class TradeHistoryService {
    private final QuantCredentialMapper credentialMapper;
    private final TradeHistoryMapper historyMapper;

    private static Instant boundary(String date, boolean nextDay) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(date);
            return (nextDay ? parsed.plusDays(1) : parsed).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant();
        } catch (java.time.DateTimeException ex) {
            throw new ServiceException("日期格式应为 yyyy-MM-dd");
        }
    }

    /**
     * 查询当前用户有权访问的客户开平仓记录。
     */
    @Transactional(readOnly = true)
    public TradeHistoryPageVo page(PageQuery query, Long credentialId, String symbol, String from, String to) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录后查询开平仓记录");
        }
        List<QuantCredential> credentials = credentialMapper.selectList(new LambdaQueryWrapper<QuantCredential>().select(QuantCredential::getId, QuantCredential::getUserId,
            QuantCredential::getAccountName).eq(!LoginHelper.isSuperAdmin(userId), QuantCredential::getUserId, userId));
        Map<Long, String> names = credentials.stream().collect(Collectors.toMap(QuantCredential::getId, QuantCredential::getAccountName));
        if (credentialId != null && !names.containsKey(credentialId)) {
            throw new ServiceException("无权访问该交易账户");
        }
        List<Long> ids = credentialId == null ? new ArrayList<>(names.keySet()) : List.of(credentialId);
        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        Instant start = boundary(from, false), end = boundary(to, true);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }
        String normalizedSymbol = symbol == null || symbol.isBlank() ? null : symbol.trim().toUpperCase(Locale.ROOT);
        var fills = ids.isEmpty() ? List.<TradeHistoryFill>of() : historyMapper.selectFills(ids, normalizedSymbol);
        if (fills.size() > 20000) {
            throw new ServiceException("成交账本超过单次汇总上限，请选择客户及交易对后查询，避免截断持仓链");
        }
        // SQL 关联若出现多对多，拒绝重复计入成交，不能用 DISTINCT 隐藏来源歧义。
        if (fills.stream().map(f -> f.getId()).distinct().count() != fills.size()) {
            throw new ServiceException("成交来源关联重复，暂不能可靠汇总开平仓记录");
        }
        var result = TradeHistoryAssembler.assemble(fills.stream().map(f -> new TradeHistoryAssembler.Fill(f.getId(), f.getCredentialId(), f.getSymbol(), f.getSide(),
            f.getPositionSide(), f.getOperationType(), f.getReduceOnly(), f.getQuantity(), f.getPrice(), f.getCommission(), f.getRealizedPnl(), f.getTradeTime(),
            f.getLeverageSnapshot(), f.getPriceTickSizeSnapshot(), f.getPostFillWalletBalance(), f.getPostFillBalanceAt(), f.getCurrentPriceTickSize(), f.getExchangeTradeId(),
            f.getExchangeOrderId())).toList(), names);
        var rows = result.rows().stream().filter(row -> {
            Instant opened = Instant.parse((String) row.get("openedAt"));
            return (start == null || !opened.isBefore(start)) && (end == null || opened.isBefore(end));
        }).toList();
        long offset = (long) (pageNum - 1) * pageSize;
        int first = (int) Math.min(offset, rows.size());
        return new TradeHistoryPageVo(rows.subList(first, Math.min(first + pageSize, rows.size())), rows.size(), result.incompleteCount());
    }
}
