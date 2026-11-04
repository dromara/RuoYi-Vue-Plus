package org.dromara.quant.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.quant.account.domain.vo.TradeCyclePageVo;
import org.dromara.quant.account.mapper.TradeCycleMapper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-only paged query over the persisted trade-cycle projection. */
@Service
@RequiredArgsConstructor
public class TradeCycleQueryService {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");

    private final QuantCredentialMapper credentialMapper;
    private final TradeCycleMapper cycleMapper;

    public TradeCyclePageVo page(PageQuery query, Long credentialId, String symbol, String from, String to) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录后查询交易记录");
        }
        List<QuantCredential> credentials = credentialMapper.selectList(new LambdaQueryWrapper<QuantCredential>()
            .select(QuantCredential::getId, QuantCredential::getUserId, QuantCredential::getAccountName)
            .eq(!LoginHelper.isSuperAdmin(userId), QuantCredential::getUserId, userId));
        Map<Long, String> names = credentials.stream()
            .collect(Collectors.toMap(QuantCredential::getId, QuantCredential::getAccountName));
        if (credentialId != null && !names.containsKey(credentialId)) {
            throw new ServiceException("无权访问该交易账户");
        }

        List<Long> credentialIds = credentialId == null
            ? credentials.stream().map(QuantCredential::getId).toList()
            : List.of(credentialId);
        if (credentialIds.isEmpty()) {
            return new TradeCyclePageVo(List.of(), 0, 0, 0);
        }

        String normalizedSymbol = symbol == null || symbol.isBlank()
            ? null : symbol.trim().toUpperCase(Locale.ROOT);
        if (normalizedSymbol != null && !normalizedSymbol.matches("[A-Z0-9]{2,32}")) {
            throw new ServiceException("交易对格式无效");
        }
        LocalDateTime start = boundary(from, false);
        LocalDateTime end = boundary(to, true);
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServiceException("开始日期不能晚于结束日期");
        }

        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 10 : Math.min(100, Math.max(1, query.getPageSize()));
        long total = cycleMapper.countCycles(credentialIds, normalizedSymbol, start, end);
        int incompleteCount = cycleMapper.countIncompleteScopes(credentialIds, normalizedSymbol);
        int inferredCount = cycleMapper.countInferredCycles(credentialIds, normalizedSymbol, start, end);
        long offset = (long) (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = total == 0 ? List.of()
            : cycleMapper.selectCycles(credentialIds, normalizedSymbol, start, end, offset, pageSize);
        return new TradeCyclePageVo(rows == null ? List.of() : rows, total, incompleteCount, inferredCount);
    }

    private static LocalDateTime boundary(String date, boolean nextDay) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate parsed = LocalDate.parse(date);
            return LocalDateTime.ofInstant(
                (nextDay ? parsed.plusDays(1) : parsed).atStartOfDay(DISPLAY_ZONE).toInstant(), ZoneOffset.UTC);
        } catch (java.time.DateTimeException ex) {
            throw new ServiceException("日期格式应为 yyyy-MM-dd");
        }
    }
}
