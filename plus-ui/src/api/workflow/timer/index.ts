import type { PageResult } from '@/api/types';
import type { EventWaitQuery, EventWaitSummary, EventWaitVO, TimerTriggerForm } from '@/api/workflow/timer/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

/**
 * 分页查询定时任务（事件等待记录）列表
 * @param query
 */
export const listEventWait = (query?: EventWaitQuery): AxiosPromise<PageResult<EventWaitVO>> => {
  return request({
    url: '/workflow/eventWait/list',
    method: 'get',
    params: query
  });
};

/**
 * 按同一组筛选条件统计各状态等待记录数量
 * @param query
 */
export const summaryEventWait = (query?: EventWaitQuery): AxiosPromise<EventWaitSummary> => {
  return request({
    url: '/workflow/eventWait/summary',
    method: 'get',
    params: query
  });
};

/**
 * 查询定时任务详情
 * @param id
 */
export const getEventWait = (id: string | number): AxiosPromise<EventWaitVO> => {
  return request({
    url: '/workflow/eventWait/' + id,
    method: 'get'
  });
};

/** 恢复已死的事件等待记录，定时启动任务除外 */
export const reviveEventWait = (id: string | number): AxiosPromise<void> => {
  return request({
    url: `/workflow/eventWait/${id}/revive`,
    method: 'post'
  });
};

/**
 * 手动补发一次定时启动任务，返回新建实例主键；不影响该任务的下次触发时间
 * @param data
 */
export const triggerTimerStart = (data: TimerTriggerForm): AxiosPromise<string | number> => {
  return request({
    url: '/workflow/eventWait/trigger',
    method: 'post',
    data: data
  });
};
