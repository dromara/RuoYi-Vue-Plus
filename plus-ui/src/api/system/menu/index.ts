import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { MenuForm, MenuQuery, MenuTreeOption, MenuVO, RoleMenuTree } from './types';

// 查询菜单列表
export const listMenu = (query?: MenuQuery): AxiosPromise<MenuVO[]> => {
  return request({
    url: '/system/menu/list',
    method: 'get',
    params: query
  });
};

// 查询菜单详细
export const getMenu = (menuId: string | number): AxiosPromise<MenuVO> => {
  return request({
    url: '/system/menu/' + menuId,
    method: 'get'
  });
};

// 查询菜单下拉树结构
export const treeselect = (): AxiosPromise<MenuTreeOption[]> => {
  return request({
    url: '/system/menu/treeselect',
    method: 'get'
  });
};

// 根据角色ID查询菜单下拉树结构
export const roleMenuTreeselect = (roleId: string | number): AxiosPromise<RoleMenuTree> => {
  return request({
    url: '/system/menu/roleMenuTreeselect/' + roleId,
    method: 'get'
  });
};

// 新增菜单
export const addMenu = (data: MenuForm) => {
  return request({
    url: '/system/menu',
    method: 'post',
    data: data
  });
};

// 修改菜单
export const updateMenu = (data: MenuForm) => {
  return request({
    url: '/system/menu',
    method: 'put',
    data: data
  });
};

// 删除菜单
export const delMenu = (menuId: string | number) => {
  return request({
    url: '/system/menu/' + menuId,
    method: 'delete'
  });
};

// 级联删除菜单
export const cascadeDelMenu = (menuIds: Array<string | number>) => {
  return request({
    url: '/system/menu/cascade/' + menuIds,
    method: 'delete'
  });
};
