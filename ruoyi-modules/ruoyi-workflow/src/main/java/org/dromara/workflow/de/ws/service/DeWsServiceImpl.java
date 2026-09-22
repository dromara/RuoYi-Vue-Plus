package org.dromara.workflow.de.ws.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeErrorCode;
import org.dromara.workflow.de.ws.domain.bo.DeWsAttachmentBo;
import org.dromara.workflow.de.ws.domain.bo.DeWsBindBo;
import org.dromara.workflow.de.ws.domain.bo.DeWsPermissionsBo;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WS 域服务实现。
 *
 * <p>落点：flow_node.ext（varchar 2000）—— 工作空间 / 附件 / 权限 配置统一入此列；
 * 三种信息以 JSON 形式共存，按 key 区分。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeWsServiceImpl implements IDeWsService {

    private final FlowNodeMapper flowNodeMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Long bind(DeWsBindBo bo) {
        FlowNode node = mustNode(bo.getNodeId());
        Map<String, Object> ext = parse(node.getExt());
        ext.put("workspaceBinding", Map.of(
            "wsType", bo.getWsType(),
            "workspaceId", bo.getWorkspaceId()
        ));
        node.setExt(toJson(ext));
        flowNodeMapper.updateById(node);
        log.info("[ws.bind] nodeId={} wsType={} workspaceId={}",
            bo.getNodeId(), bo.getWsType(), bo.getWorkspaceId());
        return node.getId();
    }

    @Override
    public Long attachment(DeWsAttachmentBo bo) {
        FlowNode node = mustNode(bo.getNodeId());
        Map<String, Object> ext = parse(node.getExt());
        ext.put("attachment", Map.of(
            "ossId", bo.getOssId(),
            "reference", bo.getReference() == null ? "" : bo.getReference()
        ));
        node.setExt(toJson(ext));
        flowNodeMapper.updateById(node);
        log.info("[ws.attachment] nodeId={} ossId={}", bo.getNodeId(), bo.getOssId());
        return node.getId();
    }

    @Override
    public Long permissions(DeWsPermissionsBo bo) {
        FlowNode node = mustNode(bo.getNodeId());
        Map<String, Object> ext = parse(node.getExt());
        ext.put("execPermissions", Map.of(
            "fileReadPaths", bo.getFileReadPaths() == null ? java.util.List.of() : bo.getFileReadPaths(),
            "fileWritePaths", bo.getFileWritePaths() == null ? java.util.List.of() : bo.getFileWritePaths(),
            "networkDomains", bo.getNetworkDomains() == null ? java.util.List.of() : bo.getNetworkDomains(),
            "commands", bo.getCommands() == null ? java.util.List.of() : bo.getCommands()
        ));
        node.setExt(toJson(ext));
        flowNodeMapper.updateById(node);
        log.info("[ws.permissions] nodeId={}", bo.getNodeId());
        return node.getId();
    }

    private FlowNode mustNode(Long nodeId) {
        FlowNode n = flowNodeMapper.selectById(nodeId);
        if (n == null) {
            throw new ServiceException("节点不存在: id=" + nodeId);
        }
        return n;
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            // 2026-09-19 H1 P1 #3: 静默返空 HashMap 会让后续 ext.put(...) 把现有合法字段全部抹平
            // 改为显式抛错，避免静默写覆盖
            log.error("[ws.parse] flow_node.ext 解析失败: {}", e.getMessage());
            throw new ServiceException(
                DeErrorCode.DE_NODE_EXT_INVALID_JSON.format(e.getMessage()),
                DeErrorCode.DE_NODE_EXT_INVALID_JSON.getCode());
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            // 2026-09-19 H1 P1 #2: 静默返 "{}" 会让 flow_node.ext 被覆盖成空对象，导致下游读全空
            // 改为显式抛错，避免写路径吞数据
            log.error("[ws.toJson] flow_node.ext 序列化失败: {}", e.getMessage());
            throw new ServiceException(
                DeErrorCode.DE_NODE_EXT_SERIALIZE_FAILED.format(e.getMessage()),
                DeErrorCode.DE_NODE_EXT_SERIALIZE_FAILED.getCode());
        }
    }
}