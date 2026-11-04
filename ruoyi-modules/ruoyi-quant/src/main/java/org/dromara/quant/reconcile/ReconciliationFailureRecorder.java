package org.dromara.quant.reconcile;

import lombok.RequiredArgsConstructor;
import org.dromara.quant.reconcile.domain.QuantReconcileRecord;
import org.dromara.quant.reconcile.mapper.QuantReconcileRecordMapper;
import org.dromara.quant.util.QuantJsonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 独立事务写入对账失败事实，避免外层回滚事务吞掉失败记录。
 */
@RequiredArgsConstructor
@Service
public class ReconciliationFailureRecorder {
    private final QuantReconcileRecordMapper mapper;

    public void record(Long credentialId, String message) {
        QuantReconcileRecord record = new QuantReconcileRecord();
        record.setCredentialId(credentialId);
        record.setReconcileType("FULL");
        record.setResult("FAILED");
        record.setReconciledAt(LocalDateTime.now(ZoneOffset.UTC));
        String safeMessage = message == null ? "" : message.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "?");
        try {
            record.setDetailJson(QuantJsonMapper.writeMap(Map.of("message", safeMessage)));
        } catch (Exception ex) {
            record.setDetailJson("{\"message\":\"对账失败\"}");
        }
        mapper.insert(record);
    }
}
