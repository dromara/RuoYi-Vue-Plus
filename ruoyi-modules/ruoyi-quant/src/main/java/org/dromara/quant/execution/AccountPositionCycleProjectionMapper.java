package org.dromara.quant.execution;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.quant.account.domain.vo.AccountPositionCycleFill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 账户级成交周期投影的持久化边界。
 */
@Mapper
public interface AccountPositionCycleProjectionMapper {
    List<String> selectPositionSides(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    List<AccountPositionCycleFill> selectAllFills(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    List<AccountPositionCycleFill> selectUnprojectedFills(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    List<String> selectProjectionBlockedSymbols(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    int deleteState(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("positionSide") String positionSide);

    int refreshBalanceSnapshots(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    int ensureState(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("positionSide") String positionSide);

    Map<String, Object> lockState(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("positionSide") String positionSide);

    int updateState(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("positionSide") String positionSide,
                    @Param("quantity") BigDecimal quantity, @Param("activeCycleId") Long activeCycleId,
                    @Param("lastTradeTime") LocalDateTime lastTradeTime, @Param("lastTradeId") Long lastTradeId,
                    @Param("projectedThrough") LocalDateTime projectedThrough, @Param("status") String status, @Param("incomplete") boolean incomplete, @Param("reason") String reason);

    int insertCycle(@Param("id") Long id, @Param("cycleKey") String cycleKey, @Param("credentialId") Long credentialId,
                    @Param("symbol") String symbol, @Param("positionSide") String positionSide, @Param("direction") String direction,
                    @Param("status") String status, @Param("baselineUnknown") boolean baselineUnknown,
                    @Param("openingQuantity") BigDecimal openingQuantity, @Param("remainingQuantity") BigDecimal remainingQuantity,
                    @Param("openingFillCount") int openingFillCount, @Param("openingNotional") BigDecimal openingNotional, @Param("openedAt") LocalDateTime openedAt,
                    @Param("leverage") Integer leverage, @Param("tickSize") BigDecimal tickSize,
                    @Param("openingBalance") BigDecimal openingBalance, @Param("openingBalanceAt") LocalDateTime openingBalanceAt);

    int updateOpenCycle(@Param("id") Long id, @Param("quantity") BigDecimal quantity, @Param("notional") BigDecimal notional,
                        @Param("leverage") Integer leverage, @Param("tickSize") BigDecimal tickSize,
                        @Param("balance") BigDecimal balance, @Param("balanceAt") LocalDateTime balanceAt);

    Map<String, Object> selectCycleState(@Param("id") Long id);

    int updateCloseCycle(@Param("id") Long id, @Param("quantity") BigDecimal quantity, @Param("notional") BigDecimal notional,
                         @Param("remainingQuantity") BigDecimal remainingQuantity, @Param("realizedPnl") BigDecimal realizedPnl,
                         @Param("status") String status, @Param("closedAt") LocalDateTime closedAt,
                         @Param("balance") BigDecimal balance, @Param("balanceAt") LocalDateTime balanceAt);

    int insertAllocation(@Param("id") Long id, @Param("credentialId") Long credentialId, @Param("symbol") String symbol,
                         @Param("tradeId") Long tradeId, @Param("orderId") Long orderId, @Param("cycleId") Long cycleId,
                         @Param("role") String role, @Param("quantity") BigDecimal quantity, @Param("price") BigDecimal price,
                         @Param("commission") BigDecimal commission, @Param("commissionAsset") String commissionAsset,
                         @Param("realizedPnl") BigDecimal realizedPnl, @Param("tradeTime") LocalDateTime tradeTime);

    List<Map<String, Object>> selectCycles(@Param("credentialIds") List<Long> credentialIds, @Param("symbol") String symbol,
                                           @Param("fromUtc") LocalDateTime fromUtc, @Param("toUtc") LocalDateTime toUtc,
                                           @Param("offset") long offset, @Param("limit") int limit);

    long countCycles(@Param("credentialIds") List<Long> credentialIds, @Param("symbol") String symbol,
                     @Param("fromUtc") LocalDateTime fromUtc, @Param("toUtc") LocalDateTime toUtc);

    long countIncompleteScopes(@Param("credentialIds") List<Long> credentialIds, @Param("symbol") String symbol);

    int deleteCycleAllocationsByScope(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("positionSide") String positionSide);

    int deleteCyclesByScope(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("positionSide") String positionSide);

    List<Map<String, Object>> selectCycleAllocations(@Param("cycleIds") List<Long> cycleIds);

    List<Map<String, Object>> selectCycleFeeTotals(@Param("cycleIds") List<Long> cycleIds);
}
