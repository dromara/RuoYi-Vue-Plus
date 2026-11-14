export interface TreeVO {
  /**
   * 主键
   */
  id: string | number;

  /**
   * 父id
   */
  parentId: string | number;

  /**
   * 部门id
   */
  deptId: string | number;

  /**
   * 用户id
   */
  userId: string | number;

  /**
   * 值
   */
  treeName: string;

  /**
   * 子对象
   */
  children: TreeVO[];
}

export interface TreeForm extends BaseEntity {
  /**
   * 主键
   */
  id?: string | number;

  /**
   * 父id
   */
  parentId?: string | number;

  /**
   * 部门id
   */
  deptId?: string | number;

  /**
   * 用户id
   */
  userId?: string | number;

  /**
   * 值
   */
  treeName?: string;
}

export interface TreeQuery {
  /**
   * 父id
   */
  parentId?: string | number;

  /**
   * 部门id
   */
  deptId?: string | number;

  /**
   * 用户id
   */
  userId?: string | number;

  /**
   * 值
   */
  treeName?: string;
}
