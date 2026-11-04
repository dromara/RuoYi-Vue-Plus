package org.dromara.workflow.domain.bo;

import lombok.Data;
import org.dromara.warm.flow.core.dto.EventWaitQuery;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 定时任务（事件等待记录）查询业务对象。
 * <p>过滤维度与引擎 {@link EventWaitQuery} 一一对应：多值条件取「或」、不同条件之间取「且」；
 * 事件类型留空时按 {@link #timerOnly} 决定是否只查定时类事件。数据来自引擎 flow_event_wait，
 * 本对象只读，不含写操作字段。</p>
 *
 * @author may
 * @since 2026/10/21
 */
@Data
public class FlowEventWaitBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页条数
     */
    private Integer pageSize;

    /**
     * 排序字段（引擎白名单：id / nextTriggerAt / retryAt / createTime / updateTime / status）
     */
    private String orderByColumn;

    /**
     * 排序方向 asc / desc
     */
    private String isAsc;

    /**
     * 等待状态多选（WAITING / RETRY / RUNNING / DEAD / CANCELLED），逗号分隔，留空表示不限
     */
    private String statuses;

    /**
     * 事件类型多选（TIMER_START / TIMER_CATCH / TIMER_BOUNDARY / …），逗号分隔，
     * 留空时按 timerOnly 取默认范围
     */
    private String eventTypes;

    /**
     * 是否只查定时类事件，默认 true
     */
    private Boolean timerOnly = true;

    /**
     * 流程定义编码
     */
    private String flowCode;

    /**
     * 业务流程主键
     */
    private String businessId;

    /**
     * 事件节点编码
     */
    private String nodeCode;

    /**
     * 事件名称（消息名 / 错误码）
     */
    private String eventName;

    /**
     * 下次触发时间下界
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextTriggerAtFrom;

    /**
     * 下次触发时间上界
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextTriggerAtTo;

    /**
     * 转换为引擎查询条件。
     *
     * @return 引擎只读查询条件
     */
    public EventWaitQuery toQuery() {
        EventWaitQuery query = new EventWaitQuery();
        query.setPageNum(pageNum == null || pageNum < 1 ? 1 : pageNum);
        query.setPageSize(pageSize == null || pageSize < 1 ? 10 : pageSize);
        query.setStatuses(splitCsv(statuses));
        query.setEventTypes(splitCsv(eventTypes));
        query.setTimerOnly(!Boolean.FALSE.equals(timerOnly));
        query.setFlowCode(flowCode);
        query.setBusinessId(businessId);
        query.setNodeCode(nodeCode);
        query.setEventName(eventName);
        query.setNextTriggerAtFrom(nextTriggerAtFrom);
        query.setNextTriggerAtTo(nextTriggerAtTo);
        if (orderByColumn != null && !orderByColumn.isBlank()) {
            query.setOrderBy(orderByColumn);
            query.setIsAsc(isAsc);
        }
        return query;
    }

    /**
     * 逗号分隔串拆成引擎多值条件；空串返回 {@code null} 表示不限。
     *
     * @param csv 逗号分隔串
     * @return 多值列表或 null
     */
    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
