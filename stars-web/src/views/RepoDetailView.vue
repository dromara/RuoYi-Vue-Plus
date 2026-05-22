<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { getRepoDetail, listTags, regenerateSummary, updateRepo } from '@/api/stars'
import { CATEGORY_OPTIONS, SUMMARY_STATUS_LABEL } from '@/constants'
import type { StarsRepoDetail, StarsTag } from '@/types/stars'
import { isEnrichmentPending, normalizeStatus } from '@/utils/status'
import { useBreakpoint } from '@/composables/useBreakpoint'

const route = useRoute()
const router = useRouter()
const { isMobile } = useBreakpoint()

const repoId = computed(() => {
  const raw = route.params.id
  return Array.isArray(raw) ? raw[0] : String(raw)
})
const loading = ref(false)
const saving = ref(false)
const regenerating = ref(false)
const detail = ref<StarsRepoDetail | null>(null)
const tags = ref<StarsTag[]>([])

const form = reactive({
  summaryOneLiner: '',
  summaryText: '',
  category: '',
  tagIds: [] as string[],
  note: '',
})

function statusLabel(status?: string | null) {
  return SUMMARY_STATUS_LABEL[normalizeStatus(status)] ?? status ?? '未知'
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getRepoDetail(repoId.value)
    detail.value = data
    form.summaryOneLiner = data.summaryOneLiner ?? ''
    form.summaryText = data.summaryText ?? ''
    form.category = data.category ?? ''
    form.tagIds = [...(data.tagIds ?? [])]
    form.note = data.note ?? ''
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateRepo(repoId.value, {
      summaryOneLiner: form.summaryOneLiner,
      summaryText: form.summaryText,
      category: form.category || null,
      tagIds: form.tagIds,
      note: form.note,
    })
    ElMessage.success('保存成功')
    await loadDetail()
  } finally {
    saving.value = false
  }
}

async function handleRegenerate() {
  regenerating.value = true
  try {
    await regenerateSummary(repoId.value)
    ElMessage.success('已提交重新生成请求')
    await loadDetail()
  } finally {
    regenerating.value = false
  }
}

function openExternal(url?: string | null) {
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
}

function goBack() {
  router.push('/repos')
}

onMounted(async () => {
  tags.value = await listTags()
  await loadDetail()
})
</script>

<template>
  <div class="repo-detail-view">
    <div class="page-card">
      <div class="detail-header">
        <el-button v-if="isMobile" text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="title-block">
          <h2>{{ detail?.fullName ?? '仓库详情' }}</h2>
          <div v-if="detail" class="meta-row">
            <el-tag size="small">
              <el-icon v-if="isEnrichmentPending(detail.summaryStatus)" class="spin">
                <Loading />
              </el-icon>
              {{ statusLabel(detail.summaryStatus) }}
            </el-tag>
            <span v-if="detail.language">{{ detail.language }}</span>
            <span v-if="detail.stargazersCount != null">★ {{ detail.stargazersCount }}</span>
          </div>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="8" animated />

      <template v-else-if="detail">
        <div class="external-links detail-links">
          <el-button v-if="detail.githubUrl" @click="openExternal(detail.githubUrl)">GitHub</el-button>
          <el-button v-if="detail.zreadUrl" @click="openExternal(detail.zreadUrl)">Zread</el-button>
          <el-button v-if="detail.deepwikiUrl" @click="openExternal(detail.deepwikiUrl)">
            DeepWiki
          </el-button>
        </div>

        <p v-if="detail.description" class="description">{{ detail.description }}</p>

        <el-form label-position="top" class="detail-form">
          <el-form-item label="一句话概述">
            <el-input v-model="form.summaryOneLiner" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="详细概述">
            <el-input v-model="form.summaryText" type="textarea" :rows="4" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.category" clearable placeholder="选择分类" style="width: 100%">
              <el-option v-for="item in CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select
              v-model="form.tagIds"
              multiple
              filterable
              placeholder="选择标签"
              style="width: 100%"
            >
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="个人备注">
            <el-input v-model="form.note" type="textarea" :rows="3" placeholder="记录使用心得..." />
          </el-form-item>
        </el-form>

        <div class="actions">
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button :loading="regenerating" @click="handleRegenerate">重新生成概述</el-button>
        </div>

        <el-collapse v-if="detail.readmeSnippet" class="readme-collapse">
          <el-collapse-item title="README 摘要" name="readme">
            <pre class="readme-snippet">{{ detail.readmeSnippet }}</pre>
          </el-collapse-item>
        </el-collapse>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.detail-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.title-block h2 {
  margin: 0 0 8px;
  word-break: break-all;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  color: #6b7280;
  font-size: 13px;
}

.detail-links {
  margin-bottom: 16px;
}

.description {
  margin: 0 0 16px;
  color: #374151;
  line-height: 1.6;
}

.detail-form {
  margin-top: 8px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.readme-collapse {
  margin-top: 20px;
}

.readme-snippet {
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 8px;
  background: #f9fafb;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.5;
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
