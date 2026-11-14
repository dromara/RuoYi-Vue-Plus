import type { MenuTypeEnum } from '@/enums/MenuTypeEnum';

/**
 * 菜单树形结构类型
 */
export interface MenuTreeOption {
  id: string | number;
  label: string;
  parentId: string | number;
  weight: number;
  menuType?: MenuTypeEnum | string;
  visible?: string;
  status?: string;
  disabled?: boolean;
  children?: MenuTreeOption[];
}

export interface RoleMenuTree {
  menus: MenuTreeOption[];
  checkedKeys: Array<string | number>;
}

/**
 * 角色菜单分配中的按钮节点类型
 */
export interface RoleMenuButtonOption {
  menuId: string | number;
  menuName: string;
  parentId: string | number;
  perms?: string;
  status?: string;
  disabled?: boolean;
}

/**
 * 菜单查询参数类型
 */
export interface MenuQuery {
  keywords?: string;
  menuName?: string;
  status?: string;
}

/**
 * 菜单视图对象类型
 */
export interface MenuVO extends BaseEntity {
  parentName: string;
  parentId: string | number;
  children: MenuVO[];
  menuId: string | number;
  menuName: string;
  orderNum: number;
  path: string;
  component: string;
  queryParam: string;
  isFrame: string;
  isCache: string;
  menuType: MenuTypeEnum;
  visible: string;
  status: string;
  icon: string;
  activeMenu: string;
  ext: string;
  remark: string;
}

export interface MenuForm {
  parentName?: string;
  parentId?: string | number;
  children?: MenuForm[];
  menuId?: string | number;
  menuName: string;
  orderNum: number;
  path: string;
  component?: string;
  queryParam?: string;
  isFrame?: string;
  isCache?: string;
  menuType?: MenuTypeEnum;
  visible?: string;
  status?: string;
  icon?: string;
  activeMenu?: string;
  ext?: string;
  remark?: string;
  query?: string;
  perms?: string;
}
