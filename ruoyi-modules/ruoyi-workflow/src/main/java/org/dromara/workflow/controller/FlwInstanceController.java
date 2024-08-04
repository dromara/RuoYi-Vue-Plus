package org.dromara.workflow.controller;

import com.warm.flow.core.entity.Instance;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.service.IFlwInstanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程实例管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/processInstance")
public class FlwInstanceController extends BaseController {

    private final IFlwInstanceService flwInstanceService;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @GetMapping("/getPageByRunning")
    public TableDataInfo<Instance> getPageByRunning(Instance instance, PageQuery pageQuery) {
        return flwInstanceService.getPageByRunning(instance, pageQuery);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @GetMapping("/getPageByFinish")
    public TableDataInfo<Instance> getPageByFinish(Instance instance, PageQuery pageQuery) {
        return flwInstanceService.getPageByFinish(instance, pageQuery);
    }

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     */
    @DeleteMapping("/deleteByBusinessIds/{businessIds}")
    public R<Void> deleteByBusinessIds(@PathVariable List<Long> businessIds) {
        return toAjax(flwInstanceService.deleteByBusinessIds(businessIds));
    }

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     */
    @DeleteMapping("/deleteByInstanceIds/{instanceIds}")
    public R<Void> deleteByInstanceIds(@PathVariable List<Long> instanceIds) {
        return toAjax(flwInstanceService.deleteByInstanceIds(instanceIds));
    }

    /**
     * 撤销流程
     *
     * @param businessId 业务id
     */
    @PutMapping("/cancelProcessApply/{businessId}")
    public R<Void> cancelProcessApply(@PathVariable String businessId) {
        return toAjax(flwInstanceService.cancelProcessApply(businessId));
    }
}
