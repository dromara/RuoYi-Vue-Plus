export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

export interface TableData<T> {
  code: number
  msg: string
  rows: T[]
  total: number
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}
