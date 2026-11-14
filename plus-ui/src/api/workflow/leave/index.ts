import type { PageResult } from '@/api/types';
import type { LeaveForm, LeaveQuery, LeaveVO } from '@/api/workflow/leave/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

/**
 * 查询请假列表
 * @param query
 * @returns {*}
 */

export const listLeave = (query?: LeaveQuery): AxiosPromise<PageResult<LeaveVO>> => {
  return request({
    url: '/workflow/leave/list',
    method: 'get',
    params: query
  });
};

/**
 * 查询请假详细
 * @param id
 */
export const getLeave = (id: string | number): AxiosPromise<LeaveVO> => {
  return request({
    url: '/workflow/leave/' + id,
    method: 'get'
  });
};

/**
 * 新增请假
 * @param data
 */
export const addLeave = (data: LeaveForm): AxiosPromise<LeaveVO> => {
  return request({
    url: '/workflow/leave',
    method: 'post',
    data: data
  });
};

/**
 * 提交请假并发起流程
 * @param data
 */
export const submitAndFlowStart = (data: LeaveForm): AxiosPromise<LeaveVO> => {
  return request({
    url: '/workflow/leave/submitAndFlowStart',
    method: 'post',
    data: data
  });
};

/**
 * 修改请假
 * @param data
 */
export const updateLeave = (data: LeaveForm): AxiosPromise<LeaveVO> => {
  return request({
    url: '/workflow/leave',
    method: 'put',
    data: data
  });
};

/**
 * 删除请假
 * @param id
 */
export const delLeave = (id: string | number | Array<string | number>) => {
  return request({
    url: '/workflow/leave/' + id,
    method: 'delete'
  });
};
