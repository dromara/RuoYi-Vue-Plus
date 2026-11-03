package org.dromara.quant.config;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * MySQL/InnoDB metadata only. These queries never mutate application schema.
 */
@Mapper
public interface LiveSchemaContractMapper {

    List<String> innodbTables();

    List<String> uniqueContracts();

    List<String> leaseEpochColumns();

    List<String> entryGuardColumns();

    List<String> protectionOwnerColumns();

    List<String> positionReturnRateColumns();

    List<String> orderBalanceSnapshotColumns();

    List<String> positionCycleKeyColumns();

}
