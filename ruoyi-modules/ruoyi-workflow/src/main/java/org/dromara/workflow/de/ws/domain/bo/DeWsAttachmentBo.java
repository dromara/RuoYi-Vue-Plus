package org.dromara.workflow.de.ws.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * WS-02 节点附件选择 BO。
 *
 * @author AIP
 */
@Data
public class DeWsAttachmentBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long nodeId;

    @NotNull
    private Long ossId;

    /** 引用文本（落 flow_node.ext JSON） */
    private String reference;
}