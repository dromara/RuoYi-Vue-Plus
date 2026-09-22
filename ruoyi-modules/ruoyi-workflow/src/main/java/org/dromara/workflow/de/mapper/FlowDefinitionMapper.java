package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.FlowDefinition;
import org.springframework.stereotype.Component;

/**
 * 流程定义表 flow_definition 的 Mapper。
 *
 * <p>本 mapper 显式声明 bean 名 {@code deFlowDefinitionMapper}，避开
 * warm-flow starter 自带的 {@code org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper}
 * （同名类，默认 bean 名同为 {@code flowDefinitionMapper}，触发
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 的
 * {@code ConflictingBeanDefinitionException}）。</p>
 *
 * @author AIP
 */
@Component("deFlowDefinitionMapper")
public interface FlowDefinitionMapper extends BaseMapperPlus<FlowDefinition, FlowDefinition> {
}
