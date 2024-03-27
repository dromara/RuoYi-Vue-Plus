package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.WfCategory;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.dromara.workflow.domain.vo.WfFormDefinitionVo;
import org.dromara.workflow.service.IActProcessDefinitionService;
import org.dromara.workflow.service.IWfCategoryService;
import org.dromara.workflow.service.IWfFormDefinitionService;
import org.dromara.workflow.utils.QueryUtils;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.bpmn.deployer.ResourceNameUtil;
import org.flowable.engine.repository.*;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 流程定义 服务层实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class ActProcessDefinitionServiceImpl implements IActProcessDefinitionService {

    private final RepositoryService repositoryService;
    private final ProcessMigrationService processMigrationService;
    private final IWfCategoryService wfCategoryService;
    private final IWfFormDefinitionService iWfFormDefinitionService;

    /**
     * 分页查询
     *
     * @param bo 参数
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<ProcessDefinitionVo> page(ProcessDefinitionBo bo, PageQuery pageQuery) {
        ProcessDefinitionQuery query = QueryUtils.definitionQuery();
        if (StringUtils.isNotEmpty(bo.getKey())) {
            query.processDefinitionKey(bo.getKey());
        }
        if (StringUtils.isNotEmpty(bo.getCategoryCode())) {
            query.processDefinitionCategory(bo.getCategoryCode());
        }
        if (StringUtils.isNotEmpty(bo.getName())) {
            query.processDefinitionNameLike("%" + bo.getName() + "%");
        }
        query.orderByDeploymentId().desc();
        // 分页查询
        List<ProcessDefinitionVo> processDefinitionVoList = new ArrayList<>();
        List<ProcessDefinition> definitionList = query.latestVersion().listPage(pageQuery.getFirstNum(), pageQuery.getPageSize());
        List<Deployment> deploymentList = null;
        if (CollUtil.isNotEmpty(definitionList)) {
            List<String> deploymentIds = StreamUtils.toList(definitionList, ProcessDefinition::getDeploymentId);
            deploymentList = QueryUtils.deploymentQuery(deploymentIds).list();
        }
        if (CollUtil.isNotEmpty(definitionList)) {
            List<String> ids = StreamUtils.toList(definitionList, ProcessDefinition::getId);
            List<WfFormDefinitionVo> wfFormDefinitionVos = iWfFormDefinitionService.queryList(ids);
            for (ProcessDefinition processDefinition : definitionList) {
                ProcessDefinitionVo processDefinitionVo = BeanUtil.toBean(processDefinition, ProcessDefinitionVo.class);
                if (CollUtil.isNotEmpty(deploymentList)) {
                    // 部署时间
                    deploymentList.stream().filter(e -> e.getId().equals(processDefinition.getDeploymentId())).findFirst().ifPresent(e -> {
                        processDefinitionVo.setDeploymentTime(e.getDeploymentTime());
                    });
                }
                if (CollUtil.isNotEmpty(wfFormDefinitionVos)) {
                    wfFormDefinitionVos.stream().filter(e -> e.getDefinitionId().equals(processDefinition.getId())).findFirst().ifPresent(processDefinitionVo::setWfFormDefinitionVo);
                }
                processDefinitionVoList.add(processDefinitionVo);
            }
        }
        // 总记录数
        long total = query.count();
        TableDataInfo<ProcessDefinitionVo> build = TableDataInfo.build();
        build.setRows(processDefinitionVoList);
        build.setTotal(total);
        return build;
    }

    /**
     * 查询历史流程定义列表
     *
     * @param key 流程定义key
     */
    @Override
    public List<ProcessDefinitionVo> getListByKey(String key) {
        List<ProcessDefinitionVo> processDefinitionVoList = new ArrayList<>();
        ProcessDefinitionQuery query = QueryUtils.definitionQuery();
        List<ProcessDefinition> definitionList = query.processDefinitionKey(key).list();
        List<Deployment> deploymentList = null;
        if (CollUtil.isNotEmpty(definitionList)) {
            List<String> deploymentIds = StreamUtils.toList(definitionList, ProcessDefinition::getDeploymentId);
            deploymentList = QueryUtils.deploymentQuery(deploymentIds).list();
        }
        if (CollUtil.isNotEmpty(definitionList)) {
            List<String> ids = StreamUtils.toList(definitionList, ProcessDefinition::getId);
            List<WfFormDefinitionVo> wfFormDefinitionVos = iWfFormDefinitionService.queryList(ids);
            for (ProcessDefinition processDefinition : definitionList) {
                ProcessDefinitionVo processDefinitionVo = BeanUtil.toBean(processDefinition, ProcessDefinitionVo.class);
                if (CollUtil.isNotEmpty(deploymentList)) {
                    // 部署时间
                    deploymentList.stream().filter(e -> e.getId().equals(processDefinition.getDeploymentId())).findFirst().ifPresent(e -> {
                        processDefinitionVo.setDeploymentTime(e.getDeploymentTime());
                    });
                    if (CollUtil.isNotEmpty(wfFormDefinitionVos)) {
                        wfFormDefinitionVos.stream().filter(e -> e.getDefinitionId().equals(processDefinition.getId())).findFirst().ifPresent(processDefinitionVo::setWfFormDefinitionVo);
                    }
                }
                processDefinitionVoList.add(processDefinitionVo);
            }
        }
        return CollectionUtil.reverse(processDefinitionVoList);
    }

    /**
     * 查看流程定义图片
     *
     * @param processDefinitionId 流程定义id
     */
    @SneakyThrows
    @Override
    public String definitionImage(String processDefinitionId) {
        InputStream inputStream = repositoryService.getProcessDiagram(processDefinitionId);
        return Base64.encode(IOUtils.toByteArray(inputStream));
    }

    /**
     * 查看流程定义xml文件
     *
     * @param processDefinitionId 流程定义id
     */
    @Override
    public String definitionXml(String processDefinitionId) {
        StringBuilder xml = new StringBuilder();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);
        InputStream inputStream;
        try {
            inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getResourceName());
            xml.append(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xml.toString();
    }

    /**
     * 删除流程定义
     *
     * @param deploymentId        部署id
     * @param processDefinitionId 流程定义id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeployment(String deploymentId, String processDefinitionId) {
        try {
            List<HistoricTaskInstance> taskInstanceList = QueryUtils.hisTaskInstanceQuery()
                .processDefinitionId(processDefinitionId).list();
            if (CollectionUtil.isNotEmpty(taskInstanceList)) {
                throw new ServiceException("当前流程定义已被使用不可删除！");
            }
            //删除流程定义
            repositoryService.deleteDeployment(deploymentId);
            //删除表单配置
            iWfFormDefinitionService.getByDefId(processDefinitionId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 激活或者挂起流程定义
     *
     * @param processDefinitionId 流程定义id
     */
    @Override
    public boolean updateDefinitionState(String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = QueryUtils.definitionQuery()
                .processDefinitionId(processDefinitionId).singleResult();
            //将当前为挂起状态更新为激活状态
            //参数说明：参数1：流程定义id,参数2：是否激活（true是否级联对应流程实例，激活了则对应流程实例都可以审批），
            //参数3：什么时候激活，如果为null则立即激活，如果为具体时间则到达此时间后激活
            if (processDefinition.isSuspended()) {
                repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
            } else {
                repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("操作失败:" + e.getMessage());
        }
    }

    /**
     * 迁移流程定义
     *
     * @param currentProcessDefinitionId 当前流程定义id
     * @param fromProcessDefinitionId    需要迁移到的流程定义id
     */

    @Override
    public boolean migrationDefinition(String currentProcessDefinitionId, String fromProcessDefinitionId) {
        try {
            // 迁移验证
            boolean migrationValid = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(currentProcessDefinitionId)
                .validateMigrationOfProcessInstances(fromProcessDefinitionId)
                .isMigrationValid();
            if (!migrationValid) {
                throw new ServiceException("流程定义差异过大无法迁移，请修改流程图");
            }
            // 已结束的流程实例不会迁移
            processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(currentProcessDefinitionId)
                .migrateProcessInstances(fromProcessDefinitionId);
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 流程定义转换为模型
     *
     * @param processDefinitionId 流程定义id
     */
    @Override
    public boolean convertToModel(String processDefinitionId) {
        ProcessDefinition pd = QueryUtils.definitionQuery()
            .processDefinitionId(processDefinitionId).singleResult();
        InputStream inputStream = repositoryService.getResourceAsStream(pd.getDeploymentId(), pd.getResourceName());
        ModelQuery query = QueryUtils.modelQuery();
        Model model = query.modelKey(pd.getKey()).singleResult();
        try {
            if (ObjectUtil.isNotNull(model)) {
                repositoryService.addModelEditorSource(model.getId(), IoUtil.readBytes(inputStream));
            } else {
                Model modelData = repositoryService.newModel();
                modelData.setKey(pd.getKey());
                modelData.setName(pd.getName());
                modelData.setTenantId(pd.getTenantId());
                repositoryService.saveModel(modelData);
                repositoryService.addModelEditorSource(modelData.getId(), IoUtil.readBytes(inputStream));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 通过zip或xml部署流程定义
     *
     * @param file         文件
     * @param categoryCode 分类
     */
    @SneakyThrows
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deployByFile(MultipartFile file, String categoryCode) {

        WfCategory wfCategory = wfCategoryService.queryByCategoryCode(categoryCode);
        if (wfCategory == null) {
            throw new ServiceException("流程分类不存在");
        }
        // 文件后缀名
        String suffix = FileUtil.extName(file.getOriginalFilename());
        InputStream inputStream = file.getInputStream();
        if (FlowConstant.ZIP.equalsIgnoreCase(suffix)) {
            ZipInputStream zipInputStream = null;
            try {
                zipInputStream = new ZipInputStream(inputStream);
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String filename = zipEntry.getName();
                    String[] splitFilename = filename.substring(0, filename.lastIndexOf(".")).split("-");
                    //流程名称
                    String processName = splitFilename[0];
                    //流程key
                    String processKey = splitFilename[1];
                    DeploymentBuilder builder = repositoryService.createDeployment();
                    Deployment deployment = builder.addInputStream(filename, zipInputStream)
                        .tenantId(TenantHelper.getTenantId())
                        .name(processName).key(processKey).category(categoryCode).deploy();
                    ProcessDefinition definition = QueryUtils.definitionQuery().deploymentId(deployment.getId()).singleResult();
                    repositoryService.setProcessDefinitionCategory(definition.getId(), categoryCode);
                    zipInputStream.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            }
        } else {
            String originalFilename = file.getOriginalFilename();
            String bpmnResourceSuffix = ResourceNameUtil.BPMN_RESOURCE_SUFFIXES[0];
            if (originalFilename.contains(bpmnResourceSuffix)) {
                // 文件名 = 流程名称-流程key
                String[] splitFilename = originalFilename.substring(0, originalFilename.lastIndexOf(".")).split("-");
                if (splitFilename.length < 2) {
                    throw new ServiceException("文件名 = 流程名称-流程KEY");
                }
                //流程名称
                String processName = splitFilename[0];
                //流程key
                String processKey = splitFilename[1];
                DeploymentBuilder builder = repositoryService.createDeployment();
                Deployment deployment = builder.addInputStream(originalFilename, inputStream)
                    .tenantId(TenantHelper.getTenantId())
                    .name(processName).key(processKey).category(categoryCode).deploy();
                // 更新分类
                ProcessDefinition definition = QueryUtils.definitionQuery().deploymentId(deployment.getId()).singleResult();
                repositoryService.setProcessDefinitionCategory(definition.getId(), categoryCode);
            } else {
                throw new ServiceException("文件类型上传错误！");
            }
        }

    }
}
