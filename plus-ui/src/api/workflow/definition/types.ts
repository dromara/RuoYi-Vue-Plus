export interface FlowDefinitionQuery extends PageQuery {
  flowCode?: string;
  flowName?: string;
  category: string | number;
  isPublish?: number;
}

export interface FlowDefinitionVo {
  id: string;
  flowName: string;
  flowCode: string;
  formPath: string;
  version: string;
  isPublish: number;
  activityStatus: number;
  createTime: Date;
  updateTime: Date;
}

export interface FlowDefinitionForm {
  id: string;
  flowName: string;
  flowCode: string;
  category: string;
  ext: string;
  formPath: string;
  formCustom: string;
  modelValue: string;
}

export interface definitionXmlVO {
  xml: string[];
  xmlStr: string;
}
