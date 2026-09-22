package org.dromara.workflow.de.nc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.nc.domain.bo.DeNcDiffBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcExecModeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcScopeBo;
import org.dromara.workflow.de.nc.service.IDeNcService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 节点执行配置 NC 域 REST 端点（NC-05 / NC-06 / NC-08 共 3 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/nc")
public class DeNcController extends BaseController {

    private final IDeNcService deNcService;

    /** NC-05 保存范围 */
    @PostMapping("/scope")
    public R<Long> saveScope(@Valid @RequestBody DeNcScopeBo bo) {
        return R.ok("scope 已保存", deNcService.saveScope(bo));
    }

    /** NC-06 执行模式 */
    @PutMapping("/exec-mode")
    public R<Long> setExecMode(@Valid @RequestBody DeNcExecModeBo bo) {
        return R.ok("exec-mode 已更新", deNcService.setExecMode(bo));
    }

    /** NC-08 更换执行者 / 资源时显示差异 */
    @PostMapping("/diff")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> showDiff(@Valid @RequestBody DeNcDiffBo bo) {
        return R.ok((Map<String, Object>) deNcService.showDiff(bo));
    }
}