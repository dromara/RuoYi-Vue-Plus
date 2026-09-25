package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.SaiModelConfig;

import java.util.Map;

/**
 * 模型配置表 sai_model_config 的 Mapper（已实装）。
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * <p><b>实体类型</b>：本表无专属 domain POJO（不实装），用 {@code Map<String, Object>}
 * 作为通用实体（MyBatis-Plus 兼容）。
 *
 * @author AIP
 */
public interface SaiModelConfigMapper extends BaseMapperPlus<SaiModelConfig, SaiModelConfig> {
}