import { useIntervalFn } from '@vueuse/core';
import { ElNotification } from 'element-plus';
import type { MessageVO } from '@/api/system/message/types';
import { getMessageBox } from '@/api/system/message';
import { useNoticeStore } from '@/store/modules/notice';
import { useUserStore } from '@/store/modules/user';
import { getToken } from '@/utils/auth';
import { isMessageRead } from '@/utils/message-read';
import { parsePushMessage, resolveNoticeGroup, resolveNoticeTitle, shouldAppendNotice } from '@/utils/push-message';

let closePushConnection: (() => void) | undefined;
let stopPushWatchers: Array<() => void> = [];
const KICKED_MESSAGE = 'kicked';
let pushKicked = false;
let resumePushTimer: ReturnType<typeof setTimeout> | undefined;

const formatNoticeTime = (timestamp?: number | string) => {
  const time = timestamp ? new Date(timestamp) : new Date();
  return time.toLocaleString();
};

const appendNotice = (raw: string) => {
  const payload = parsePushMessage(raw);
  if (!shouldAppendNotice(payload)) {
    return;
  }
  const userId = useUserStore().userId;
  const title = resolveNoticeTitle(payload);
  useNoticeStore().addNotice({
    messageId: payload.messageId,
    title,
    category: resolveNoticeGroup(payload),
    type: payload.type,
    source: payload.source,
    message: payload.message ?? '',
    content: payload.data?.noticeContent,
    data: payload.data,
    path: payload.path,
    read: isMessageRead(userId, payload.messageId),
    timestamp: payload.timestamp ?? Date.now(),
    time: formatNoticeTime(payload.timestamp)
  });
  ElNotification({
    title,
    message: payload.message ?? '',
    type: 'success',
    duration: 3000
  });
};

const handlePushMessage = (raw: string) => {
  if (raw === KICKED_MESSAGE) {
    pushKicked = true;
    closePush();
    return;
  }
  appendNotice(raw);
};

const toNoticeItem = (item: MessageVO) => {
  const userId = useUserStore().userId;
  const timestamp = item.createTime ? new Date(item.createTime).getTime() : Date.now();
  return {
    messageId: item.messageId,
    title: item.title,
    category: resolveNoticeGroup(item),
    type: item.type,
    source: item.source,
    message: item.message ?? '',
    content: item.content,
    data: item.data ?? null,
    path: item.path,
    read: isMessageRead(userId, item.messageId),
    timestamp,
    time: formatNoticeTime(timestamp)
  };
};

const buildSseUrl = (path: string) => {
  return `${import.meta.env.VITE_APP_BASE_API}${path}?Authorization=Bearer ${getToken()}&clientid=${import.meta.env.VITE_APP_CLIENT_ID}`;
};

const buildWsUrl = (path: string) => {
  const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
  return `${protocol}${window.location.host}${buildSseUrl(path)}`;
};

const initSsePush = (url: string) => {
  const { data, error, close } = useEventSource(url, [], {
    autoReconnect: {
      retries: 5,
      delay: 5000,
      onFailed() {
        console.warn('SSE connection failed after 5 retries');
      }
    }
  });
  closePushConnection = close;

  const stopErrorWatch = watch(error, () => {
    console.warn('SSE connection error:', error.value);
    error.value = null;
  });

  const stopDataWatch = watch(data, () => {
    if (!data.value) return;
    handlePushMessage(data.value);
    data.value = null;
  });
  stopPushWatchers.push(stopErrorWatch, stopDataWatch);
};

const initWsPush = (url: string) => {
  const { close } = useWebSocket(url, {
    autoReconnect: {
      retries: 3,
      delay: 1000,
      onFailed() {
        console.warn('websocket重连失败');
      }
    },
    heartbeat: {
      message: 'ping',
      responseMessage: 'pong',
      scheduler: callback => useIntervalFn(callback, 10000, { immediate: false }),
      pongTimeout: 2000
    },
    onMessage: (_, e) => {
      if (String(e.data) === 'pong') {
        return;
      }
      handlePushMessage(String(e.data));
    }
  });
  closePushConnection = close;
};

export const initPush = () => {
  closePush();
  if (import.meta.env.VITE_APP_MESSAGE_ENABLED === 'false') {
    return;
  }
  pushKicked = false;
  const path = import.meta.env.VITE_APP_MESSAGE_PATH || '/resource/message';
  const transport = import.meta.env.VITE_APP_MESSAGE_TRANSPORT || 'sse';
  if (transport.toLowerCase() === 'websocket') {
    initWsPush(buildWsUrl(path));
    return;
  }
  initSsePush(buildSseUrl(path));
};

export const initMessageBox = async () => {
  if (import.meta.env.VITE_APP_MESSAGE_ENABLED === 'false') {
    useNoticeStore().clearNotice();
    return;
  }
  const { data } = await getMessageBox();
  const notices = [...(data?.systemList ?? []), ...(data?.noticeList ?? []), ...(data?.workflowList ?? [])].map(
    toNoticeItem
  );
  useNoticeStore().setNotices(notices);
};

export const closePush = () => {
  closePushConnection?.();
  closePushConnection = undefined;
  stopPushWatchers.forEach(stop => stop());
  stopPushWatchers = [];
};

const resumePushIfNeeded = () => {
  if (!pushKicked || !getToken() || document.visibilityState !== 'visible') {
    return;
  }
  if (resumePushTimer) {
    clearTimeout(resumePushTimer);
  }
  resumePushTimer = setTimeout(async () => {
    resumePushTimer = undefined;
    if (!pushKicked || !getToken() || document.visibilityState !== 'visible') {
      return;
    }
    try {
      await initMessageBox();
    } finally {
      initPush();
    }
  }, 300);
};

window.addEventListener('focus', resumePushIfNeeded);
document.addEventListener('visibilitychange', resumePushIfNeeded);
window.addEventListener('online', resumePushIfNeeded);
