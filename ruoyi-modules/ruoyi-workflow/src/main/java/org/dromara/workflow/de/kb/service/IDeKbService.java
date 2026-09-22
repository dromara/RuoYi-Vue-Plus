package org.dromara.workflow.de.kb.service;

import org.dromara.workflow.de.kb.domain.vo.DeKbWorkspaceVo;

/**
 * KB 域服务接口 —— P1 档 A：KB-01 共 1 项。
 *
 * @author AIP
 */
public interface IDeKbService {

    /** KB-01 知识详情工作区 + 「我维护 / 共享给我 / 可使用」范围 */
    DeKbWorkspaceVo workspace(Long documentId, Long userId);
}