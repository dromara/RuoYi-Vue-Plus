package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.workflow.common.ConditionalOnEnable;
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
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/definition")
public class FlwDefinitionController extends BaseController {

    private final DefService defService;
    private final IFlwDefinitionService flwDefinitionService;

    /**
     * 分页查询流程定义列表。
     *
     * @param flowDefinition 查询条件
     * @param pageQuery      分页参数
     * @return 流程定义分页数据
     */
    @GetMapping("/list")
    @SaCheckPermission("workflow:definition:list")
    public R<PageResult<FlowDefinitionVo>> list(FlowDefinition flowDefinition, PageQuery pageQuery) {
        return R.ok(flwDefinitionService.queryList(flowDefinition, pageQuery));
    }

    /**
     * 分页查询未发布的流程定义列表。
     *
     * @param flowDefinition 查询条件
     * @param pageQuery      分页参数
     * @return 未发布流程定义分页数据
     */
    @GetMapping("/unPublishList")
    @SaCheckPermission("workflow:definition:list")
    public R<PageResult<FlowDefinitionVo>> unPublishList(FlowDefinition flowDefinition, PageQuery pageQuery) {
        return R.ok(flwDefinitionService.unPublishList(flowDefinition, pageQuery));
    }

    /**
     * 获取流程定义详细信息。
     *
     * @param id 流程定义id
     * @return 流程定义详情
     */
    @GetMapping(value = "/{id}")
    @SaCheckPermission("workflow:definition:query")
    public R<Definition> getInfo(@PathVariable Long id) {
        return R.ok(defService.getById(id));
    }

    /**
     * 新增流程定义并执行格式校验。
     *
     * @param flowDefinition 流程定义信息
     * @return 操作结果
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit()
    @Transactional(rollbackFor = Exception.class)
    @SaCheckPermission("workflow:definition:add")
    public R<Boolean> add(@RequestBody FlowDefinition flowDefinition) {
        return R.ok(defService.checkAndSave(flowDefinition));
    }

    /**
     * 修改流程定义。
     *
     * @param flowDefinition 流程定义信息
     * @return 操作结果
     */
    @Log(title = "流程定义", businessType = BusinessType.UPDATE)
    @PutMapping
    @RepeatSubmit()
    @Transactional(rollbackFor = Exception.class)
    @SaCheckPermission("workflow:definition:edit")
    public R<Boolean> edit(@RequestBody FlowDefinition flowDefinition) {
        return R.ok(defService.updateById(flowDefinition));
    }

    /**
     * 发布流程定义，使其进入可用状态。
     *
     * @param id 流程定义id
     * @return 发布结果
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PutMapping("/publish/{id}")
    @RepeatSubmit()
    @SaCheckPermission("workflow:definition:publish")
    public R<Boolean> publish(@PathVariable Long id) {
        return R.ok(flwDefinitionService.publish(id));
    }

    /**
     * 取消发布流程定义。
     *
     * @param id 流程定义id
     * @return 操作结果
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PutMapping("/unPublish/{id}")
    @RepeatSubmit()
    @Transactional(rollbackFor = Exception.class)
    @SaCheckPermission("workflow:definition:publish")
    public R<Boolean> unPublish(@PathVariable Long id) {
        return R.ok(defService.unPublish(id));
    }

    /**
     * 批量删除流程定义。
     *
     * @param ids 流程定义ID集合
     * @return 操作结果
     */
    @Log(title = "流程定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @SaCheckPermission("workflow:definition:remove")
    public R<Void> remove(@PathVariable List<Long> ids) {
        return toAjax(flwDefinitionService.removeDef(ids));
    }

    /**
     * 复制一份流程定义，便于快速创建相似流程。
     *
     * @param id 流程定义id
     * @return 复制结果
     */
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{id}")
    @RepeatSubmit()
    @Transactional(rollbackFor = Exception.class)
    @SaCheckPermission("workflow:definition:copy")
    public R<Boolean> copy(@PathVariable Long id) {
        return R.ok(defService.copyDef(id));
    }

    /**
     * 通过文件导入流程定义。
     *
     * @param file     文件
     * @param category 分类
     * @return 导入结果
     */
    @Log(title = "流程定义", businessType = BusinessType.IMPORT)
    @PostMapping("/importDef")
    @SaCheckPermission("workflow:definition:import")
    public R<Boolean> importDef(MultipartFile file, String category) {
        return R.ok(flwDefinitionService.importJson(file, category));
    }

    /**
     * 导出流程定义文件。
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    @Log(title = "流程定义", businessType = BusinessType.EXPORT)
    @PostMapping("/exportDef/{id}")
    @SaCheckPermission("workflow:definition:export")
    public void exportDef(@PathVariable Long id, HttpServletResponse response) throws IOException {
        flwDefinitionService.exportDef(id, response);
    }

    /**
     * 获取流程定义 JSON 字符串。
     *
     * @param id 流程定义id
     * @return 流程定义 JSON
     */
    @GetMapping("/xmlString/{id}")
    @SaCheckPermission("workflow:definition:query")
    public R<String> xmlString(@PathVariable Long id) {
        return R.ok("操作成功", defService.exportJson(id));
    }

    /**
    /**
     * 激活或挂起流程定义。
     *
     * @param id     流程定义id
     * @param active 激活/挂起
     * @return 处理结果
     */
    @RepeatSubmit()
    @PutMapping("/active/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Log(title = "流程定义", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:definition:active")
    public R<Boolean> active(@PathVariable Long id, @RequestParam boolean active) {
        return R.ok(active ? defService.active(id) : defService.unActive(id));
    }

}
