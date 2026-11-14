import { defineStore } from 'pinia';
import { computed, reactive } from 'vue';

export interface NoticeItem {
  messageId?: string | number;
  title?: string;
  category?: string;
  type?: string;
  source?: string;
  read: boolean;
  message: string;
  content?: string;
  data?: Record<string, any> | null;
  path?: string;
  timestamp?: number;
  time: string;
}

export const useNoticeStore = defineStore('notice', () => {
  const state = reactive({
    notices: [] as NoticeItem[]
  });

  const unreadCount = computed(() => state.notices.filter(item => !item.read).length);

  const buildNoticeKey = (notice: NoticeItem) => {
    if (notice.messageId !== undefined && notice.messageId !== null) {
      return String(notice.messageId);
    }
    return [notice.type, notice.source, notice.timestamp, notice.message].join(':');
  };

  const sortNotices = () => {
    state.notices.sort((a, b) => Number(b.timestamp || 0) - Number(a.timestamp || 0));
  };

  const setNotices = (notices: NoticeItem[]) => {
    state.notices = [...notices];
    sortNotices();
  };

  const addNotice = (notice: NoticeItem) => {
    const key = buildNoticeKey(notice);
    const index = state.notices.findIndex(item => buildNoticeKey(item) === key);
    if (index > -1) {
      state.notices[index] = {
        ...state.notices[index],
        ...notice
      };
    } else {
      state.notices.unshift(notice);
    }
    sortNotices();
  };

  const markRead = (messageId?: string | number) => {
    if (messageId === undefined || messageId === null) {
      return;
    }
    const target = state.notices.find(item => String(item.messageId) === String(messageId));
    if (target) {
      target.read = true;
    }
  };

  const markReadBatch = (messageIds: Array<string | number>) => {
    const idSet = new Set(messageIds.map(item => String(item)));
    state.notices.forEach(item => {
      if (item.messageId !== undefined && item.messageId !== null && idSet.has(String(item.messageId))) {
        item.read = true;
      }
    });
  };

  const readAll = () => {
    markReadBatch(
      state.notices
        .map(item => item.messageId)
        .filter((item): item is string | number => item !== undefined && item !== null)
    );
  };

  const clearNotice = () => {
    state.notices = [];
  };
  return {
    state,
    unreadCount,
    setNotices,
    addNotice,
    markRead,
    markReadBatch,
    readAll,
    clearNotice
  };
});
