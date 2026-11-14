import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { OperLogQuery, OperLogVO } from './types';

// 查询操作日志列表
export function list(query: OperLogQuery): AxiosPromise<PageResult<OperLogVO>> {
  return request({
    url: '/monitor/operlog/list',
    method: 'get',
    params: query
  });
}

// 删除操作日志
export function delOperlog(operId: string | number | Array<string | number>) {
  return request({
    url: '/monitor/operlog/' + operId,
    method: 'delete'
  });
}

// 清空操作日志
export function cleanOperlog() {
  return request({
    url: '/monitor/operlog/clean',
    method: 'delete'
  });
}
