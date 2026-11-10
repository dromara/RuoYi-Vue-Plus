package org.dromara.quant.protection.mapper;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Atomic protection intent ownership and short transaction SQL.
 * No external Binance request may execute inside these mapper statements.
 */
@Mapper
public interface ProtectionTargetMapper {

    int insertTarget(@Param("key") String key, @Param("credentialId") Long credentialId, @Param("positionId") Long positionId, @Param("cycleKey") String cycleKey,
                     @Param("side") String side, @Param("type") String type);

    int acquireLease(@Param("key") String key, @Param("owner") String owner);

    List<Integer> confirmedSteps(@Param("credentialId") Long credentialId, @Param("cycleKey") String cycleKey, @Param("side") String side);

    int renewLease(@Param("key") String key, @Param("owner") String owner);

    Long countOwned(@Param("key") String key, @Param("owner") String owner);

    Map<String, Object> getTarget(@Param("key") String key);

    Map<String, Object> lockTarget(@Param("key") String key);

    int prepareTarget(@Param("key") String key, @Param("owner") String owner, @Param("version") long version, @Param("stepIndex") int stepIndex,
                      @Param("quantity") BigDecimal quantity, @Param("triggerPrice") BigDecimal triggerPrice, @Param("orderType") String orderType,
                      @Param("pendingId") Long pendingId, @Param("previousId") Long previousId);

    int confirmTarget(@Param("key") String key, @Param("owner") String owner, @Param("currentId") Long currentId, @Param("stepIndex") int stepIndex,
                      @Param("confirmedStep") int confirmedStep, @Param("quantity") BigDecimal quantity, @Param("triggerPrice") BigDecimal triggerPrice,
                      @Param("orderType") String orderType);

    int clearPending(@Param("key") String key, @Param("owner") String owner);

    int clearPrevious(@Param("key") String key, @Param("owner") String owner);

    int markOrderTerminal(@Param("key") String key, @Param("owner") String owner, @Param("orderId") Long orderId);

    int releaseLease(@Param("key") String key, @Param("owner") String owner);
}
