export interface OperLogQuery extends PageQuery {
  operIp: string;
  title: string;
  operName: string;
  userId: string;
  deptId: string;
  clientKey: string;
  deviceType: string;
  browser: string;
  os: string;
  businessType: string;
  status: string;
  orderByColumn: string;
  isAsc: string;
}

export interface OperLogVO extends BaseEntity {
  operId: string | number;
  tenantId: string;
  title: string;
  businessType: number;
  businessTypes: number[] | undefined;
  method: string;
  requestMethod: string;
  operatorType: number;
  operName: string;
  userId: string | number;
  deptId: string | number;
  deptName: string;
  clientKey: string;
  deviceType: string;
  browser: string;
  os: string;
  operUrl: string;
  operIp: string;
  operLocation: string;
  operParam: string;
  jsonResult: string;
  status: number;
  errorMsg: string;
  operTime: string;
  costTime: number;
}

export interface OperLogForm {
  operId: number | string | undefined;
  tenantId: string | number | undefined;
  title: string;
  businessType: number;
  businessTypes: number[] | undefined;
  method: string;
  requestMethod: string;
  operatorType: number;
  operName: string;
  userId: string | number | undefined;
  deptId: string | number | undefined;
  deptName: string;
  clientKey: string;
  deviceType: string;
  browser: string;
  os: string;
  operUrl: string;
  operIp: string;
  operLocation: string;
  operParam: string;
  jsonResult: string;
  status: number;
  errorMsg: string;
  operTime: string;
  costTime: number;
}
