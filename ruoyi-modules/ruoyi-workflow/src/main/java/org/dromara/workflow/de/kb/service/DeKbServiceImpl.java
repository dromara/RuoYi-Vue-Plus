package org.dromara.workflow.de.kb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.kb.domain.vo.DeKbWorkspaceVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * KB 域服务实现（KB-01 · P1 档 A 实接）。
 *
 * <p>真实落点 = 底座 {@code sai_rag_document}（DDL 在
 * {@code 04-backend-code/ruoyi-ai/script/sql/ry_ai.sql}）。本仓面<b>未注入</b>
 * sai_rag_document mapper（不动底座、不发明新 mapper）→ KB-01 应用层：
 * <ul>
 *   <li>强校验 documentId 必填且 > 0（N-XX 文档存在性前置）</li>
 *   <li>暴露「我维护 / 共享给我 / 可使用」三档范围接口契约</li>
 *   <li>owner 解锁 sai_rag_document mapper 后填实</li>
 * </ul>
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeKbServiceImpl implements IDeKbService {

    @Override
    public DeKbWorkspaceVo workspace(Long documentId, Long userId) {
        // N-XX 校验：documentId 必填且 > 0
        if (documentId == null || documentId <= 0) {
            throw new ServiceException(
                "KB-01 documentId 非法: " + documentId + "（必须 > 0）",
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        DeKbWorkspaceVo vo = new DeKbWorkspaceVo();
        vo.setDocumentId(documentId);
        vo.setTitle("(sai_rag_document 待 owner 解锁 mapper 后填实)");
        // 三档范围接口契约 —— owner 解锁 sai_rag_document 后填实
        vo.setScopes(new ArrayList<>());
        vo.setSnippets(new ArrayList<>());
        log.info("[kb.workspace] documentId={} userId={} validated=true", documentId, userId);
        return vo;
    }
}