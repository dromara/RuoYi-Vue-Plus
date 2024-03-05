package org.dromara.workflow.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ModelBo;
import org.dromara.workflow.domain.vo.ModelVo;
import org.flowable.engine.repository.Model;


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
    TableDataInfo<Model> page(ModelBo modelBo);

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
    ModelVo getInfo(String modelId);

    /**
     * 修改模型信息
     *
     * @param modelBo 模型数据
     * @return 结果
     */
    boolean update(ModelBo modelBo);

    /**
     * 编辑模型XML
     *
     * @param modelBo 模型数据
     * @return 结果
     */
    boolean editModelXml(ModelBo modelBo);

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
