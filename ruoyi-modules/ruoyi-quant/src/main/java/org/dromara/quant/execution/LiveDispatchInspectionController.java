package org.dromara.quant.execution;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.quant.execution.mapper.LiveOrderDispatchMapper;
import org.dromara.quant.protection.ProtectionOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Operators can inspect quarantined outbound orders without unsafe manual unlock.
 * This endpoint deliberately has no write action. All exchange/snowflake IDs are
 * represented as strings to avoid JavaScript IEEE-754 precision loss.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/quant/infrastructure/dispatch")
public class LiveDispatchInspectionController {
    private final LiveOrderDispatchMapper mapper;
    private final ProtectionOrderService protectionOrders;

    private static String value(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }

    /**
     * 查询尚未完成确认的 LIVE 订单派发摘要。
     */
    @GetMapping("/unresolved")
    @SaCheckPermission("quant:dispatch:view")
    public R<List<BlockedDispatch>> unresolved() {
        return R.ok(mapper.unresolvedDispatches().stream().map(row -> new BlockedDispatch(value(row, "credential_id"), value(row, "symbol"), value(row, "dispatch_key"),
            value(row, "operation_type"), value(row, "client_order_id"), value(row, "dispatch_status"), value(row, "exchange_order_id"), value(row, "strategy_instance_id"),
            value(row, "lease_epoch"), value(row, "blocked_at"))).toList());
    }

    /**
     * 按页面提交的完整票据身份，人工触发一次保护单 Binance 事实核验。
     * 只允许 PROTECT_CREATE / PROTECT_CANCEL；核验失败时不释放派发门禁。
     */
    @PostMapping("/recheck")
    @SaCheckPermission("quant:dispatch:recover")
    public R<Boolean> recheck(@RequestBody RecheckRequest body) {
        long credentialId = parseId(body.credentialId());
        boolean recovered = protectionOrders.recheckProtectionDispatch(
            credentialId, body.symbol(), body.dispatchKey(), body.clientOrderId());
        return R.ok(recovered);
    }

    private static long parseId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("Positive decimal credential ID string required");
        }
        return Long.parseLong(value);
    }

    public record RecheckRequest(String credentialId, String symbol, String dispatchKey, String clientOrderId) {
    }

    /**
     * 可安全展示的阻塞派发摘要，不包含凭证密钥或签名内容。
     */
    public record BlockedDispatch(String credentialId, String symbol, String dispatchKey, String operation, String clientOrderId, String status, String exchangeOrderId,
                                  String strategyInstanceId, String leaseEpoch, String blockedAt) {
    }
}
