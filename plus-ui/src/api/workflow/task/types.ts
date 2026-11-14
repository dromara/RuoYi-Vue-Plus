export interface TaskQuery extends PageQuery {
  nodeName?: string;
  flowCode?: string;
  flowName?: string;
  createByIds?: Array<string | number>;
}

export interface ParticipantVo {
  groupIds?: string[] | number[];
  candidate: string[] | number[];
  candidateName: string[];
  claim: boolean;
}
export interface FlowTaskVO {
  id: string | number;
  createTime?: Date;
  updateTime?: Date;
  tenantId?: string;
  definitionId?: string;
  instanceId: string;
  flowName: string;
  businessId: string;
  nodeCode: string;
  nodeName: string;
  flowCode: string;
  flowStatus: string;
  formCustom: string;
  formPath: string;
  nodeType: number;
  nodeRatio: string | number;
  version?: string;
  applyNode?: boolean;
  buttonList?: ButtonList[];
  copyList?: FlowCopyVo[];
  varList?: Map<string, string>;
  businessCode: string;
  businessTitle: string;
}

export interface ButtonList {
  code: string;
  show: boolean;
}
export interface FlowCopyVo {
  userId: string | number;
  nickName: string;
}

export interface TaskOperationBo {
  //委派/转办人的用户ID（必填，准对委派/转办人操作）
  userId?: string;
  //加签/减签人的用户ID列表（必填，针对加签/减签操作）
  userIds?: string[];
  //任务ID（必填）
  taskId: string | number;
  //消息类型
  messageType?: string[];
  //意见或备注信息（可选）
  message?: string;
}
