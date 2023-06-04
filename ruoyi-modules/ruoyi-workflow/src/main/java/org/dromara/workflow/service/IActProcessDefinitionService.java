package org.dromara.workflow.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ProcessDefinitionBo;
import org.dromara.workflow.domain.vo.ProcessDefinitionVo;
import org.flowable.engine.repository.ProcessDefinition;

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
     * @return 返回分页列表
     */
    TableDataInfo<ProcessDefinitionVo> getByPage(ProcessDefinitionBo processDefinitionBo);

    /**
     * 查询历史流程定义列表
     *
     * @param key 流程定义key
     * @return 结果
     */
    List<ProcessDefinitionVo> getProcessDefinitionListByKey(String key);

    /**
     * 查看流程定义图片
     *
     * @param processDefinitionId 流程定义id
     * @param response            响应
     */
    void processDefinitionImage(String processDefinitionId, HttpServletResponse response);

    /**
     * 查看流程定义xml文件
     *
     * @param processDefinitionId 流程定义id
     * @return 结果
     */
    String processDefinitionXml(String processDefinitionId);
}
