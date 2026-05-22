export type SummaryStatus = 'pending' | 'processing' | 'done' | 'failed'

export type ImportJobStatus = 'pending' | 'running' | 'done' | 'failed' | 'partial'

export type ImportJobType = 'self_sync' | 'import_user'

/** 雪花 ID，后端 JSON 可能为 string，禁止 Number() 转换 */
export type SnowflakeId = string

export interface StarsRepoCard {
  id: SnowflakeId
  fullName: string
  owner: string
  repoName: string
  language: string | null
  stargazersCount: number | null
  category: string | null
  tags: string[]
  summaryOneLiner: string | null
  summaryText: string | null
  summaryStatus: string | null
  note: string | null
  importSource: string | null
  githubUrl: string | null
  zreadUrl: string | null
  deepwikiUrl: string | null
}

export interface StarsRepoDetail extends StarsRepoCard {
  repoId: SnowflakeId
  description: string | null
  readmeSnippet: string | null
  classificationSource: string | null
  summarySource: string | null
  importTime: string | null
  updateTime: string | null
  tagIds: number[]
}

export interface RepoQuery {
  keyword?: string
  category?: string
  tagIds?: string
  importSource?: string
  summaryStatus?: string
  orderBy?: string
  pageNum?: number
  pageSize?: number
}

export interface UpdateStarsRepoPayload {
  note?: string | null
  summaryOneLiner?: string | null
  summaryText?: string | null
  category?: string | null
  tagIds?: SnowflakeId[]
}

export interface StarsTag {
  id: SnowflakeId
  name: string
  color: string | null
  createTime: string | null
}

export interface CreateTagPayload {
  name: string
  color?: string | null
}

export interface UpdateTagPayload {
  id: SnowflakeId
  name: string
  color?: string | null
}

export interface GithubStatus {
  bound: boolean
  githubLogin: string | null
}

export interface BindGithubPayload {
  token: string
}

export interface StarsImportJob {
  id: SnowflakeId
  jobType: string | null
  sourceLogin: string | null
  importLimit: number | null
  status: string | null
  totalCount: number | null
  processedCount: number | null
  failedCount: number | null
  startTime: string | null
  endTime: string | null
}

export interface BatchTagsPayload {
  userRepoIds: SnowflakeId[]
  tagIds: SnowflakeId[]
}
