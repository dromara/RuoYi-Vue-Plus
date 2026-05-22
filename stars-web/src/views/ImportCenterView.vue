<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  bindGithub,
  getGithubStatus,
  getImportJob,
  importUserStars,
  listImportJobs,
  syncSelfStars,
  unbindGithub,
} from '@/api/stars'
import { IMPORT_STATUS_LABEL } from '@/constants'
import type { GithubStatus, StarsImportJob } from '@/types/stars'
import { normalizeStatus } from '@/utils/status'

const githubStatus = ref<GithubStatus>({ bound: false, githubLogin: null })
const patToken = ref('')
const importLogin = ref('')
const importLimit = ref(100)
const jobs = ref<StarsImportJob[]>([])
const loadingGithub = ref(false)
const loadingSync = ref(false)
const loadingImport = ref(false)
const activeJob = ref<StarsImportJob | null>(null)
const pollingTimers = new Map<string, ReturnType<typeof setTimeout>>()

function statusLabel(status?: string | null) {
  return IMPORT_STATUS_LABEL[normalizeStatus(status)] ?? status ?? '未知'
}

function statusType(status?: string | null) {
  const normalized = normalizeStatus(status)
  if (normalized === 'done') return 'success'
  if (normalized === 'failed') return 'danger'
  if (normalized === 'running' || normalized === 'partial') return 'warning'
  return 'info'
}

function jobProgress(job: StarsImportJob) {
  const total = job.totalCount ?? 0
  const processed = job.processedCount ?? 0
  if (!total) return 0
  return Math.min(100, Math.round((processed / total) * 100))
}

async function refreshGithubStatus() {
  githubStatus.value = await getGithubStatus()
}

async function refreshJobs() {
  const result = await listImportJobs({ pageNum: 1, pageSize: 10 })
  jobs.value = result.rows
}

async function handleBind() {
  if (!patToken.value.trim()) {
    ElMessage.warning('请输入 GitHub PAT')
    return
  }
  loadingGithub.value = true
  try {
    await bindGithub({ token: patToken.value.trim() })
    patToken.value = ''
    ElMessage.success('GitHub 绑定成功')
    await refreshGithubStatus()
  } finally {
    loadingGithub.value = false
  }
}

async function handleUnbind() {
  await ElMessageBox.confirm('确定解绑 GitHub 账号吗？', '提示', { type: 'warning' })
  await unbindGithub()
  ElMessage.success('已解绑')
  await refreshGithubStatus()
}

function stopPolling(jobId: string) {
  const timer = pollingTimers.get(jobId)
  if (timer) {
    clearTimeout(timer)
    pollingTimers.delete(jobId)
  }
}

async function pollJob(jobId: string) {
  try {
    const job = await getImportJob(jobId)
    activeJob.value = job

    const idx = jobs.value.findIndex((item) => String(item.id) === jobId)
    if (idx >= 0) {
      jobs.value[idx] = job
    } else {
      jobs.value.unshift(job)
    }

    const normalized = normalizeStatus(job.status)
    if (normalized === 'running' || normalized === 'pending') {
      const timer = setTimeout(() => pollJob(jobId), 2000)
      pollingTimers.set(jobId, timer)
    } else {
      stopPolling(jobId)
      await refreshJobs()
    }
  } catch {
    stopPolling(jobId)
  }
}

function startPolling(jobId: string | number) {
  const key = String(jobId)
  stopPolling(key)
  pollJob(key)
}

async function handleSyncSelf() {
  loadingSync.value = true
  try {
    const jobId = await syncSelfStars(importLimit.value)
    ElMessage.success('已开始同步，请查看任务进度')
    startPolling(jobId)
    await refreshJobs()
  } finally {
    loadingSync.value = false
  }
}

async function handleImportUser() {
  const login = importLogin.value.trim()
  if (!login) {
    ElMessage.warning('请输入 GitHub 用户名')
    return
  }
  loadingImport.value = true
  try {
    const jobId = await importUserStars(login, importLimit.value)
    ElMessage.success(`已开始导入 ${login} 的 Stars`)
    importLogin.value = ''
    startPolling(jobId)
    await refreshJobs()
  } finally {
    loadingImport.value = false
  }
}

onMounted(async () => {
  await Promise.all([refreshGithubStatus(), refreshJobs()])
})

onUnmounted(() => {
  pollingTimers.forEach((timer) => clearTimeout(timer))
  pollingTimers.clear()
})
</script>

<template>
  <div class="import-center-view">
    <div class="page-card section">
      <h2>GitHub 绑定</h2>
      <p class="section-desc">绑定 PAT 后可同步自己的 Stars，并提高 GitHub API 速率限制。</p>

      <el-alert
        v-if="githubStatus.bound"
        type="success"
        :closable="false"
        show-icon
        :title="`已绑定：@${githubStatus.githubLogin}`"
        class="status-alert"
      />
      <el-alert
        v-else
        type="warning"
        :closable="false"
        show-icon
        title="尚未绑定 GitHub 账号"
        class="status-alert"
      />

      <div v-if="!githubStatus.bound" class="bind-form">
        <el-input
          v-model="patToken"
          type="password"
          show-password
          placeholder="粘贴 GitHub Personal Access Token"
        />
        <el-button type="primary" :loading="loadingGithub" @click="handleBind">绑定</el-button>
      </div>
      <el-button v-else type="danger" plain @click="handleUnbind">解绑</el-button>
    </div>

    <div class="page-card section">
      <h2>导入 Stars</h2>
      <p class="section-desc">按 GitHub 最近 Star 时间倒序导入，可指定条数（默认 100）。</p>
      <div class="limit-row">
        <span class="limit-label">导入条数</span>
        <el-input-number v-model="importLimit" :min="1" :max="5000" :step="50" />
      </div>
      <div class="action-row">
        <el-button type="primary" :loading="loadingSync" @click="handleSyncSelf">
          同步我的 Stars
        </el-button>
      </div>
      <div class="action-row import-user">
        <el-input v-model="importLogin" placeholder="他人 GitHub 用户名" />
        <el-button type="success" :loading="loadingImport" @click="handleImportUser">
          导入公开 Stars
        </el-button>
      </div>
    </div>

    <div v-if="activeJob" class="page-card section">
      <h2>当前任务</h2>
      <div class="job-row">
        <span>#{{ activeJob.id }}</span>
        <el-tag :type="statusType(activeJob.status)" size="small">
          {{ statusLabel(activeJob.status) }}
        </el-tag>
      </div>
      <el-progress :percentage="jobProgress(activeJob)" :stroke-width="10" />
      <div class="job-stats">
        <span>计划 {{ activeJob.importLimit ?? activeJob.totalCount ?? 0 }}</span>
        <span>已处理 {{ activeJob.processedCount ?? 0 }}</span>
        <span>失败 {{ activeJob.failedCount ?? 0 }}</span>
      </div>
    </div>

    <div class="page-card section">
      <h2>最近任务</h2>
      <el-table :data="jobs" stripe empty-text="暂无导入任务">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="jobType" label="类型" min-width="120" />
        <el-table-column prop="sourceLogin" label="来源用户" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" min-width="160">
          <template #default="{ row }">
            {{ row.processedCount ?? 0 }} / {{ row.totalCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="160" />
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.section {
  margin-bottom: 16px;

  h2 {
    margin: 0 0 8px;
    font-size: 18px;
  }
}

.section-desc {
  margin: 0 0 16px;
  color: #6b7280;
  font-size: 14px;
}

.status-alert {
  margin-bottom: 16px;
}

.limit-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.limit-label {
  font-size: 14px;
  color: #374151;
}

.bind-form,
.action-row,
.import-user {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.import-user {
  margin-top: 12px;
}

.job-row,
.job-stats {
  display: flex;
  gap: 16px;
  align-items: center;
}

.job-stats {
  margin-top: 8px;
  color: #6b7280;
  font-size: 13px;
}
</style>
