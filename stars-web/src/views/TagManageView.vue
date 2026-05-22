<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTag, deleteTag, listTags, updateTag } from '@/api/stars'
import type { SnowflakeId, StarsTag } from '@/types/stars'
import { useBreakpoint } from '@/composables/useBreakpoint'

const { isMobile } = useBreakpoint()

const loading = ref(false)
const tags = ref<StarsTag[]>([])
const dialogVisible = ref(false)
const editingId = ref<SnowflakeId | null>(null)

const form = reactive({
  name: '',
  color: '#409EFF',
})

function resetForm() {
  form.name = ''
  form.color = '#409EFF'
  editingId.value = null
}

async function fetchTags() {
  loading.value = true
  try {
    tags.value = await listTags()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(tag: StarsTag) {
  editingId.value = tag.id
  form.name = tag.name
  form.color = tag.color || '#409EFF'
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }

  if (editingId.value) {
    await updateTag({
      id: editingId.value,
      name: form.name.trim(),
      color: form.color,
    })
    ElMessage.success('标签已更新')
  } else {
    await createTag({
      name: form.name.trim(),
      color: form.color,
    })
    ElMessage.success('标签已创建')
  }

  dialogVisible.value = false
  resetForm()
  await fetchTags()
}

async function handleDelete(tag: StarsTag) {
  await ElMessageBox.confirm(`确定删除标签「${tag.name}」吗？`, '提示', { type: 'warning' })
  await deleteTag(tag.id)
  ElMessage.success('标签已删除')
  await fetchTags()
}

onMounted(fetchTags)
</script>

<template>
  <div class="tag-manage-view">
    <div class="page-card">
      <div class="page-toolbar">
        <h2>标签管理</h2>
        <el-button type="primary" @click="openCreate">新建标签</el-button>
      </div>

      <el-skeleton v-if="loading && !tags.length" :rows="5" animated />

      <template v-else>
        <el-table v-if="!isMobile" v-loading="loading" :data="tags" stripe empty-text="暂无标签">
          <el-table-column prop="name" label="名称" min-width="160">
            <template #default="{ row }">
              <el-tag :color="row.color || undefined" effect="dark">{{ row.name }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="color" label="颜色" width="120" />
          <el-table-column prop="createTime" label="创建时间" min-width="180" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="mobile-tag-list">
          <el-empty v-if="!tags.length" description="暂无标签" />
          <div v-for="tag in tags" :key="tag.id" class="mobile-tag-item">
            <el-tag :color="tag.color || undefined" effect="dark">{{ tag.name }}</el-tag>
            <div class="mobile-actions">
              <el-button size="small" type="danger" plain @click="handleDelete(tag)">删除</el-button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑标签' : '新建标签'"
      width="90%"
      :style="{ maxWidth: '420px' }"
      @closed="resetForm"
    >
      <el-form label-position="top">
        <el-form-item label="标签名称">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item v-if="!isMobile || !editingId" label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 18px;
  }
}

.mobile-tag-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mobile-tag-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}
</style>
