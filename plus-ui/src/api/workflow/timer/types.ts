export interface EventWaitVO {
  id: string | number;
  definitionId?: string | number;
  flowName?: string;
  instanceId?: string | number;
  businessId?: string;
  nodeCode?: string;
  eventType?: string;
  eventTypeName?: string;
  eventName?: string;
  attachedTaskId?: string | number;
  triggerType?: string;
  triggerInterval?: string;
  nextTriggerAt?: string;
  timezone?: string;
  repeatCount?: number;
  maxRepeatCount?: number;
  status?: string;
  statusName?: string;
  retryCount?: number;
  maxRetryCount?: number;
  retryAt?: string;
  payload?: string;
  lastError?: string;
  createTime?: string;
  updateTime?: string;
  /** 定时引擎扫描开关是否开启 */
  timerEnabled?: boolean;
}

export interface EventWaitQuery extends PageQuery {
  /** 等待状态多选，逗号分隔（WAITING / RETRY / RUNNING / DEAD / CANCELLED） */
  statuses?: string;
  /** 事件类型多选，逗号分隔；留空按 timerOnly 取默认范围 */
  eventTypes?: string;
  /** 是否只查定时类事件，默认 true */
  timerOnly?: boolean;
  flowCode?: string;
  businessId?: string;
  nodeCode?: string;
  eventName?: string;
  nextTriggerAtFrom?: string;
  nextTriggerAtTo?: string;
  orderByColumn?: string;
  isAsc?: string;
}

export interface EventWaitSummary {
  waiting: number;
  retry: number;
  running: number;
  dead: number;
  cancelled: number;
}

export interface TimerTriggerForm {
  waitId: string | number;
  businessId?: string;
  variables?: Record<string, unknown>;
}
