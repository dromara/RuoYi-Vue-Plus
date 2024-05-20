package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 流程定义 服务层
 *
 * @author may
 */
public interface IActProcessDefinitionService {
    /**
     * 分页查询
     *
     * @param processDefinitionBo 参数
     * @param pageQuery           分页
     * @return 返回分页列表
     */
    TableDataInfo<ProcessDefinitionVo> page(ProcessDefinitionBo processDefinitionBo, PageQuery pageQuery);

    /**
     * 查询历史流程定义列表
     *
     * @param key 流程定义key
     * @return 结果
     */
    List<ProcessDefinitionVo> getListByKey(String key);

    /**
     * 查看流程定义图片
     *
     * @param processDefinitionId 流程定义id
     * @return 结果
     */
    String definitionImage(String processDefinitionId);

    /**
     * 查看流程定义xml文件
     *
     * @param processDefinitionId 流程定义id
     * @return 结果
     */
    String definitionXml(String processDefinitionId);

    /**
     * 删除流程定义
     *
     * @param deploymentIds        部署id
     * @param processDefinitionIds 流程定义id
     * @return 结果
     */
    boolean deleteDeployment(List<String> deploymentIds, List<String> processDefinitionIds);

    /**
     * 激活或者挂起流程定义
     *
     * @param processDefinitionId 流程定义id
     * @return 结果
     */
    boolean updateDefinitionState(String processDefinitionId);

    /**
     * 迁移流程定义
     *
     * @param currentProcessDefinitionId 当前流程定义id
     * @param fromProcessDefinitionId    需要迁移到的流程定义id
     * @return 结果
     */
    boolean migrationDefinition(String currentProcessDefinitionId, String fromProcessDefinitionId);

    /**
     * 流程定义转换为模型
     *
     * @param processDefinitionId 流程定义id
     * @return 结果
     */
    boolean convertToModel(String processDefinitionId);

    /**
     * 通过zip或xml部署流程定义
     *
     * @param file         文件
     * @param categoryCode 分类
     */
    void deployByFile(MultipartFile file, String categoryCode);
}
