package org.dromara.workflow.service;

import com.warm.flow.core.entity.Definition;
import com.warm.flow.orm.entity.FlowDefinition;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.vo.FlowDefinitionVo;

import java.io.IOException;

/**
 * 流程定义 服务层
 *
 * @author may
 */
public interface IFlwDefinitionService {
    /**
     * 分页查询
     *
     * @param flowDefinition 参数
     * @param pageQuery      分页
     * @return 返回分页列表
     */
    TableDataInfo<FlowDefinitionVo> page(FlowDefinition flowDefinition, PageQuery pageQuery);

    /**
     * 导出流程定义
     *
     * @param id       流程定义id
     * @param response 响应
     * @throws IOException 异常
     */
    void exportDefinition(Long id, HttpServletResponse response) throws IOException;
}
