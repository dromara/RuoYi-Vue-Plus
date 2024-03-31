package org.dromara.workflow.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.*;
import org.dromara.workflow.domain.bo.WfDefinitionConfigBo;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.workflow.domain.vo.WfDefinitionConfigVo;
import org.dromara.workflow.service.IWfDefinitionConfigService;

/**
 * 表单配置
 *
 * @author may
 * @date 2024-03-18
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/definitionConfig")
public class WfDefinitionConfigController extends BaseController {

    private final IWfDefinitionConfigService wfDefinitionConfigService;


    /**
     * 获取表单配置详细信息
     *
     * @param definitionId 主键
     */
    @GetMapping("/getByDefId/{definitionId}")
    public R<WfDefinitionConfigVo> getByDefId(@NotBlank(message = "流程定义ID不能为空")
                                            @PathVariable String definitionId) {
        return R.ok(wfDefinitionConfigService.getByDefId(definitionId));
    }

    /**
     * 新增表单配置
     */
    @Log(title = "表单配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/saveOrUpdate")
    public R<Void> saveOrUpdate(@Validated(AddGroup.class) @RequestBody WfDefinitionConfigBo bo) {
        return toAjax(wfDefinitionConfigService.saveOrUpdate(bo));
    }

    /**
     * 删除表单配置
     *
     * @param ids 主键串
     */
    @Log(title = "表单配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(wfDefinitionConfigService.deleteByIds(List.of(ids)));
    }
}
