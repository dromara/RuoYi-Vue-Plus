package org.dromara.workflow.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.dromara.workflow.service.IActProcessDefinitionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 流程定义管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/processDefinition")
public class ActProcessDefinitionController extends BaseController {

    private final IActProcessDefinitionService actProcessDefinitionService;

    /**
     * 分页查询
     *
     * @param processDefinitionBo 参数
     */
    @GetMapping("/list")
    public TableDataInfo<ProcessDefinitionVo> page(ProcessDefinitionBo processDefinitionBo) {
        return actProcessDefinitionService.page(processDefinitionBo);
    }

    /**
     * 查询历史流程定义列表
     *
     * @param key 流程定义key
     */
    @GetMapping("/getProcessDefinitionListByKey/{key}")
    public R<List<ProcessDefinitionVo>> getProcessDefinitionListByKey(@NotEmpty(message = "流程定义key不能为空") @PathVariable String key) {
        return R.ok("操作成功", actProcessDefinitionService.getProcessDefinitionListByKey(key));
    }

    /**
     * 查看流程定义图片
     *
     * @param processDefinitionId 流程定义id
     */
    @GetMapping("/processDefinitionImage/{processDefinitionId}")
    public R<String> processDefinitionImage(@PathVariable String processDefinitionId) {
        return R.ok("操作成功", actProcessDefinitionService.processDefinitionImage(processDefinitionId));
    }

    /**
     * 查看流程定义xml文件
     *
     * @param processDefinitionId 流程定义id
     */
    @GetMapping("/processDefinitionXml/{processDefinitionId}")
    public R<Map<String, Object>> getXml(@NotBlank(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        Map<String, Object> map = new HashMap<>();
        String xmlStr = actProcessDefinitionService.processDefinitionXml(processDefinitionId);
        map.put("xml", Arrays.asList(xmlStr.split("\n")));
        map.put("xmlStr", xmlStr);
        return R.ok(map);
    }

    /**
     * 删除流程定义
     *
     * @param deploymentId        部署id
     * @param processDefinitionId 流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{deploymentId}/{processDefinitionId}")
    public R<Void> deleteDeployment(@NotBlank(message = "流程部署id不能为空") @PathVariable String deploymentId,
                                    @NotBlank(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        return toAjax(actProcessDefinitionService.deleteDeployment(deploymentId, processDefinitionId));
    }

    /**
     * 激活或者挂起流程定义
     *
     * @param processDefinitionId 流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/updateProcessDefState/{processDefinitionId}")
    public R<Void> updateProcDefState(@NotBlank(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        return toAjax(actProcessDefinitionService.updateProcessDefState(processDefinitionId));
    }

    /**
     * 迁移流程定义
     *
     * @param currentProcessDefinitionId 当前流程定义id
     * @param fromProcessDefinitionId    需要迁移到的流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/migrationProcessDefinition/{currentProcessDefinitionId}/{fromProcessDefinitionId}")
    public R<Void> migrationProcessDefinition(@NotBlank(message = "当前流程定义id") @PathVariable String currentProcessDefinitionId,
                                              @NotBlank(message = "需要迁移到的流程定义id") @PathVariable String fromProcessDefinitionId) {
        return toAjax(actProcessDefinitionService.migrationProcessDefinition(currentProcessDefinitionId, fromProcessDefinitionId));
    }

    /**
     * 流程定义转换为模型
     *
     * @param processDefinitionId 流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/convertToModel/{processDefinitionId}")
    public R<Void> convertToModel(@NotEmpty(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        return toAjax(actProcessDefinitionService.convertToModel(processDefinitionId));
    }

    /**
     * 通过zip或xml部署流程定义
     *
     * @param file         文件
     * @param categoryCode 分类
     */
    @Log(title = "流程定义管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/deployByFile")
    public R<Void> deployByFile(@RequestParam("file") MultipartFile file, @RequestParam("categoryCode") String categoryCode) {
        return toAjax(actProcessDefinitionService.deployByFile(file, categoryCode));
    }
}
