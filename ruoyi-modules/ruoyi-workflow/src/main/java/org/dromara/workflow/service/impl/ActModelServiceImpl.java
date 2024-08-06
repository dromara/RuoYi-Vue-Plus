package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.WfNodeConfig;
import org.dromara.workflow.domain.bo.ModelBo;
import org.dromara.workflow.domain.bo.WfDefinitionConfigBo;
import org.dromara.workflow.domain.vo.ModelVo;
import org.dromara.workflow.domain.vo.WfDefinitionConfigVo;
import org.dromara.workflow.service.IActModelService;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.dromara.workflow.service.IWfNodeConfigService;
import org.dromara.workflow.utils.ModelUtils;
import org.dromara.workflow.utils.QueryUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.validation.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 模型管理 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ActModelServiceImpl implements IActModelService {

    @Autowired(required = false)
    private RepositoryService repositoryService;
    private final IWfNodeConfigService wfNodeConfigService;
    private final IWfDefinitionConfigService wfDefinitionConfigService;

    /**
     * 分页查询模型
     *
     * @param modelBo 模型参数
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<Model> page(ModelBo modelBo, PageQuery pageQuery) {
        ModelQuery query = QueryUtils.modelQuery();
        if (StringUtils.isNotBlank(modelBo.getName())) {
            query.modelNameLike("%" + modelBo.getName() + "%");
        }
        if (StringUtils.isNotBlank(modelBo.getKey())) {
            query.modelKey(modelBo.getKey());
        }
        if (StringUtils.isNotBlank(modelBo.getCategoryCode())) {
            query.modelCategory(modelBo.getCategoryCode());
        }
        query.orderByLastUpdateTime().desc();
        // 创建时间降序排列
        query.orderByCreateTime().desc();
        // 分页查询
        List<Model> modelList = query.listPage(pageQuery.getFirstNum(), pageQuery.getPageSize());
        // 总记录数
        long total = query.count();
        TableDataInfo<Model> build = TableDataInfo.build();
        build.setRows(modelList);
        build.setTotal(total);
        return build;
    }

    /**
     * 新增模型
     *
     * @param modelBo 模型请求对象
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNewModel(ModelBo modelBo) {
        try {
            int version = 0;
            String key = modelBo.getKey();
            String name = modelBo.getName();
            String description = modelBo.getDescription();
            String categoryCode = modelBo.getCategoryCode();
            String xml = modelBo.getXml();
            Model checkModel = QueryUtils.modelQuery().modelKey(key).singleResult();
            if (ObjectUtil.isNotNull(checkModel)) {
                throw new ServiceException("模型key已存在！");
            }
            //初始空的模型
            Model model = repositoryService.newModel();
            model.setKey(key);
            model.setName(name);
            model.setVersion(version);
            model.setCategory(categoryCode);
            model.setMetaInfo(description);
            model.setTenantId(TenantHelper.getTenantId());
            //保存初始化的模型基本信息数据
            repositoryService.saveModel(model);
            repositoryService.addModelEditorSource(model.getId(), StrUtil.utf8Bytes(xml));
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 查询模型
     *
     * @param id 模型id
     * @return 模型数据
     */
    @Override
    public ModelVo getInfo(String id) {
        ModelVo modelVo = new ModelVo();
        Model model = repositoryService.getModel(id);
        if (model != null) {
            try {
                byte[] modelEditorSource = repositoryService.getModelEditorSource(model.getId());
                modelVo.setXml(StrUtil.utf8Str(modelEditorSource));
                modelVo.setId(model.getId());
                modelVo.setKey(model.getKey());
                modelVo.setName(model.getName());
                modelVo.setCategoryCode(model.getCategory());
                modelVo.setDescription(model.getMetaInfo());
                return modelVo;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ServiceException(e.getMessage());
            }
        }
        return modelVo;
    }

    /**
     * 修改模型信息
     *
     * @param modelBo 模型数据
     * @return 结果
     */
    @Override
    public boolean update(ModelBo modelBo) {
        try {
            Model model = repositoryService.getModel(modelBo.getId());
            List<Model> list = QueryUtils.modelQuery().modelKey(modelBo.getKey()).list();
            list.stream().filter(e -> !e.getId().equals(model.getId())).findFirst().ifPresent(e -> {
                throw new ServiceException("模型KEY已存在！");
            });
            model.setCategory(modelBo.getCategoryCode());
            model.setMetaInfo(modelBo.getDescription());
            repositoryService.saveModel(model);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 编辑模型XML
     *
     * @param modelBo 模型数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editModelXml(ModelBo modelBo) {
        try {
            String xml = modelBo.getXml();
            String svg = modelBo.getSvg();
            String modelId = modelBo.getId();
            String key = modelBo.getKey();
            String name = modelBo.getName();
            BpmnModel bpmnModel = ModelUtils.xmlToBpmnModel(xml);
            ModelUtils.checkBpmnModel(bpmnModel);
            Model model = repositoryService.getModel(modelId);
            List<Model> list = QueryUtils.modelQuery().modelKey(key).list();
            list.stream().filter(e -> !e.getId().equals(model.getId())).findFirst().ifPresent(e -> {
                throw new ServiceException("模型KEY已存在！");
            });
            // 校验key命名规范
            if (!Validator.isMatchRegex(FlowConstant.MODEL_KEY_PATTERN, key)) {
                throw new ServiceException("模型标识KEY只能字符或者下划线开头！");
            }
            model.setKey(key);
            model.setName(name);
            model.setVersion(model.getVersion() + 1);
            repositoryService.saveModel(model);
            repositoryService.addModelEditorSource(model.getId(), StrUtil.utf8Bytes(xml));
            // 转换图片
            InputStream svgStream = new ByteArrayInputStream(StrUtil.utf8Bytes(svg));
            TranscoderInput input = new TranscoderInput(svgStream);

            PNGTranscoder transcoder = new PNGTranscoder();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outStream);

            transcoder.transcode(input, output);
            final byte[] result = outStream.toByteArray();
            repositoryService.addModelEditorSourceExtra(model.getId(), result);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 模型部署
     *
     * @param id 模型id
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean modelDeploy(String id) {
        try {
            // 查询流程定义模型xml
            byte[] xmlBytes = repositoryService.getModelEditorSource(id);
            if (ArrayUtil.isEmpty(xmlBytes)) {
                throw new ServiceException("模型数据为空，请先设计流程定义模型，再进行部署！");
            }
            if (JSONUtil.isTypeJSON(new String(xmlBytes, StandardCharsets.UTF_8))) {
                byte[] bytes = ModelUtils.bpmnJsonToXmlBytes(xmlBytes);
                if (ArrayUtil.isEmpty(bytes)) {
                    throw new ServiceException("模型不能为空，请至少设计一条主线流程！");
                }
            }
            BpmnModel bpmnModel = ModelUtils.xmlToBpmnModel(xmlBytes);
            // 校验模型
            ModelUtils.checkBpmnModel(bpmnModel);
            List<ValidationError> validationErrors = repositoryService.validateProcess(bpmnModel);
            if (CollUtil.isNotEmpty(validationErrors)) {
                String errorMsg = validationErrors.stream().map(ValidationError::getProblem).distinct().collect(Collectors.joining(","));
                throw new ServiceException(errorMsg);
            }
            // 查询模型的基本信息
            Model model = repositoryService.getModel(id);
            ProcessDefinition processDefinition = QueryUtils.definitionQuery().processDefinitionKey(model.getKey()).latestVersion().singleResult();
            // xml资源的名称 ，对应act_ge_bytearray表中的name_字段
            String processName = model.getName() + ".bpmn20.xml";
            // 调用部署相关的api方法进行部署流程定义
            Deployment deployment = repositoryService.createDeployment()
                // 部署名称
                .name(model.getName())
                // 部署标识key
                .key(model.getKey())
                // 部署流程分类
                .category(model.getCategory())
                // bpmn20.xml资源
                .addBytes(processName, xmlBytes)
                // 租户id
                .tenantId(TenantHelper.getTenantId())
                .deploy();

            // 更新 部署id 到流程定义模型数据表中
            model.setDeploymentId(deployment.getId());
            repositoryService.saveModel(model);
            // 更新分类
            ProcessDefinition definition = QueryUtils.definitionQuery().deploymentId(deployment.getId()).singleResult();
            repositoryService.setProcessDefinitionCategory(definition.getId(), model.getCategory());
            //更新流程定义配置
            if (processDefinition != null) {
                WfDefinitionConfigVo definitionVo = wfDefinitionConfigService.getByDefId(processDefinition.getId());
                if (definitionVo != null) {
                    wfDefinitionConfigService.deleteByDefIds(Collections.singletonList(processDefinition.getId()));
                    WfDefinitionConfigBo wfFormDefinition = new WfDefinitionConfigBo();
                    wfFormDefinition.setDefinitionId(definition.getId());
                    wfFormDefinition.setProcessKey(definition.getKey());
                    wfFormDefinition.setTableName(definitionVo.getTableName());
                    wfFormDefinition.setVersion(definition.getVersion());
                    wfFormDefinition.setRemark(definitionVo.getRemark());
                    wfDefinitionConfigService.saveOrUpdate(wfFormDefinition);
                }
            }
            //更新流程节点配置表单
            List<UserTask> userTasks = ModelUtils.getUserTaskFlowElements(definition.getId());
            UserTask applyUserTask = ModelUtils.getApplyUserTask(definition.getId());
            List<WfNodeConfig> wfNodeConfigList = new ArrayList<>();
            for (UserTask userTask : userTasks) {
                if (StringUtils.isNotBlank(userTask.getFormKey()) && userTask.getFormKey().contains(StrUtil.COLON)) {
                    WfNodeConfig wfNodeConfig = new WfNodeConfig();
                    wfNodeConfig.setNodeId(userTask.getId());
                    wfNodeConfig.setNodeName(userTask.getName());
                    wfNodeConfig.setDefinitionId(definition.getId());
                    String[] split = userTask.getFormKey().split(StrUtil.COLON);
                    wfNodeConfig.setFormType(split[0]);
                    wfNodeConfig.setFormId(Long.valueOf(split[1]));
                    wfNodeConfig.setApplyUserTask(applyUserTask.getId().equals(userTask.getId()) ? FlowConstant.TRUE : FlowConstant.FALSE);
                    wfNodeConfigList.add(wfNodeConfig);
                }
            }
            if (CollUtil.isNotEmpty(wfNodeConfigList)) {
                wfNodeConfigService.saveOrUpdate(wfNodeConfigList);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 导出模型zip压缩包
     *
     * @param modelIds 模型id
     * @param response 相应
     */
    @Override
    public void exportZip(List<String> modelIds, HttpServletResponse response) {
        try (ZipOutputStream zos = ZipUtil.getZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            // 压缩包文件名
            String zipName = "模型不存在";
            // 查询模型基本信息
            for (String modelId : modelIds) {
                Model model = repositoryService.getModel(modelId);
                byte[] xmlBytes = repositoryService.getModelEditorSource(modelId);
                if (ObjectUtil.isNotNull(model)) {
                    if (JSONUtil.isTypeJSON(new String(xmlBytes, StandardCharsets.UTF_8)) && ArrayUtil.isEmpty(ModelUtils.bpmnJsonToXmlBytes(xmlBytes))) {
                        zipName = "模型不能为空，请至少设计一条主线流程！";
                        zos.putNextEntry(new ZipEntry(zipName + ".txt"));
                        zos.write(zipName.getBytes(StandardCharsets.UTF_8));
                    } else if (ArrayUtil.isEmpty(xmlBytes)) {
                        zipName = "模型数据为空，请先设计流程定义模型，再进行部署！";
                        zos.putNextEntry(new ZipEntry(zipName + ".txt"));
                        zos.write(zipName.getBytes(StandardCharsets.UTF_8));
                    } else {
                        String fileName = model.getName() + "-" + model.getKey();
                        // 压缩包文件名
                        zipName = fileName + ".zip";
                        // 将xml添加到压缩包中(指定xml文件名：请假流程.bpmn20.xml
                        zos.putNextEntry(new ZipEntry(fileName + ".bpmn20.xml"));
                        zos.write(xmlBytes);
                    }
                }
            }
            response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(zipName, StandardCharsets.UTF_8) + ".zip");
            response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
            // 刷出响应流
            response.flushBuffer();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 复制模型
     *
     * @param modelBo 模型数据
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copyModel(ModelBo modelBo) {
        try {
            String key = modelBo.getKey();
            if (StringUtils.isNotBlank(key)) {
                // 查询模型
                Model model = repositoryService.createModelQuery().modelId(modelBo.getId()).singleResult();
                if (ObjectUtil.isNotNull(model)) {
                    byte[] modelEditorSource = repositoryService.getModelEditorSource(model.getId());
                    List<Model> list = QueryUtils.modelQuery().modelKey(key).list();
                    if (CollUtil.isNotEmpty(list)) {
                        throw new ServiceException("模型KEY已存在！");
                    }
                    // 校验key命名规范
                    if (!Validator.isMatchRegex(FlowConstant.MODEL_KEY_PATTERN, key)) {
                        throw new ServiceException("模型标识KEY只能字符或者下划线开头！");
                    }
                    // 复制模型数据
                    Model newModel = repositoryService.newModel();
                    newModel.setKey(modelBo.getKey());
                    newModel.setName(modelBo.getName());
                    newModel.setCategory(modelBo.getCategoryCode());
                    newModel.setVersion(1);
                    newModel.setMetaInfo(modelBo.getDescription());
                    newModel.setTenantId(TenantHelper.getTenantId());
                    String xml = StrUtil.utf8Str(modelEditorSource);
                    BpmnModel bpmnModel = ModelUtils.xmlToBpmnModel(xml);
                    Process mainProcess = bpmnModel.getMainProcess();
                    mainProcess.setId(modelBo.getKey());
                    mainProcess.setName(modelBo.getName());
                    byte[] xmlBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
                    repositoryService.saveModel(newModel);
                    repositoryService.addModelEditorSource(newModel.getId(), xmlBytes);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }
}
