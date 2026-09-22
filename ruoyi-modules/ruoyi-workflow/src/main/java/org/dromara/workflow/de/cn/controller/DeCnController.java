package org.dromara.workflow.de.cn.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.cn.service.IDeCnService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CN 域 REST 端点（CN-03 共 1 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/cn")
public class DeCnController extends BaseController {

    private final IDeCnService deCnService;

    /** CN-03 连接器业务选择器 */
    @GetMapping("/selectors")
    public R<java.util.List<Map<String, Object>>> selectors(@RequestParam(required = false) Long userId) {
        return R.ok(deCnService.selectors(userId));
    }
}