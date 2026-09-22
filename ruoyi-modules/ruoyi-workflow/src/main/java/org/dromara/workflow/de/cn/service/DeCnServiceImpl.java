package org.dromara.workflow.de.cn.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CN 域服务实现（CN-03）。
 *
 * <p>本类面向 sai_mcp_server / sai_agent_mcp_server（DDL 表），
 * 本仓**未建实体 / mapper**。CN-03 真源 = 业务表单替代工具 ID；
 * 本类仅暴露端点形状 + 占位。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeCnServiceImpl implements IDeCnService {

    @Override
    public List<Map<String, Object>> selectors(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> placeholder = new HashMap<>();
        placeholder.put("id", 0L);
        placeholder.put("label", "(连接器选择器 —— 占位)");
        placeholder.put("transport", "http/mcp");
        placeholder.put("visibility", userId == null ? "public" : "user");
        out.add(placeholder);
        return out;
    }
}