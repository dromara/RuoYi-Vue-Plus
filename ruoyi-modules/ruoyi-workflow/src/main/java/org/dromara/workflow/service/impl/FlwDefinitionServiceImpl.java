package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.vo.FlowDefinitionVo;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.IFlwDefinitionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 流程定义 服务层实现
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwDefinitionServiceImpl implements IFlwDefinitionService {

    private final DefService defService;
    private final FlowDefinitionMapper flowDefinitionMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowNodeMapper flowNodeMapper;
    private final FlwCategoryMapper flwCategoryMapper;
    private final IFlwCommonService flwCommonService;

    /**
     * 查询流程定义列表
     *
     * @param flowDefinition 流程定义信息
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    @Override
    public PageResult<FlowDefinitionVo> queryList(FlowDefinition flowDefinition, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowDefinition> wrapper = buildQueryWrapper(flowDefinition);
        wrapper.eq(FlowDefinition::getIsPublish, PublishStatus.PUBLISHED.getKey());
        Page<FlowDefinition> page = flowDefinitionMapper.selectPage(pageQuery.build(), wrapper);
        List<FlowDefinitionVo> list = BeanUtil.copyToList(page.getRecords(), FlowDefinitionVo.class);
        return PageResult.build(list, page.getTotal());
    }

    /**
     * 查询未发布的流程定义列表
     *
     * @param flowDefinition 流程定义信息
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    @Override
    public PageResult<FlowDefinitionVo> unPublishList(FlowDefinition flowDefinition, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowDefinition> wrapper = buildQueryWrapper(flowDefinition);
        wrapper.in(FlowDefinition::getIsPublish, Arrays.asList(PublishStatus.UNPUBLISHED.getKey(), PublishStatus.EXPIRED.getKey()));
        Page<FlowDefinition> page = flowDefinitionMapper.selectPage(pageQuery.build(), wrapper);
        List<FlowDefinitionVo> list = BeanUtil.copyToList(page.getRecords(), FlowDefinitionVo.class);
        return PageResult.build(list, page.getTotal());
    }

    /**
     * 构造流程定义分页查询条件。
     *
     * @param flowDefinition 页面传入的流程定义筛选条件
     * @return 叠加流程编码、名称、分类以及排序条件后的查询包装器
     */
    private LambdaQueryWrapper<FlowDefinition> buildQueryWrapper(FlowDefinition flowDefinition) {
        LambdaQueryWrapper<FlowDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.isNotBlank(flowDefinition.getFlowCode()), FlowDefinition::getFlowCode, flowDefinition.getFlowCode());
        wrapper.like(StringUtils.isNotBlank(flowDefinition.getFlowName()), FlowDefinition::getFlowName, flowDefinition.getFlowName());
        if (StringUtils.isNotBlank(flowDefinition.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowDefinition.getCategory()));
            wrapper.in(FlowDefinition::getCategory, StreamUtils.toList(categoryIds, Convert::toStr));
        }
        wrapper.orderByDesc(FlowDefinition::getCreateTime);
        return wrapper;
    }

    /**
     * 发布流程定义
     *
     * @param id 流程定义id
     * @return 发布成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(Long id) {
        List<FlowNode> flowNodes = flowNodeMapper.selectList(
            QueryBuilder.lambda(FlowNode.class).eq(FlowNode::getDefinitionId, id).build());
        List<String> errorMsg = new ArrayList<>();
        if (CollUtil.isNotEmpty(flowNodes)) {
            String applyNodeCode = flwCommonService.applyNodeCode(id);
            for (FlowNode flowNode : flowNodes) {
                if (StringUtils.isBlank(flowNode.getPermissionFlag()) && !applyNodeCode.equals(flowNode.getNodeCode()) && NodeType.BETWEEN.getKey().equals(flowNode.getNodeType())) {
                    errorMsg.add(flowNode.getNodeName());
                }
            }
            if (CollUtil.isNotEmpty(errorMsg)) {
                throw new ServiceException("节点【{}】未配置办理人!", StringUtils.joinComma(errorMsg));
            }
        }
        return defService.publish(id);
    }

    /**
     * 导入流程定义
     *
     * @param file     流程定义 Json 文件
     * @param category 导入后绑定的流程分类
     * @return 导入成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importJson(MultipartFile file, String category) {
        try {
            DefJson defJson = JsonUtils.parseObject(file.getBytes(), DefJson.class);
            defJson.setCategory(category);
            defService.importDef(defJson);
        } catch (IOException e) {
            log.error("读取文件流错误: {}", e.getMessage(), e);
            throw new IllegalStateException("文件读取失败，请检查文件内容", e);
        }
        return true;
    }

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response HTTP 响应对象，用于输出定义 Json 内容
     * @throws IOException 异常
     */
    @Override
    public void exportDef(Long id, HttpServletResponse response) throws IOException {
        byte[] data = defService.exportJson(id).getBytes(StandardCharsets.UTF_8);
        // 设置响应头和内容类型
        response.reset();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/text");
        response.setHeader("Content-Disposition", "attachment;");
        response.addHeader("Content-Length", "" + data.length);
        IoUtil.write(response.getOutputStream(), false, data);
    }

    /**
     * 删除流程定义
     *
     * @param ids 流程定义id
     * @return 删除成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDef(Collection<Long> ids) {
        LambdaQueryWrapper<FlowHisTask> wrapper = Wrappers.lambdaQuery();
        wrapper.in(FlowHisTask::getDefinitionId, ids);
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(flowHisTasks)) {
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectByIds(StreamUtils.toList(flowHisTasks, FlowHisTask::getDefinitionId));
            if (CollUtil.isNotEmpty(flowDefinitions)) {
                String join = StreamUtils.join(flowDefinitions, FlowDefinition::getFlowCode);
                log.info("流程定义【{}】已被使用不可被删除！", join);
                throw new ServiceException("流程定义【{}】已被使用不可被删除！", join);
            }
        }
        try {
            defService.removeDef((List<Long>) ids);
        } catch (Exception e) {
            log.error("Error removing flow definitions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove flow definitions", e);
        }
        return true;
    }

    /**
     * 新增或更新流程定义。
     *
     * <p>D-016 偏离登记。{@code id == null} ⇒ {@code flowDefinitionMapper.insert(def)}；
     * 否则 ⇒ {@code flowDefinitionMapper.updateById(def)}。两路复用本类已有的
     * {@link org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper} 依赖（warm-flow starter 自带，
     * bean 名 {@code flowDefinitionMapper}；与项目自有 {@code deFlowDefinitionMapper} 不冲突，见 D-004）。
     *
     * <p>调用方负责保证 {@code def.flowCode} / {@code flowName} / {@code version} 必填（DDL NOT NULL）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(FlowDefinition def) {
        if (def == null) {
            throw new ServiceException("流程定义不能为空");
        }
        // D-016：id 为空 ⇒ 视为新增；DDL 的 id 列无 AUTO_INCREMENT，调用方必须自填主键或由 defService.checkAndSave 上游路径生成
        if (def.getId() == null) {
            log.info("[flw.def.saveOrUpdate] insert flowCode={}", def.getFlowCode());
            return flowDefinitionMapper.insert(def) > 0;
        }
        log.info("[flw.def.saveOrUpdate] update id={}", def.getId());
        return flowDefinitionMapper.updateById(def) > 0;
    }

    /**
     * 仅修改 {@code flow_definition.activity_status}。
     *
     * <p>D-016 偏离登记。仅携带 {@code id} 与 {@code activityStatus} 两字段的实体传给
     * {@code updateById}：MyBatis-Plus 默认 {@code FieldStrategy.NOT_NULL} ⇒ NULL 字段不入 SET，
     * 等价「单列更新」。返回 {@code int > 0} ⇒ 成功；不存在 id ⇒ 无更新行 ⇒ 抛 {@link ServiceException}。
     *
     * <p>activity_status 与 DDL 真值对照（{@code 04-backend-code/script/sql/ry_workflow.sql:15}）：
     * 0 = 挂起；1 = 激活。非法值（如 {@code -1} / {@code 2}）由调用方校验，本方法不内嵌 whitelist。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateActivityStatus(Long definitionId, Integer activityStatus) {
        if (definitionId == null) {
            throw new ServiceException("流程定义 id 不能为空");
        }
        if (activityStatus == null) {
            throw new ServiceException("activityStatus 不能为空");
        }
        FlowDefinition patch = new FlowDefinition();
        patch.setId(definitionId);
        patch.setActivityStatus(activityStatus);
        int rows = flowDefinitionMapper.updateById(patch);
        if (rows <= 0) {
            // D-016：id 不存在时 MyBatis-Plus 返 0（按主键删改的常规语义）⇒ 抛错而非静默返 false，
            // 便于上层区分「无副作用成功」与「找不到记录」两种场景。
            throw new ServiceException("流程定义不存在或状态未变更：id={}", definitionId);
        }
        log.info("[flw.def.updateActivityStatus] id={} activityStatus={}", definitionId, activityStatus);
        return true;
    }

}
