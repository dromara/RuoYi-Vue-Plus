import type { FlowTaskVO } from '@/api/workflow/task/types';

export interface FlowInstanceQuery extends PageQuery {
  category?: string | number;
  nodeName?: string;
  flowCode?: string;
  flowName?: string;
  createByIds?: Array<string | number>;
  businessId?: string;
}

export interface FlowInstanceVO extends BaseEntity {
  id: string | number;
  definitionId: string;
  flowName: string;
  flowCode: string;
  version: string;
  businessId: string;
  activityStatus: number;
  tenantId: string;
  createTime: string;
  createBy: string;
  flowStatus: string;
  flowStatusName: string;
  flowTaskList: FlowTaskVO[];
  businessCode: string;
  businessTitle: string;
}
