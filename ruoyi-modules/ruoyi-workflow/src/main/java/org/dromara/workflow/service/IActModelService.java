package org.dromara.workflow.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ModelBo;
import org.flowable.engine.repository.Model;
import org.springframework.util.MultiValueMap;

/**
 * 模型管理 服务层
 *
 * @author may
 */
public interface IActModelService {
    /**
     * 分页查询模型
     *
     * @param modelBo 模型参数
     * @return 返回分页列表
     */
    TableDataInfo<Model> getByPage(ModelBo modelBo);

    /**
     * 新增模型
     *
     * @param modelBo 模型请求对象
     * @return 结果
     */
    boolean saveNewModel(ModelBo modelBo);

    /**
     * 查询模型
     *
     * @param modelId 模型id
     * @return 模型数据
     */
    ObjectNode getModelInfo(String modelId);

    /**
     * 编辑模型
     *
     * @param modelId 模型id
     * @param values  模型数据
     * @return 结果
     */
    boolean editModel(String modelId, MultiValueMap<String, String> values);

    /**
     * 模型部署
     *
     * @param id 模型id
     * @return 结果
     */
    boolean modelDeploy(String id);

    /**
     * 导出模型zip压缩包
     *
     * @param modelId  模型id
     * @param response 相应
     */
    void exportZip(String modelId, HttpServletResponse response);
}
