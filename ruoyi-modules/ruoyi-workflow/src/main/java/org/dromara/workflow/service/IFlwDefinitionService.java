package org.dromara.workflow.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.workflow.domain.vo.FlowDefinitionVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;

/**
 * 流程定义 服务层
 *
 * @author may
 */
public interface IFlwDefinitionService {

    /**
     * 查询流程定义列表
     *
     * @param flowDefinition 参数
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    PageResult<FlowDefinitionVo> queryList(FlowDefinition flowDefinition, PageQuery pageQuery);

    /**
     * 查询未发布的流程定义列表
     *
     * @param flowDefinition 参数
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    PageResult<FlowDefinitionVo> unPublishList(FlowDefinition flowDefinition, PageQuery pageQuery);

    /**
     * 发布流程定义
     *
     * @param id 流程定义id
     * @return 是否发布成功
     */
    boolean publish(Long id);

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    void exportDef(Long id, HttpServletResponse response) throws IOException;

    /**
     * 导入流程定义
     *
     * @param file     文件
     * @param category 分类
     * @return 是否导入成功
     */
    boolean importJson(MultipartFile file, String category);

    /**
     * 删除流程定义
     *
     * @param ids 流程定义id
     * @return 是否删除成功
     */
    boolean removeDef(Collection<Long> ids);

    /**
     * 新增或更新流程定义（id 为空 ⇒ 插入；否则 ⇒ 按主键更新）。
     *
     * <p>D-016 偏离登记：底座 {@code FlwDefinitionController} 仅暴露 POST(insert via {@code defService.checkAndSave})
     * 与 PUT(update via {@code defService.updateById}) 两端点，且调用 {@code DefService}（warm-flow）
     * 而非 {@code FlowDefinitionMapper}。为给 de/ 层提供一条「按主键写、避开 {@code defService} 校验链
     * 适用 UI 设计器主动落盘」的窄入口，本方法显式声明在 {@code IFlwDefinitionService}。
     *
     * <p>回退路径：{@code git -C 04-backend-code checkout -- ruoyi-modules/ruoyi-workflow/src/main/java/org/dromara/workflow/service/IFlwDefinitionService.java}。
     *
     * @param def 流程定义（id 为空 ⇒ 新增；非空 ⇒ 更新）
     * @return 写操作成功返回 {@code true}
     */
    boolean saveOrUpdate(FlowDefinition def);

    /**
     * 仅修改流程定义的「激活/挂起」状态（{@code flow_definition.activity_status}）。
     *
     * <p>D-016 偏离登记：底座 {@code FlwDefinitionController.active} 端点走 {@code defService.active} /
     * {@code defService.unActive}（{@code active=true ⇒ active / false ⇒ unActive}），
     * 接口签名是 {@code boolean active} 反向语义。本方法显式接收目标值 {@code Integer activityStatus}
     *（0=挂起 / 1=激活，与 DDL 真值一致）以避免上层做一次反向解码。
     *
     * <p>回退路径同 {@link #saveOrUpdate(FlowDefinition)}。
     *
     * @param definitionId   流程定义主键 id
     * @param activityStatus 目标激活状态（0=挂起 / 1=激活）
     * @return 更新成功返回 {@code true}
     */
    boolean updateActivityStatus(Long definitionId, Integer activityStatus);

}
