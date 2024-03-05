package org.dromara.workflow.flowable.cmd;

import org.dromara.common.core.exception.ServiceException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * 修改流程状态
 *
 * @author may
 */
public class UpdateBusinessStatusCmd implements Command<Boolean> {

    private final String processInstanceId;
    private final String status;

    public UpdateBusinessStatusCmd(String processInstanceId, String status) {
        this.processInstanceId = processInstanceId;
        this.status = status;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        try {
            HistoricProcessInstanceEntityManager manager = CommandContextUtil.getHistoricProcessInstanceEntityManager();
            HistoricProcessInstanceEntity processInstance = manager.findById(processInstanceId);
            processInstance.setBusinessStatus(status);
            manager.update(processInstance);
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
