export interface SpelVO {
  /**
   * 主键id
   */
  id: string | number;

  /**
   * 组件名称
   */
  componentName: string;

  /**
   * 方法名
   */
  methodName: string;

  /**
   * 参数
   */
  methodParams: string;

  /**
   * 预览spel值
   */
  viewSpel: string;

  /**
   * 状态（0正常 1停用）
   */
  status: string;

  /**
   * 备注
   */
  remark?: string;
}

export interface SpelForm extends BaseEntity {
  /**
   * 主键id
   */
  id?: string | number;

  /**
   * 组件名称
   */
  componentName?: string;

  /**
   * 方法名
   */
  methodName?: string;

  /**
   * 参数
   */
  methodParams?: string;

  /**
   * 预览spel值
   */
  viewSpel?: string;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 备注
   */
  remark?: string;
}

export interface SpelQuery extends PageQuery {
  /**
   * 组件名称
   */
  componentName?: string;

  /**
   * 方法名
   */
  methodName?: string;

  /**
   * 参数
   */
  methodParams?: string;

  /**
   * 预览spel值
   */
  viewSpel?: string;

  /**
   * 状态（0正常 1停用）
   */
  status?: string;

  /**
   * 日期范围参数
   */
  params?: any;
}
