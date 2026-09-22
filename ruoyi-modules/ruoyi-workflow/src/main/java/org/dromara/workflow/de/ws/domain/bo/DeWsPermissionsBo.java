package org.dromara.workflow.de.ws.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * WS-04 文件读写 / 外部网络 / 命令权限 BO。
 *
 * @author AIP
 */
@Data
public class DeWsPermissionsBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long nodeId;

    /** 允许的文件路径前缀列表 */
    private List<String> fileReadPaths;
    private List<String> fileWritePaths;

    /** 允许的外部网络域 */
    private List<String> networkDomains;

    /** 允许的命令白名单 */
    private List<String> commands;
}