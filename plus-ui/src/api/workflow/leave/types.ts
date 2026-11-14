export interface LeaveVO {
  id: string | number;
  applyCode?: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  leaveDays: number;
  remark: string;
  status?: string;
  /** 已有流程实例时回显的流程定义编码 */
  flowCode?: string;
}

export interface LeaveForm extends BaseEntity {
  id?: string | number;
  applyCode?: string;
  leaveType?: string;
  startDate?: string;
  endDate?: string;
  leaveDays?: number;
  remark?: string;
  status?: string;
}

export interface LeaveQuery extends PageQuery {
  startLeaveDays?: number;
  endLeaveDays?: number;
}
