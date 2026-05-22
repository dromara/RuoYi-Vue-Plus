<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { listRepos, listTags } from '@/api/stars'
import { CATEGORY_OPTIONS, SUMMARY_STATUS_LABEL } from '@/constants'
import type { StarsRepoCard, StarsTag } from '@/types/stars'
import { isEnrichmentPending, normalizeStatus } from '@/utils/status'
import { useBreakpoint } from '@/composables/useBreakpoint'

const router = useRouter()
const { isMobile } = useBreakpoint()

const loading = ref(false)
const repos = ref<StarsRepoCard[]>([])
const tags = ref<StarsTag[]>([])
const total = ref(0)
const filtersVisible = ref(false)

const query = reactive({
  keyword: '',
  category: '',
  tagIds: [] as string[],
  pageNum: 1,
  pageSize: 12,
})

function statusLabel(status?: string | null) {
  return SUMMARY_STATUS_LABEL[normalizeStatus(status)] ?? status ?? '未知'
}

function statusType(status?: string | null) {
  const normalized = normalizeStatus(status)
  if (normalized === 'done') return 'success'
  if (normalized === 'failed') return 'danger'
  if (normalized === 'processing') return 'warning'
  return 'info'
}

async function fetchTags() {
  tags.value = await listTags()
}

async function fetchRepos() {
  loading.value = true
  try {
    const result = await listRepos({
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      tagIds: query.tagIds.length ? query.tagIds.join(',') : undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    repos.value = result.rows
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchRepos()
}

function openDetail(id: string | number) {
  router.push(`/repos/${String(id)}`)
}

function openExternal(url?: string | null) {
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
}

watch(
  () => [query.pageNum, query.pageSize],
  () => fetchRepos(),
)

onMounted(async () => {
  await Promise.all([fetchTags(), fetchRepos()])
})
</script>

<template>
  <div class="repo-list-view">
    <div class="page-card">
      <div class="page-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索仓库名、概述、分类..."
          clearable
          style="min-width: 220px; flex: 1"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button v-if="isMobile" @click="filtersVisible = !filtersVisible">
          {{ filtersVisible ? '收起筛选' : '展开筛选' }}
        </el-button>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>

      <el-collapse-transition>
        <div v-show="!isMobile || filtersVisible" class="filter-row">
          <el-select
            v-model="query.category"
            clearable
            placeholder="分类"
            style="width: 180px"
            @change="handleSearch"
          >
            <el-option v-for="item in CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select
            v-model="query.tagIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            placeholder="标签"
            style="min-width: 220px"
            @change="handleSearch"
          >
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </div>
      </el-collapse-transition>

      <el-skeleton v-if="loading && !repos.length" :rows="6" animated />

      <el-empty v-else-if="!repos.length" description="暂无仓库，请前往导入中心同步 Stars" />

      <div v-else class="repo-grid">
        <el-card
          v-for="repo in repos"
          :key="repo.id"
          shadow="hover"
          class="repo-card"
          @click="openDetail(repo.id)"
        >
          <div class="repo-card-header">
            <div>
              <div class="repo-name">{{ repo.fullName }}</div>
              <div class="repo-meta">
                <span v-if="repo.language">{{ repo.language }}</span>
                <span v-if="repo.stargazersCount != null">★ {{ repo.stargazersCount }}</span>
              </div>
            </div>
            <el-tag :type="statusType(repo.summaryStatus)" size="small">
              <el-icon v-if="isEnrichmentPending(repo.summaryStatus)" class="spin">
                <Loading />
              </el-icon>
              {{ statusLabel(repo.summaryStatus) }}
            </el-tag>
          </div>

          <p class="repo-summary">
            {{ repo.summaryOneLiner || repo.summaryText || '暂无概述，AI 正在生成或等待处理...' }}
          </p>

          <div class="repo-tags">
            <el-tag v-if="repo.category" size="small" effect="plain">{{ repo.category }}</el-tag>
            <el-tag v-for="tag in repo.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
          </div>

          <div class="external-links" @click.stop>
            <el-button
              v-if="repo.githubUrl"
              size="small"
              link
              type="primary"
              @click="openExternal(repo.githubUrl)"
            >
              GitHub
            </el-button>
            <el-button
              v-if="repo.zreadUrl"
              size="small"
              link
              type="primary"
              @click="openExternal(repo.zreadUrl)"
            >
              Zread
            </el-button>
            <el-button
              v-if="repo.deepwikiUrl"
              size="small"
              link
              type="primary"
              @click="openExternal(repo.deepwikiUrl)"
            >
              DeepWiki
            </el-button>
          </div>
        </el-card>
      </div>

      <div v-if="total > query.pageSize" class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-sizes="[12, 24, 48]"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.repo-card {
  cursor: pointer;
}

.repo-card-header {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.repo-name {
  font-size: 16px;
  font-weight: 600;
  word-break: break-all;
}

.repo-meta {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
}

.repo-summary {
  min-height: 44px;
  margin: 12px 0;
  color: #374151;
  font-size: 14px;
  line-height: 1.5;
}

.repo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.spin {
  margin-right: 4px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
