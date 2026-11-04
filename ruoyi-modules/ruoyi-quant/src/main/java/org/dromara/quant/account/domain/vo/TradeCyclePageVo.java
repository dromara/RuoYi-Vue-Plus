package org.dromara.quant.account.domain.vo;

import lombok.Getter;
import org.dromara.common.core.domain.PageResult;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/** Paged account-wide position-cycle records and reconstruction diagnostics. */
@Getter
public class TradeCyclePageVo extends PageResult<Map<String, Object>> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int incompleteCount;
    private final int inferredCount;

    public TradeCyclePageVo(List<Map<String, Object>> rows, long total, int incompleteCount, int inferredCount) {
        super(rows, total);
        this.incompleteCount = incompleteCount;
        this.inferredCount = inferredCount;
    }
}
