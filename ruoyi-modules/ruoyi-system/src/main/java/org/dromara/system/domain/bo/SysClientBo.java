package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.system.domain.SysClient;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 授权管理业务对象 sys_client
 *
 * @author Michelle.Chung
 * @date 2023-05-15
 */
@Data
@AutoMapper(target = SysClient.class, reverseConvertGenerate = false)
public class SysClientBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @NotNull(message = "id不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * 客户端key
     */
    @NotBlank(message = "客户端key不能为空", groups = { AddGroup.class, EditGroup.class })
    private String clientKey;

    /**
     * 客户端秘钥
     */
    @NotBlank(message = "客户端秘钥不能为空", groups = { AddGroup.class, EditGroup.class })
    private String clientSecret;

    /**
     * 授权类型
     */
    @NotNull(message = "授权类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private List<String> grantTypeList;

    /**
     * 授权类型
     */
    private String grantType;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 允许访问路径
     */
    private String accessPath;

    /**
     * 允许访问路径列表
     */
    private List<String> accessPathList;

    /**
     * IP白名单
     */
    private String ipWhitelist;

    /**
     * IP白名单列表
     */
    private List<String> ipWhitelistList;

    /**
     * token活跃超时时间
     */
    private Long activeTimeout;

    /**
     * token固定超时时间
     */
    private Long timeout;

    /**
     * 状态（0正常 1停用）
     */
    private String status;


}
