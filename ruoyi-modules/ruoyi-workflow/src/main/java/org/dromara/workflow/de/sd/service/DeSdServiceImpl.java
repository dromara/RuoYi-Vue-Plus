package org.dromara.workflow.de.sd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SD 域服务实现。
 *
 * <p>落点：flow_definition.ext / flow_node.ext（双层 JSON 形状）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSdServiceImpl implements IDeSdService {

    @Override
    public Map<String, Object> attributes(Long sopId) {
        if (sopId == null) {
            throw new ServiceException("sopId 必填");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sopId", sopId);
        out.put("职责", new ArrayList<>());
        out.put("输入", new ArrayList<>());
        out.put("交付", new ArrayList<>());
        out.put("AI能力", new ArrayList<>());
        out.put("审核", new ArrayList<>());
        out.put("异常", new ArrayList<>());
        return out;
    }

    @Override
    public Map<String, Object> resourceRules(Long sopId) {
        if (sopId == null) {
            throw new ServiceException("sopId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("sopId", sopId);
        out.put("selectable", new ArrayList<>());
        out.put("appendable", new ArrayList<>());
        out.put("mustKeep", new ArrayList<>());
        return out;
    }

    @Override
    public Map<String, Object> preview(Long sopId, Long userId) {
        if (sopId == null) {
            throw new ServiceException("sopId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("sopId", sopId);
        out.put("userId", userId);
        out.put("parsedNodes", new ArrayList<>());
        out.put("missingCapabilities", new ArrayList<>());
        out.put("previewHtml", "(占位)");
        return out;
    }

    @Override
    public Map<String, Object> graph(Long sopId) {
        if (sopId == null) {
            throw new ServiceException("sopId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("sopId", sopId);
        out.put("nodes", new ArrayList<>());
        out.put("edges", new ArrayList<>());
        out.put("branches", new ArrayList<>());
        out.put("parallelGroups", new ArrayList<>());
        out.put("timeouts", new ArrayList<>());
        return out;
    }

    @Override
    public Map<String, Object> versions(Long sopId) {
        if (sopId == null) {
            throw new ServiceException("sopId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("sopId", sopId);
        out.put("versions", new ArrayList<>());
        out.put("diff", "(占位)");
        out.put("inFlightImpact", "(占位 —— 在途实例影响分析)");
        return out;
    }
}