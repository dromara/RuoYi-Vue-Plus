package org.dromara.workflow.liteflow.start;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.context.StartProcessContext;

import java.util.Map;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 加载流程定义并补齐启动变量。
 *
 * @author may
 */
@ConditionalOnEnable
@LiteflowComponent("startPrepareInstance")
public class StartPrepareInstanceComponent extends NodeComponent {

    @Override
    public void process() {
        StartProcessContext context = getContextBean(StartProcessContext.class);
        Definition definition = FlowEngine.defService().getPublishByFlowCode(context.getStartProcessBo().getFlowCode());
        if (ObjectUtil.isNull(definition)) {
            throw new ServiceException("流程【" + context.getStartProcessBo().getFlowCode() + "】未发布，请先在流程设计器中发布流程定义");
        }
        context.setDefinition(definition);

        Map<String, Object> variables = context.getVariables();
        Dict dict = JsonUtils.parseMap(definition.getExt());
        boolean autoPass = !ObjectUtil.isNull(dict) && dict.getBool(AUTO_PASS);
        variables.put(AUTO_PASS, autoPass);
        variables.put(BUSINESS_CODE, generateBusinessCode(context.getBizExt()));
    }

    private String generateBusinessCode(FlowInstanceBizExt bizExt) {
        if (ObjectUtil.isNull(bizExt.getBusinessCode()) || bizExt.getBusinessCode().isBlank()) {
            // TODO: 按照自己业务规则生成编号
            String businessCode = Convert.toStr(System.currentTimeMillis());
            bizExt.setBusinessCode(businessCode);
            return businessCode;
        }
        return bizExt.getBusinessCode();
    }

}
