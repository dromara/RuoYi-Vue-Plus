package org.dromara.quant.account.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.quant.account.domain.vo.TradeCycleFillRecord;
import org.dromara.quant.account.domain.vo.TradeCycleRecord;
import org.dromara.quant.account.domain.vo.TradeCycleSourceFill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Database access for the durable account-wide position-cycle projection. */
@Mapper
public interface TradeCycleMapper {
    int markDirty(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    List<Map<String, Object>> selectPendingScopes(@Param("limit") int limit);

    Map<String, Object> lockScope(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    long countSourceFills(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    List<TradeCycleSourceFill> selectSourceFills(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    int deleteCycleFills(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    int deleteCycles(@Param("credentialId") Long credentialId, @Param("symbol") String symbol);

    int insertCycle(TradeCycleRecord cycle);

    int insertCycleFill(TradeCycleFillRecord fill);

    int completeProjection(@Param("credentialId") Long credentialId, @Param("symbol") String symbol,
                           @Param("version") long version, @Param("incompleteCount") int incompleteCount,
                           @Param("inferredCount") int inferredCount);

    int markFailed(@Param("credentialId") Long credentialId, @Param("symbol") String symbol, @Param("reason") String reason);

    long countCycles(@Param("ids") List<Long> ids, @Param("symbol") String symbol,
                     @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Map<String, Object>> selectCycles(@Param("ids") List<Long> ids, @Param("symbol") String symbol,
                                           @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                           @Param("offset") long offset, @Param("pageSize") int pageSize);

    int countIncompleteScopes(@Param("ids") List<Long> ids, @Param("symbol") String symbol);

    int countInferredCycles(@Param("ids") List<Long> ids, @Param("symbol") String symbol,
                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
