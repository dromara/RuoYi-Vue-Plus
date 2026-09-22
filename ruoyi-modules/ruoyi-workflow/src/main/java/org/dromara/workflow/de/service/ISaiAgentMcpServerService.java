package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.dromara.common.core.domain.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * sai_agent_mcp_server 服务接口（存根）。
 *
 * <p><b>存根说明</b>：本接口为 OW-03 决议清单的接口层存根（7 端点），
 * 实现由 {@link SaiAgentMcpServerServiceImpl} 全部抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：本表无专属 domain POJO（不实装），用 {@code Map<String, Object>}
 * 作为通用实体。
 *
 * @author AIP
 */
public interface ISaiAgentMcpServerService extends IService<Map<String, Object>> {

    /**
     * 新增（INSERT sai_agent_mcp_server）。
     *
     * @param bo 请求参数
     * @return 新写入的主键 id
     */
    Long add(Map<String, Object> bo);

    /**
     * 修改（UPDATE sai_agent_mcp_server BY id）。
     *
     * @param bo 请求参数（必须含 id）
     * @return true 更新成功
     */
    Boolean edit(Map<String, Object> bo);

    /**
     * 删除（DELETE sai_agent_mcp_server BY id 集合）。
     *
     * @param ids 主键集合
     * @return true 删除成功
     */
    Boolean remove(Collection<Long> ids);

    /**
     * 单条详情（SELECT sai_agent_mcp_server BY id）。
     *
     * @param id 主键
     * @return 单条；未命中返回 null
     */
    Map<String, Object> getInfo(Long id);

    /**
     * 列表（无分页，按 BO 过滤）。
     *
     * @param bo 请求参数
     * @return 列表
     */
    List<Map<String, Object>> list(Map<String, Object> bo);

    /**
     * 分页（按 BO 过滤 + PageQuery 翻页）。
     *
     * @param bo 请求参数
     * @return 分页结果
     */
    PageResult<Map<String, Object>> page(Map<String, Object> bo);

    /**
     * 按外键查询（无分页）。
     *
     * @param fkId 外键 id
     * @return 列表
     */
    List<Map<String, Object>> listByFk(Long fkId);
}