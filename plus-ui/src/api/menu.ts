import type { RouteRecordRaw } from 'vue-router';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

// 获取路由
export function getRouters(): AxiosPromise<RouteRecordRaw[]> {
  return request({
    url: '/system/menu/getRouters',
    method: 'get'
  });
}
