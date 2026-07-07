package org.dromara.workflow.liteflow.start;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.context.StartProcessContext;

/**
 * 判断启动流程时是否走已有实例续提交分支。
 *
 * @author may
 */
@ConditionalOnEnable
@LiteflowComponent("startExists")
public class StartExistsComponent extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        StartProcessContext context = getContextBean(StartProcessContext.class);
        return ObjectUtil.isNotNull(context.getExistingInstance());
    }

}
