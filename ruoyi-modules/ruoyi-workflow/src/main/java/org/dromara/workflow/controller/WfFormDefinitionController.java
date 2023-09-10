package org.dromara.workflow.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.workflow.domain.bo.WfFormDefinitionBo;
import org.dromara.workflow.service.IWfFormDefinitionService;

/**
 * 动态单与流程定义关联信息
 *
 * @author may
 * @date 2023-08-31
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/formDefinition")
public class WfFormDefinitionController extends BaseController {

    private final IWfFormDefinitionService wfFormDefinitionService;

    /**
     * 新增动态单与流程定义关联信息
     */
    @Log(title = "动态单与流程定义关联信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WfFormDefinitionBo bo) {
        return toAjax(wfFormDefinitionService.insertByBo(bo));
    }
}
