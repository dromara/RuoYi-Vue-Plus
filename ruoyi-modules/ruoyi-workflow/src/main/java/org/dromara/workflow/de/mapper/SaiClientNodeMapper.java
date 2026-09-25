package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.SaiClientNode;

import java.util.Map;

/**
 * 客户端节点表 sai_client_node 的 Mapper（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：本表无专属 domain POJO（不实装），用 {@code Map<String, Object>}
 * 作为通用实体（MyBatis-Plus 兼容）。
 *
 * @author AIP
 */
public interface SaiClientNodeMapper extends BaseMapperPlus<SaiClientNode, SaiClientNode> {
}