package org.dromara.workflow.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.dromara.workflow.service.IActProcessDefinitionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;


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

    private final IActProcessDefinitionService iActProcessDefinitionService;

    /**
     * 分页查询
     *
     * @param processDefinitionBo 参数
     */
    @GetMapping("/list")
    public TableDataInfo<ProcessDefinitionVo> getByPage(ProcessDefinitionBo processDefinitionBo) {
        return iActProcessDefinitionService.getByPage(processDefinitionBo);
    }

    /**
     * 查询历史流程定义列表
     *
     * @param key 流程定义key
     */
    @GetMapping("/getProcessDefinitionListByKey/{key}")
    public R<List<ProcessDefinitionVo>> getProcessDefinitionList(@NotEmpty(message = "流程定义key不能为空") @PathVariable String key) {
        return R.ok("操作成功", iActProcessDefinitionService.getProcessDefinitionListByKey(key));
    }

    /**
     * 查看流程定义图片
     *
     * @param processDefinitionId 流程定义id
     * @param response            响应
     */
    @GetMapping("/processDefinitionImage/{processDefinitionId}")
    public void processDefinitionImage(@PathVariable String processDefinitionId, HttpServletResponse response) {
        iActProcessDefinitionService.processDefinitionImage(processDefinitionId, response);
    }

    /**
     * 查看流程定义xml文件
     *
     * @param processDefinitionId 流程定义id
     */
    @GetMapping("/processDefinitionXml/{processDefinitionId}")
    public R<Map<String, Object>> getXml(@NotBlank(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        Map<String, Object> map = new HashMap<>();
        String xmlStr = iActProcessDefinitionService.processDefinitionXml(processDefinitionId);
        List<String> xml = new ArrayList<>(Arrays.asList(xmlStr.split("\n")));
        map.put("xml", xml);
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
    @DeleteMapping("/{deploymentId}/{processDefinitionId}")
    public R<Void> deleteDeployment(@NotBlank(message = "流程部署id不能为空") @PathVariable String deploymentId,
                                    @NotBlank(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        return toAjax(iActProcessDefinitionService.deleteDeployment(deploymentId, processDefinitionId));
    }

    /**
     * 激活或者挂起流程定义
     *
     * @param processDefinitionId 流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @PutMapping("/updateProcessDefState/{processDefinitionId}")
    public R<Void> updateProcDefState(@PathVariable String processDefinitionId) {
        return toAjax(iActProcessDefinitionService.updateProcessDefState(processDefinitionId));
    }

    /**
     * 迁移流程定义
     *
     * @param currentProcessDefinitionId 当前流程定义id
     * @param fromProcessDefinitionId    需要迁移到的流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @PutMapping("/migrationProcessDefinition/{currentProcessDefinitionId}/{fromProcessDefinitionId}")
    public R<Void> migrationProcessDefinition(@PathVariable String currentProcessDefinitionId, @PathVariable String fromProcessDefinitionId) {
        return toAjax(iActProcessDefinitionService.migrationProcessDefinition(currentProcessDefinitionId, fromProcessDefinitionId));
    }

    /**
     * 流程定义转换为模型
     *
     * @param processDefinitionId 流程定义id
     */
    @Log(title = "流程定义管理", businessType = BusinessType.UPDATE)
    @PutMapping("/convertToModel/{processDefinitionId}")
    public R<Void> convertToModel(@NotEmpty(message = "流程定义id不能为空") @PathVariable String processDefinitionId) {
        return toAjax(iActProcessDefinitionService.convertToModel(processDefinitionId));
    }
}
