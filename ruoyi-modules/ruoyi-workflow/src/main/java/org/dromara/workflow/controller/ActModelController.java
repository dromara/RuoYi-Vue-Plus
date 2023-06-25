package org.dromara.workflow.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
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
import org.dromara.workflow.domain.vo.ResultListDataRepresentation;
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


    /**
     * 分页查询模型
     *
     * @param modelBo 模型参数
     */
    @GetMapping("/list")
    public TableDataInfo<Model> getByPage(ModelBo modelBo) {
        return iActModelService.getByPage(modelBo);
    }

    /**
     * 设计器登录信息
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
    @RepeatSubmit()
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
    public ObjectNode getModelInfo(@NotBlank(message = "模型id不能为空") @PathVariable String modelId) {
        return iActModelService.getModelInfo(modelId);
    }


    /**
     * 编辑模型
     *
     * @param modelId 模型id
     * @param values  模型数据
     */
    @Log(title = "模型管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
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

    /**
     * 模型部署
     *
     * @param id 模型id
     */
    @Log(title = "模型管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/modelDeploy/{id}")
    public R<Void> deploy(@NotBlank(message = "模型id不能为空") @PathVariable("id") String id) {
        return toAjax(iActModelService.modelDeploy(id));
    }

    /**
     * 导出模型zip压缩包
     *
     * @param modelId  模型id
     * @param response 相应
     */
    @GetMapping("/export/zip/{modelId}")
    public void exportZip(@NotEmpty(message = "模型id不能为空") @PathVariable String modelId,
                          HttpServletResponse response) {
        iActModelService.exportZip(modelId, response);
    }

    /**
     * 查询用户
     *
     * @param filter 参数
     */
    @GetMapping(value = "/rest/editor-users")
    public ResultListDataRepresentation getUsers(@RequestParam(value = "filter", required = false) String filter) {
        return iActModelService.getUsers(filter);
    }

    /**
     * 查询用户组
     *
     * @param filter 参数
     */
    @GetMapping(value = "/rest/editor-groups")
    public ResultListDataRepresentation getGroups(@RequestParam(required = false, value = "filter") String filter) {
        return iActModelService.getGroups(filter);
    }

}
