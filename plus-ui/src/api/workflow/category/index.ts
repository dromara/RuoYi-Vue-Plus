import type { CategoryForm, CategoryQuery, CategoryTreeVO, CategoryVO } from '@/api/workflow/category/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';

/**
 * 查询流程分类列表
 * @param query
 * @returns {*}
 */

export const listCategory = (query?: CategoryQuery): AxiosPromise<CategoryVO[]> => {
  return request({
    url: '/workflow/category/list',
    method: 'get',
    params: query
  });
};

/**
 * 查询流程分类详细
 * @param categoryId
 */
export const getCategory = (categoryId: string | number): AxiosPromise<CategoryVO> => {
  return request({
    url: '/workflow/category/' + categoryId,
    method: 'get'
  });
};

/**
 * 新增流程分类
 * @param data
 */
export const addCategory = (data: CategoryForm) => {
  return request({
    url: '/workflow/category',
    method: 'post',
    data: data
  });
};

/**
 * 修改流程分类
 * @param data
 */
export const updateCategory = (data: CategoryForm) => {
  return request({
    url: '/workflow/category',
    method: 'put',
    data: data
  });
};

/**
 * 删除流程分类
 * @param categoryId
 */
export const delCategory = (categoryId: string | number | Array<string | number>) => {
  return request({
    url: '/workflow/category/' + categoryId,
    method: 'delete'
  });
};

/**
 * 获取流程分类树列表
 * @param query 流程实例id
 * @returns
 */
export const categoryTree = (query?: CategoryForm): AxiosPromise<CategoryTreeVO[]> => {
  return request({
    url: `/workflow/category/categoryTree`,
    method: 'get',
    params: query
  });
};
