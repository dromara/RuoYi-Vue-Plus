package org.dromara.workflow.de.ws.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * WS-01 工作空间绑定节点 BO。
 *
 * @author AIP
 */
@Data
public class DeWsBindBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点 ID */
    @NotNull
    private Long nodeId;

    /** 工作空间类型：personal / team / business */
    @NotBlank
    private String wsType;

    /** 工作空间 ID */
    @NotNull
    private Long workspaceId;
}