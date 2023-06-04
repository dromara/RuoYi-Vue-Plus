package org.dromara.workflow.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ModelBo;
import org.dromara.workflow.domain.vo.AccountVo;
import org.dromara.workflow.service.IActModelService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 模型管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/model")
public class ActModelController extends BaseController {

    private final RepositoryService repositoryService;

    private final IActModelService iActModelService;


    @GetMapping("/list")
    public TableDataInfo<Model> getByPage(ModelBo modelBo) {
        return iActModelService.getByPage(modelBo);
    }

    /**
     * 参考官方提供的响应数据
     * "{\"id\":\"admin\",\"firstName\":\"Test\",\"lastName\":\"Administrator\",\"email\":\"admin@flowable.org\",\"fullName\":\"Test Administrator\",\"groups\":[],\"privileges\":[\"access-idm\",\"access-rest-api\",\"access-task\",\"access-modeler\",\"access-admin\"]}";
     */
    @GetMapping("/rest/account")
    public String getAccount() {
        AccountVo accountVo = new AccountVo();
        accountVo.setId(Objects.requireNonNull(LoginHelper.getUserId()).toString());
        accountVo.setFirstName("");
        accountVo.setLastName(LoginHelper.getUsername());
        accountVo.setFullName(LoginHelper.getUsername());
        return JsonUtils.toJsonString(accountVo);
    }

    /**
     * 新增模型
     *
     * @param modelBo 模型请求对象
     */
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    @PostMapping("/rest/models")
    public R<Void> saveNewModel(@Validated(AddGroup.class) @RequestBody ModelBo modelBo) {
        return toAjax(iActModelService.saveNewModel(modelBo));
    }

    /**
     * 查询模型
     *
     * @param modelId 模型id
     */
    @GetMapping("/rest/models/{modelId}/editor/json")
    public ObjectNode getModelInfo(@PathVariable String modelId) {
        return iActModelService.getModelInfo(modelId);
    }


    /**
     * 编辑模型
     *
     * @param modelId 模型id
     * @param values  模型数据
     */
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/rest/models/{modelId}/editor/json")
    public R<Void> editModel(@PathVariable String modelId, @RequestParam MultiValueMap<String, String> values) {
        return toAjax(iActModelService.editModel(modelId, values));
    }

    /**
     * 删除流程模型
     *
     * @param ids 模型id
     */
    @Log(title = "模型管理", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{ids}")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> delete(@NotEmpty(message = "主键不能为空") @PathVariable String[] ids) {
        for (String id : ids) {
            repositoryService.deleteModel(id);
        }
        return R.ok();
    }

}
