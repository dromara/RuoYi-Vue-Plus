import type { DemoForm, DemoQuery, DemoVO } from '@/api/demo/demo/types';
import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

/**
 * 查询测试单列表
 * @param query
 * @returns {*}
 */
export const listDemo = (query?: DemoQuery): AxiosPromise<PageResult<DemoVO>> => {
  return request({
    url: '/demo/demo/list',
    method: 'get',
    params: query
  });
};

/**
 * 查询测试单详细
 * @param id
 */
export const getDemo = (id: string | number): AxiosPromise<DemoVO> => {
  return request({
    url: '/demo/demo/' + id,
    method: 'get'
  });
};

/**
 * 新增测试单
 * @param data
 */
export const addDemo = (data: DemoForm) => {
  return request({
    url: '/demo/demo',
    method: 'post',
    data: data
  });
};

/**
 * 修改测试单
 * @param data
 */
export const updateDemo = (data: DemoForm) => {
  return request({
    url: '/demo/demo',
    method: 'put',
    data: data
  });
};

/**
 * 删除测试单
 * @param id
 */
export const delDemo = (id: string | number | Array<string | number>) => {
  return request({
    url: '/demo/demo/' + id,
    method: 'delete'
  });
};
