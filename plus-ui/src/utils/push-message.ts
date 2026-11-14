export const PUSH_MESSAGE_TYPE = {
  MESSAGE: 'message',
  NOTICE: 'notice',
  LLM: 'llm',
  CUSTOM: 'custom'
} as const;

export const PUSH_MESSAGE_SOURCE = {
  BACKEND: 'backend',
  NOTICE: 'notice',
  WORKFLOW: 'workflow',
  LLM: 'llm',
  CLIENT: 'client'
} as const;

export const NOTICE_GROUP = {
  SYSTEM: 'system',
  NOTICE: 'notice',
  WORKFLOW: 'workflow'
} as const;

export interface PushMessagePayload {
  messageId?: string | number;
  type?: string;
  source?: string;
  message?: string;
  data?: Record<string, any> | null;
  path?: string;
  timestamp?: number;
}

const MESSAGE_CENTER_TYPES = new Set<string>([PUSH_MESSAGE_TYPE.MESSAGE, PUSH_MESSAGE_TYPE.NOTICE]);

export const parsePushMessage = (raw: string): PushMessagePayload => {
  try {
    const payload = JSON.parse(raw) as PushMessagePayload;
    return {
      type: payload.type ?? PUSH_MESSAGE_TYPE.MESSAGE,
      source: payload.source ?? 'backend',
      messageId: payload.messageId,
      message: payload.message ?? '',
      data: payload.data ?? null,
      path: payload.path,
      timestamp: payload.timestamp ?? Date.now()
    };
  } catch {
    return {
      type: PUSH_MESSAGE_TYPE.MESSAGE,
      source: 'backend',
      messageId: undefined,
      message: raw,
      data: null,
      path: undefined,
      timestamp: Date.now()
    };
  }
};

export const shouldAppendNotice = (payload: PushMessagePayload) => {
  return MESSAGE_CENTER_TYPES.has(payload.type ?? PUSH_MESSAGE_TYPE.MESSAGE);
};

export const resolveNoticeGroup = (payload: PushMessagePayload) => {
  if (payload.type === PUSH_MESSAGE_TYPE.NOTICE || payload.source === PUSH_MESSAGE_SOURCE.NOTICE) {
    return NOTICE_GROUP.NOTICE;
  }
  if (payload.source === PUSH_MESSAGE_SOURCE.WORKFLOW) {
    return NOTICE_GROUP.WORKFLOW;
  }
  return NOTICE_GROUP.SYSTEM;
};

export const resolveNoticeTitle = (payload: PushMessagePayload) => {
  const group = resolveNoticeGroup(payload);
  if (group === NOTICE_GROUP.NOTICE) {
    return '通知公告消息';
  }
  if (group === NOTICE_GROUP.WORKFLOW) {
    return '工作流消息';
  }
  return '系统消息';
};
