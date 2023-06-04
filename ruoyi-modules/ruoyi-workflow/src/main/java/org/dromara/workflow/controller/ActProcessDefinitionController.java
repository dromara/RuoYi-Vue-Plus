package org.dromara.workflow.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.dromara.workflow.service.IActProcessDefinitionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
