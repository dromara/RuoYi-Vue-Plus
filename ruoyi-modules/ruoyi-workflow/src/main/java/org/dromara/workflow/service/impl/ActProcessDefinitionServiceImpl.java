package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.dromara.workflow.service.IActProcessDefinitionService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义 服务层实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class ActProcessDefinitionServiceImpl implements IActProcessDefinitionService {

    private final RepositoryService repositoryService;

    /**
     * 分页查询
     *
     * @param processDefinitionBo 参数
     * @return 返回分页列表
     */
    @Override
    public TableDataInfo<ProcessDefinitionVo> getByPage(ProcessDefinitionBo processDefinitionBo) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        query.processDefinitionTenantId(LoginHelper.getTenantId());
        if (StringUtils.isNotEmpty(processDefinitionBo.getKey())) {
            query.processDefinitionKey(processDefinitionBo.getKey());
        }
        if (StringUtils.isNotEmpty(processDefinitionBo.getName())) {
            query.processDefinitionNameLike("%" + processDefinitionBo.getName() + "%");
        }
        // 分页查询
        List<ProcessDefinitionVo> processDefinitionVoList = new ArrayList<>();
        List<ProcessDefinition> definitionList = query.latestVersion().listPage(processDefinitionBo.getPageNum(), processDefinitionBo.getPageSize());
        List<Deployment> deploymentList = null;
        if (CollUtil.isNotEmpty(definitionList)) {
            List<String> deploymentIds = definitionList.stream().map(ProcessDefinition::getDeploymentId).collect(Collectors.toList());
            deploymentList = repositoryService.createDeploymentQuery()
                .deploymentIds(deploymentIds).list();
        }
        for (ProcessDefinition processDefinition : definitionList) {
            ProcessDefinitionVo processDefinitionVo = BeanUtil.toBean(processDefinition, ProcessDefinitionVo.class);
            if (CollUtil.isNotEmpty(deploymentList)) {
                // 部署时间
                deploymentList.stream().filter(e -> e.getId().equals(processDefinition.getDeploymentId())).findFirst().ifPresent(e -> {
                    processDefinitionVo.setDeploymentTime(e.getDeploymentTime());
                });
            }
            processDefinitionVoList.add(processDefinitionVo);
        }
        // 总记录数
        long total = query.count();

        return new TableDataInfo<>(processDefinitionVoList, total);
    }

    /**
     * 查询历史流程定义列表
     *
     * @param key 流程定义key
     */
    @Override
    public List<ProcessDefinitionVo> getProcessDefinitionListByKey(String key) {
        List<ProcessDefinitionVo> processDefinitionVoList = new ArrayList<>();
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> definitionList = query.processDefinitionTenantId(LoginHelper.getTenantId()).processDefinitionKey(key).list();
        List<Deployment> deploymentList = null;
        if (CollUtil.isNotEmpty(definitionList)) {
            List<String> deploymentIds = definitionList.stream().map(ProcessDefinition::getDeploymentId).collect(Collectors.toList());
            deploymentList = repositoryService.createDeploymentQuery()
                .deploymentIds(deploymentIds).list();
        }
        for (ProcessDefinition processDefinition : definitionList) {
            ProcessDefinitionVo processDefinitionVo = BeanUtil.toBean(processDefinition, ProcessDefinitionVo.class);
            if (CollUtil.isNotEmpty(deploymentList)) {
                // 部署时间
                deploymentList.stream().filter(e -> e.getId().equals(processDefinition.getDeploymentId())).findFirst().ifPresent(e -> {
                    processDefinitionVo.setDeploymentTime(e.getDeploymentTime());
                });
            }
            processDefinitionVoList.add(processDefinitionVo);
        }
        return CollectionUtil.reverse(processDefinitionVoList);
    }

    /**
     * 查看流程定义图片
     *
     * @param processDefinitionId 流程定义id
     * @param response            响应
     */
    @Override
    public void processDefinitionImage(String processDefinitionId, HttpServletResponse response) {
        InputStream inputStream = null;
        try {
            // 设置页面不缓存
            response.setHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "no-store");
            response.setDateHeader("Expires", 0);
            inputStream = repositoryService.getProcessDiagram(processDefinitionId);
            // 响应相关图片
            response.setContentType("image/png");

            byte[] bytes = IOUtils.toByteArray(inputStream);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 查看流程定义xml文件
     *
     * @param processDefinitionId 流程定义id
     */
    @Override
    public String processDefinitionXml(String processDefinitionId) {
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
}
