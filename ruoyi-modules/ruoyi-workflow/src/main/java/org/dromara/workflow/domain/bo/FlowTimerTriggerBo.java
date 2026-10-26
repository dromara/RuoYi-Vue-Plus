package org.dromara.workflow.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 定时启动任务手动补发业务对象。
 * <p>只对 TIMER_START 事件的等待记录生效；补发只创建一次实例，不读取也不推进等待记录，
 * 定时任务的下次触发时间不受影响。</p>
 *
 * @author may
 * @since 2026/10/21
 */
@Data
public class FlowTimerTriggerBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件等待记录主键
     */
    private Long waitId;

    /**
     * 业务主键，留空时生成数字雪花 ID
     */
    private String businessId;

    /**
     * 额外流程变量，合并到节点默认变量之后
     */
    private Map<String, Object> variables;
}
