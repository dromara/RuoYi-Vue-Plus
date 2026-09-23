package org.dromara.workflow.de.kb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.domain.SaiRagDocument;
import org.dromara.workflow.de.mapper.SaiRagDocumentMapper;
import org.dromara.workflow.de.kb.domain.vo.DeKbWorkspaceVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * KB 域服务实现（KB-01 · P1 档 A 实接）。
 *
 * <p>真实落点 = sai_rag_document 表。★ 2026-09-23 接线（D-013 · 审计漂移#1 正解）：
 * SaiRagDocumentMapper 已存在（批④ typed 化），原「未注入 mapper」javadoc 已过期——
 * 本类改为直接消费该 mapper（复用既有，不造第二条数据路径）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeKbServiceImpl implements IDeKbService {

    private final SaiRagDocumentMapper saiRagDocumentMapper;

    @Override
    public DeKbWorkspaceVo workspace(Long documentId, Long userId) {
        // N-XX 校验：documentId 必填且 > 0
        if (documentId == null || documentId <= 0) {
            throw new ServiceException(
                "KB-01 documentId 非法: " + documentId + "（必须 > 0）",
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        // D-013 接线：真查 sai_rag_document
        SaiRagDocument doc = saiRagDocumentMapper.selectById(documentId);
        if (doc == null) {
            throw new ServiceException("KB-01 文档不存在: " + documentId,
                DeErrorCode.DE_AGENT_NOT_FOUND.getCode());
        }
        DeKbWorkspaceVo vo = new DeKbWorkspaceVo();
        vo.setDocumentId(doc.getId());
        vo.setTitle(doc.getName());
        vo.setScopes(new ArrayList<>());
        vo.setSnippets(new ArrayList<>());
        log.info("[kb.workspace] documentId={} userId={} name={}", documentId, userId, doc.getName());
        return vo;
    }
}