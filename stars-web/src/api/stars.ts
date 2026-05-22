import http, { unwrap } from '@/api/http'
import type { ApiResponse, TableData } from '@/types/api'
import type {
  BatchTagsPayload,
  BindGithubPayload,
  CreateTagPayload,
  GithubStatus,
  RepoQuery,
  SnowflakeId,
  StarsImportJob,
  StarsRepoCard,
  StarsRepoDetail,
  StarsTag,
  UpdateStarsRepoPayload,
  UpdateTagPayload,
} from '@/types/stars'

function idPath(id: SnowflakeId | number) {
  return String(id)
}

function unwrapTable<T>(response: { data: TableData<T> }): TableData<T> {
  const payload = response.data
  if (payload.code !== undefined && payload.code !== 200) {
    throw new Error(payload.msg || '请求失败')
  }
  return payload
}

// GitHub 绑定
export function getGithubStatus() {
  return http.get<ApiResponse<GithubStatus>>('/stars/github/status').then(unwrap)
}

export function bindGithub(payload: BindGithubPayload) {
  return http.post<ApiResponse<void>>('/stars/github/bind', payload).then(unwrap)
}

export function unbindGithub() {
  return http.delete<ApiResponse<void>>('/stars/github/unbind').then(unwrap)
}

// 导入任务
export function syncSelfStars(limit?: number) {
  return http
    .post<ApiResponse<number>>('/stars/import/self', limit != null ? { limit } : {})
    .then(unwrap)
}

export function importUserStars(login: string, limit?: number) {
  return http
    .post<ApiResponse<number>>(`/stars/import/user/${encodeURIComponent(login)}`, limit != null ? { limit } : {})
    .then(unwrap)
}

export function listImportJobs(params?: { pageNum?: number; pageSize?: number }) {
  return http
    .get<TableData<StarsImportJob>>('/stars/import/jobs', { params })
    .then(unwrapTable)
}

export function getImportJob(id: SnowflakeId | number) {
  return http.get<ApiResponse<StarsImportJob>>(`/stars/import/jobs/${idPath(id)}`).then(unwrap)
}

// 仓库
export function listRepos(params: RepoQuery) {
  return http.get<TableData<StarsRepoCard>>('/stars/repos', { params }).then(unwrapTable)
}

export function getRepoDetail(id: SnowflakeId | number) {
  return http.get<ApiResponse<StarsRepoDetail>>(`/stars/repos/${idPath(id)}`).then(unwrap)
}

export function updateRepo(id: SnowflakeId | number, payload: UpdateStarsRepoPayload) {
  return http.put<ApiResponse<void>>(`/stars/repos/${idPath(id)}`, payload).then(unwrap)
}

export function regenerateSummary(id: SnowflakeId | number) {
  return http.post<ApiResponse<void>>(`/stars/repos/${idPath(id)}/regenerate-summary`).then(unwrap)
}

export function batchAssignTags(payload: BatchTagsPayload) {
  return http.post<ApiResponse<void>>('/stars/repos/batch-tags', payload).then(unwrap)
}

// 标签
export function listTags() {
  return http.get<ApiResponse<StarsTag[]>>('/stars/tags').then(unwrap)
}

export function createTag(payload: CreateTagPayload) {
  return http.post<ApiResponse<StarsTag>>('/stars/tags', payload).then(unwrap)
}

export function updateTag(payload: UpdateTagPayload) {
  return http.put<ApiResponse<StarsTag>>('/stars/tags', payload).then(unwrap)
}

export function deleteTag(id: SnowflakeId | number) {
  return http.delete<ApiResponse<void>>(`/stars/tags/${idPath(id)}`).then(unwrap)
}
