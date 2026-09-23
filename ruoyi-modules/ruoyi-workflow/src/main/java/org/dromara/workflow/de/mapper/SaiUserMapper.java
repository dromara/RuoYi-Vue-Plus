package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.SaiUser;

import java.util.Map;

/**
 * 数字员工用户表 sai_user 的 Mapper（存根）。
 *
 * <p><b>存根说明</b>：本类为接口层存根（OW-03 决议），方法体由
 * {@link org.dromara.workflow.de.service.SaiUserServiceImpl} 抛出
 * {@code UnsupportedOperationException}，等待 owner 实装。
 *
 * <p><b>实体类型</b>：本表无专属 domain POJO（不实装），用 {@code Map<String, Object>}
 * 作为通用实体（MyBatis-Plus 兼容）。
 *
 * @author AIP
 */
public interface SaiUserMapper extends BaseMapperPlus<SaiUser, SaiUser> {
}