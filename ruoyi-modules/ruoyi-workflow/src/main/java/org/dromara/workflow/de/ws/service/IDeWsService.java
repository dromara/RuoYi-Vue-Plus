package org.dromara.workflow.de.ws.service;

import org.dromara.workflow.de.ws.domain.bo.DeWsAttachmentBo;
import org.dromara.workflow.de.ws.domain.bo.DeWsBindBo;
import org.dromara.workflow.de.ws.domain.bo.DeWsPermissionsBo;

/**
 * WS 域服务接口 —— P1 档 A：WS-01 / WS-02 / WS-04 共 3 项。
 *
 * @author AIP
 */
public interface IDeWsService {

    /** WS-01 工作空间绑定节点 */
    Long bind(DeWsBindBo bo);

    /** WS-02 节点附件 */
    Long attachment(DeWsAttachmentBo bo);

    /** WS-04 文件读写 / 网络 / 命令权限 */
    Long permissions(DeWsPermissionsBo bo);
}