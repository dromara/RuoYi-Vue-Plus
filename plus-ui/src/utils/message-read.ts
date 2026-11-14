import cache from '@/plugins/cache';

const MESSAGE_READ_KEY = 'message_read_ids';
const MAX_READ_IDS = 300;

const buildCacheKey = (userId: string | number) => `${MESSAGE_READ_KEY}:${userId}`;

const normalizeIds = (ids: Array<string | number>) => {
  const values = ids.map(item => String(item)).filter(item => !!item);
  return Array.from(new Set(values)).slice(0, MAX_READ_IDS);
};

export const getMessageReadIds = (userId: string | number) => {
  if (!userId) {
    return [] as string[];
  }
  const ids = cache.local.getJSON(buildCacheKey(userId));
  return Array.isArray(ids) ? normalizeIds(ids) : [];
};

export const isMessageRead = (userId: string | number, messageId?: string | number) => {
  if (!userId || messageId === undefined || messageId === null) {
    return false;
  }
  return getMessageReadIds(userId).includes(String(messageId));
};

export const markMessageRead = (userId: string | number, messageId?: string | number) => {
  if (!userId || messageId === undefined || messageId === null) {
    return [];
  }
  return markMessageReadBatch(userId, [messageId]);
};

export const markMessageReadBatch = (userId: string | number, messageIds: Array<string | number>) => {
  if (!userId) {
    return [] as string[];
  }
  const current = getMessageReadIds(userId);
  const next = normalizeIds([...messageIds, ...current]);
  cache.local.setJSON(buildCacheKey(userId), next);
  return next;
};
