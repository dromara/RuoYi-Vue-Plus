import type { UserQuery, UserVO } from '@/api/system/user/types';
import type { PageResult } from '@/api/types';
import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { RoleDeptTree, RoleQuery, RoleVO } from './types';

export const listRole = (query: RoleQuery): AxiosPromise<PageResult<RoleVO>> => {
  return request({
    url: '/system/role/list',
    method: 'get',
    params: query
  });
};

/**
 * 通过roleIds查询角色
 * @param roleIds
 */
export const optionSelect = (roleIds: (number | string)[]): AxiosPromise<RoleVO[]> => {
  return request({
    url: '/system/role/optionselect?roleIds=' + roleIds,
    method: 'get'
  });
};

/**
 * 查询角色详细
 */
export const getRole = (roleId: string | number): AxiosPromise<RoleVO> => {
  return request({
    url: '/system/role/' + roleId,
    method: 'get'
  });
};

/**
 * 新增角色
 */
export const addRole = (data: any) => {
  return request({
    url: '/system/role',
    method: 'post',
    data: data
  });
};

/**
 * 修改角色基础信息
 * @param data
 */
export const updateRole = (data: any) => {
  return request({
    url: '/system/role',
    method: 'put',
    data: data
  });
};

/**
 * 修改角色权限（菜单权限 + 数据权限）
 */
export const updateRolePermission = (data: any) => {
  return request({
    url: '/system/role/permission',
    method: 'put',
    data: data
  });
};

/**
 * 角色状态修改
 */
export const changeRoleStatus = (roleId: string | number, status: string) => {
  const data = {
    roleId,
    status
  };
  return request({
    url: '/system/role/changeStatus',
    method: 'put',
    data: data
  });
};

/**
 * 删除角色
 */
export const delRole = (roleId: Array<string | number> | string | number) => {
  return request({
    url: '/system/role/' + roleId,
    method: 'delete'
  });
};

/**
 * 查询角色已授权用户列表
 */
export const allocatedUserList = (query: UserQuery): AxiosPromise<PageResult<UserVO>> => {
  return request({
    url: '/system/role/authUser/allocatedList',
    method: 'get',
    params: query
  });
};

/**
 * 查询角色未授权用户列表
 */
export const unallocatedUserList = (query: UserQuery): AxiosPromise<PageResult<UserVO>> => {
  return request({
    url: '/system/role/authUser/unallocatedList',
    method: 'get',
    params: query
  });
};

/**
 * 取消用户授权角色
 */
export const authUserCancel = (data: any) => {
  return request({
    url: '/system/role/authUser/cancel',
    method: 'put',
    data: data
  });
};

/**
 * 批量取消用户授权角色
 */
export const authUserCancelAll = (data: any) => {
  return request({
    url: '/system/role/authUser/cancelAll',
    method: 'put',
    params: data
  });
};

/**
 * 授权用户选择
 */
export const authUserSelectAll = (data: any) => {
  return request({
    url: '/system/role/authUser/selectAll',
    method: 'put',
    params: data
  });
};
// 根据角色ID查询部门树结构
export const deptTreeSelect = (roleId: string | number): AxiosPromise<RoleDeptTree> => {
  return request({
    url: '/system/role/deptTree/' + roleId,
    method: 'get'
  });
};

export default {
  optionSelect,
  listRole
};
