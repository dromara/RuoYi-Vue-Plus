package org.dromara.workflow.controller;

import com.warm.flow.core.entity.Definition;
import com.warm.flow.core.service.DefService;
import com.warm.flow.orm.entity.FlowDefinition;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.vo.FlowDefinitionVo;
import org.dromara.workflow.service.IFlwDefinitionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * 流程定义管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/definition")
public class FlwDefinitionController extends BaseController {

    private final IFlwDefinitionService iFlwDefinitionService;
    private final DefService defService;

    /**
     * 分页查询
     *
     * @param flowDefinition 参数
     */
    @GetMapping("/list")
    public TableDataInfo<FlowDefinitionVo> page(FlowDefinition flowDefinition, PageQuery pageQuery) {
        return iFlwDefinitionService.page(flowDefinition, pageQuery);
    }

    /**
     * 获取历史流程定义列表
     *
     * @param flowCode 参数
     */
    @GetMapping("/getHisListByKey/{flowCode}")
    public R<List<FlowDefinitionVo>> getHisListByKey(@PathVariable String flowCode) {
        return R.ok(iFlwDefinitionService.getHisListByKey(flowCode));
    }

    /**
     * 获取流程定义详细信息
     *
     * @param id 流程定义id
     */
    @GetMapping(value = "/{id}")
    public R<Definition> getInfo(@PathVariable Long id) {
        return R.ok(defService.getById(id));
    }

    /**
     * 新增流程定义
     *
     * @param flowDefinition 参数
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> add(@RequestBody FlowDefinition flowDefinition) {
        return R.ok(defService.checkAndSave(flowDefinition));
    }

    /**
     * 修改流程定义
     *
     * @param flowDefinition 参数
     */
    @Log(title = "流程定义", businessType = BusinessType.UPDATE)
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> edit(@RequestBody FlowDefinition flowDefinition) {
        return R.ok(defService.updateById(flowDefinition));
    }

    /**
     * 发布流程定义
     *
     * @param id 流程定义id
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PutMapping("/publish/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> publish(@PathVariable Long id) {
        return R.ok(defService.publish(id));
    }

    /**
     * 取消发布流程定义
     *
     * @param id 流程定义id
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PutMapping("/unPublish/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> unPublish(@PathVariable Long id) {
        return R.ok(defService.unPublish(id));
    }

    /**
     * 删除流程定义
     */
    @Log(title = "流程定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> remove(@PathVariable List<Long> ids) {
        return R.ok(defService.removeDef(ids));
    }

    /**
     * 复制流程定义
     *
     * @param id 流程定义id
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @GetMapping("/copyDef/{id}")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> copyDef(@PathVariable Long id) {
        return R.ok(defService.copyDef(id));
    }

    /**
     * 导入流程定义
     *
     * @param file 文件
     */
    @Log(title = "流程定义", businessType = BusinessType.IMPORT)
    @PostMapping("/importDefinition")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> importDefinition(MultipartFile file) {
        return R.ok(iFlwDefinitionService.importXml(file));
    }

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    @Log(title = "流程定义", businessType = BusinessType.EXPORT)
    @PostMapping("/exportDefinition/{id}")
    public void exportDefinition(@PathVariable Long id, HttpServletResponse response) throws IOException {
        iFlwDefinitionService.exportDefinition(id, response);
    }

    /**
     * 获取流程定义xml字符串
     *
     * @param id 流程定义id
     */
    @GetMapping("/xmlString/{id}")
    public R<String> xmlString(@PathVariable Long id) {
        return R.ok(defService.xmlString(id));
    }

    /**
     * 查询流程图
     *
     * @param instanceId 流程实例id
     * @throws IOException 异常
     */
    @GetMapping("/flowChart/{instanceId}")
    public R<String> flowChart(@PathVariable Long instanceId) throws IOException {
        return R.ok(defService.flowChart(instanceId));
    }

    /**
     * 激活/挂起流程定义
     *
     * @param id     流程定义id
     * @param active 激活/挂起
     */
    @PutMapping("/updateDefinitionState/{id}/{active}")
    public R<Boolean> active(@PathVariable Long id, @PathVariable boolean active) {
        if (active) {
            return R.ok(defService.unActive(id));
        } else {
            return R.ok(defService.active(id));
        }
    }

}
