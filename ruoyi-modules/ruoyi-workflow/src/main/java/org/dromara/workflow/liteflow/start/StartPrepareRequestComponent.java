package org.dromara.workflow.liteflow.start;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.context.StartProcessContext;

import java.util.Map;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 准备流程启动请求。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("startPrepareRequest")
public class StartPrepareRequestComponent extends NodeComponent {

    private final FlowInstanceMapper flowInstanceMapper;

    @Override
    public void process() {
        StartProcessContext context = getContextBean(StartProcessContext.class);
        String businessId = context.getStartProcessBo().getBusinessId();
        if (StringUtils.isBlank(businessId)) {
            throw new ServiceException("启动工作流时必须包含业务ID");
        }
        context.setBusinessId(businessId);
        context.setVariables(context.getStartProcessBo().getVariables());
        context.setBizExt(context.getStartProcessBo().getBizExt());

        Map<String, Object> variables = context.getVariables();
        variables.put(INITIATOR, LoginHelper.getUserIdStr());
        variables.put(INITIATOR_DEPT_ID, LoginHelper.getDeptId());
        variables.put(BUSINESS_ID, businessId);

        FlowInstance flowInstance = flowInstanceMapper.selectOne(QueryBuilder.lambda(FlowInstance.class)
            .eq(FlowInstance::getBusinessId, businessId)
            .build());
        context.setExistingInstance(flowInstance);
    }

}
