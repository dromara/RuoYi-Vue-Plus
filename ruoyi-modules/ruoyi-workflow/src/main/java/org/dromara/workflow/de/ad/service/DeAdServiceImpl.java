package org.dromara.workflow.de.ad.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.ad.domain.bo.DeAdHandoverBo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AD 域服务实现。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeAdServiceImpl implements IDeAdService {

    @Override
    public Map<String, Object> dimensionSeparate(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("userId", userId);
        out.put("dimensions", new String[]{"组织任职", "身份认证", "角色策略", "资源归属", "资产授权"});
        out.put("detail", new ArrayList<>());
        return out;
    }

    @Override
    public List<Map<String, Object>> resourceCatalog(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId 必填");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> placeholder = new HashMap<>();
        placeholder.put("resourceId", 0L);
        placeholder.put("label", "(资源目录 —— 占位)");
        placeholder.put("selfService", true);
        out.add(placeholder);
        return out;
    }

    @Override
    public Map<String, Object> assetHandover(DeAdHandoverBo bo) {
        if (bo.getUserId() == null) {
            throw new ServiceException("userId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("userId", bo.getUserId());
        out.put("preview", new ArrayList<>());
        out.put("note", "占位 —— AD-03 / AD-07 涉及 sys_user / sys_role / sai_user_agent 跨表联动");
        return out;
    }
}