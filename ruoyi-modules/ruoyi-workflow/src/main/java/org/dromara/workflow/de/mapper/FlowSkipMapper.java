package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.FlowSkip;
import org.springframework.stereotype.Component;

/**
 * 流程跳转表 flow_skip 的 Mapper。
 *
 * <p>本 mapper 显式声明 bean 名 {@code deFlowSkipMapper}，避开 warm-flow starter
 * 自带的同名 {@code org.dromara.warm.flow.orm.mapper.FlowSkipMapper}，避免
 * {@code ConflictingBeanDefinitionException}。</p>
 *
 * @author AIP
 */
@Component("deFlowSkipMapper")
public interface FlowSkipMapper extends BaseMapperPlus<FlowSkip, FlowSkip> {
}
