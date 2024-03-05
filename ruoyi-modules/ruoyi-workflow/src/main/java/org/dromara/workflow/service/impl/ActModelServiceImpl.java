package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.ModelBo;
import org.dromara.workflow.domain.vo.ModelVo;
import org.dromara.workflow.service.IActModelService;
import org.dromara.workflow.utils.ModelUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.validation.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 模型管理 服务层实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class ActModelServiceImpl implements IActModelService {

    private final RepositoryService repositoryService;

    /**
     * 分页查询模型
     *
     * @param modelBo 模型参数
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<Model> page(ModelBo modelBo) {
        ModelQuery query = repositoryService.createModelQuery();
        query.modelTenantId(TenantHelper.getTenantId());
        if (StringUtils.isNotEmpty(modelBo.getName())) {
            query.modelNameLike("%" + modelBo.getName() + "%");
        }
        if (StringUtils.isNotEmpty(modelBo.getKey())) {
            query.modelKey(modelBo.getKey());
        }
        if (StringUtils.isNotEmpty(modelBo.getCategoryCode())) {
            query.modelCategory(modelBo.getCategoryCode());
        }
        query.orderByLastUpdateTime().desc();
        // 创建时间降序排列
        query.orderByCreateTime().desc();
        // 分页查询
        List<Model> modelList = query.listPage(modelBo.getPageNum(), modelBo.getPageSize());
        // 总记录数
        long total = query.count();
        return new TableDataInfo<>(modelList, total);
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
            Model checkModel = repositoryService.createModelQuery().modelKey(key).modelTenantId(TenantHelper.getTenantId()).singleResult();
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
            e.printStackTrace();
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
            List<Model> list = repositoryService.createModelQuery().modelTenantId(TenantHelper.getTenantId()).modelKey(modelBo.getKey()).list();
            list.stream().filter(e -> !e.getId().equals(model.getId())).findFirst().ifPresent(e -> {
                throw new ServiceException("模型KEY已存在！");
            });
            model.setCategory(modelBo.getCategoryCode());
            model.setMetaInfo(modelBo.getDescription());
            repositoryService.saveModel(model);
        } catch (Exception e) {
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
            List<Model> list = repositoryService.createModelQuery().modelTenantId(TenantHelper.getTenantId()).modelKey(key).list();
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
            e.printStackTrace();
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
            if (JSONUtil.isTypeJSON(IOUtils.toString(xmlBytes, StandardCharsets.UTF_8.toString()))) {
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
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            repositoryService.setProcessDefinitionCategory(definition.getId(), model.getCategory());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 导出模型zip压缩包
     *
     * @param modelId  模型id
     * @param response 相应
     */
    @Override
    public void exportZip(String modelId, HttpServletResponse response) {
        ZipOutputStream zos = null;
        try {
            zos = ZipUtil.getZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8);
            // 压缩包文件名
            String zipName = "模型不存在";
            // 查询模型基本信息
            Model model = repositoryService.getModel(modelId);
            byte[] xmlBytes = repositoryService.getModelEditorSource(modelId);
            if (ObjectUtil.isNotNull(model)) {
                if (JSONUtil.isTypeJSON(IOUtils.toString(xmlBytes, StandardCharsets.UTF_8.toString())) && ArrayUtil.isEmpty(ModelUtils.bpmnJsonToXmlBytes(xmlBytes))) {
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
            response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(zipName, StandardCharsets.UTF_8) + ".zip");
            // 刷出响应流
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zos != null) {
                try {
                    zos.closeEntry();
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
