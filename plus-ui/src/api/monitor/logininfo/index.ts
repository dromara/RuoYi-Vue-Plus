import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { LoginInfoQuery, LoginInfoVO } from './types';

// 查询登录日志列表
export function list(query: LoginInfoQuery): AxiosPromise<PageResult<LoginInfoVO>> {
  return request({
    url: '/monitor/loginInfo/list',
    method: 'get',
    params: query
  });
}

// 删除登录日志
export function delLoginInfo(infoId: string | number | Array<string | number>) {
  return request({
    url: '/monitor/loginInfo/' + infoId,
    method: 'delete'
  });
}

// 解锁用户登录状态
export function unlockLoginInfo(userName: string | Array<string>) {
  return request({
    url: '/monitor/loginInfo/unlock/' + userName,
    method: 'get'
  });
}

// 清空登录日志
export function cleanLoginInfo() {
  return request({
    url: '/monitor/loginInfo/clean',
    method: 'delete'
  });
}
