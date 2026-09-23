package org.dromara.workflow.de.cn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.dromara.workflow.de.domain.SaiMcpServer;
import org.dromara.workflow.de.mapper.SaiMcpServerMapper;

import java.util.Map;

/**
 * CN 域服务实现（CN-03）。
 *
 * <p>★ 2026-09-23 接线（D-013 · 审计漂移#1 正解）：SaiMcpServer 实体与
 * SaiMcpServerMapper 已存在（批⑥ typed 化）——原「未建实体/mapper」javadoc 已过期，
 * 本类改为直接消费该 mapper（复用既有，不造第二条数据路径）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeCnServiceImpl implements IDeCnService {

    private final SaiMcpServerMapper saiMcpServerMapper;

    @Override
    public List<Map<String, Object>> selectors(Long userId) {
        // D-013 接线：真查 sai_mcp_server
        List<SaiMcpServer> servers = saiMcpServerMapper.selectList(null);
        List<Map<String, Object>> out = new ArrayList<>();
        for (SaiMcpServer sv : servers) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", sv.getId());
            row.put("label", sv.getName());
            row.put("transport", sv.getTransportType());
            row.put("lastConnectDt", sv.getLastConnectDt());
            row.put("visibility", userId == null ? "public" : "user");
            out.add(row);
        }
        log.info("[cn.selectors] userId={} servers={}", userId, out.size());
        return out;
    }
}