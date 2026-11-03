package org.dromara.quant.account.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.quant.account.domain.vo.TradeHistoryFill;

import java.util.List;

/**
 * 客户隔离后的只读成交周期查询，不调用交易所。
 */
public interface TradeHistoryMapper {

    /**
     * 按成交账本读取完整成交链，并关联实际订单和执行归因事实。
     */
    List<TradeHistoryFill> selectFills(@Param("ids") List<Long> ids, @Param("symbol") String symbol);

    List<Long> selectOrderIdsMissingOrderFacts(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("limit") int limit);
}
