package org.dromara.job.entity;

import java.math.BigDecimal;

/**
 * 账单数据传输对象。
 *
 * @param billId      账单 ID
 * @param billChannel 账单渠道
 * @param billDate    账单日期
 * @param billAmount  账单金额
 */
public record BillDTO(
    Long billId,
    String billChannel,
    String billDate,
    BigDecimal billAmount
) {
}
