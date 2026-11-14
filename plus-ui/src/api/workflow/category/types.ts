export interface CategoryTreeVO {
  id: number | string;
  label: string;
  parentId: number | string;
  weight: number;
  children: CategoryTreeVO[];
}
export interface CategoryVO {
  /**
   * 流程分类ID
   */
  categoryId: string | number;

  /**
   * 父级id
   */
  parentId: string | number;

  /**
   * 流程分类名称
   */
  categoryName: string;

  /**
   * 显示顺序
   */
  orderNum: number;

  /**
   * 创建时间
   */
  createTime: string;

  /**
   * 子对象
   */
  children: CategoryVO[];
}

export interface CategoryForm extends BaseEntity {
  /**
   * 流程分类ID
   */
  categoryId?: string | number;

  /**
   * 流程分类名称
   */
  categoryName?: string;

  /**
   * 父流程分类id
   */
  parentId?: string | number;

  /**
   * 显示顺序
   */
  orderNum?: number;
}

export interface CategoryQuery {
  /**
   * 流程分类名称
   */
  categoryName?: string;
}
