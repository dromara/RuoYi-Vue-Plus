export const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'

export const TOKEN_KEY = 'stars-web-token'

export const CATEGORY_OPTIONS = [
  'AI/RAG',
  '后端框架',
  '前端组件',
  'DevOps',
  '数据库',
  '工具库',
  '学习参考',
  '待评估',
] as const

export const SUMMARY_STATUS_LABEL: Record<string, string> = {
  pending: '待处理',
  processing: '生成中',
  done: '已完成',
  failed: '失败',
}

export const IMPORT_STATUS_LABEL: Record<string, string> = {
  pending: '等待中',
  running: '进行中',
  done: '已完成',
  failed: '失败',
  partial: '部分完成',
}
