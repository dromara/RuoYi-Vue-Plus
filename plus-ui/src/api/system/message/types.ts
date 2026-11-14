export interface MessageVO extends BaseEntity {
  messageId: number | string;
  category: string;
  type: string;
  source: string;
  title: string;
  message: string;
  content?: string;
  data?: Record<string, any> | null;
  path?: string;
}

export interface MessageBoxVO {
  systemList: MessageVO[];
  noticeList: MessageVO[];
  workflowList: MessageVO[];
}
