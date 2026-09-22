package org.dromara.workflow.de.ad.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AD-07 资产交接一致性预览 BO。
 *
 * @author AIP
 */
@Data
public class DeAdHandoverBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long userId;

    /** 资源 ID 列表 */
    private List<Long> resourceIds;

    /** 资产 ID 列表 */
    private List<Long> assetIds;
}