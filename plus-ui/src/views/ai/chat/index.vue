<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { registerCurrentSnailUser } from '@/api/ai/agent';
import { getToken } from '@/utils/auth';

defineOptions({ name: 'AiChatPage' });

const chatUrl = ref('');
const loadError = ref('');
const loading = ref(false);
const baseUrl = import.meta.env.VITE_APP_BASE_API;

function buildChatUrl(openId: string, trustedCredential: string) {
  const params = new URLSearchParams({ openId, trustedCredential });
  return `${baseUrl}/snail-chat/?${params.toString()}`;
}

async function loadChat() {
  loading.value = true;
  loadError.value = '';
  try {
    const token = getToken();
    if (!token) {
      loadError.value = '登录凭证不存在，请重新登录后再试';
      return;
    }
    const { data: user } = await registerCurrentSnailUser();
    if (!user?.openId) {
      loadError.value = '获取 AI 用户身份失败';
      return;
    }
    chatUrl.value = buildChatUrl(user.openId, token);
  } catch {
    loadError.value = '加载 AI 聊天失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadChat();
});
</script>

<template>
  <div v-loading="loading" class="ai-chat-page">
    <iframe v-if="chatUrl" class="chat-frame" :src="chatUrl" title="Snail AI" allow="clipboard-read; clipboard-write" />
    <el-empty v-else class="chat-empty" :description="loadError || '正在加载 Snail AI'">
      <el-button v-if="loadError" type="primary" @click="loadChat">重新加载</el-button>
    </el-empty>
  </div>
</template>

<style scoped lang="scss">
.ai-chat-page {
  height: calc(100vh - 123px);
  min-height: 0;
  background: var(--el-bg-color-page);
  border-radius: var(--app-radius-base);
  overflow: hidden;
}

.chat-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  background: var(--app-surface-bg);
}

.chat-empty {
  height: 100%;
  background: var(--app-surface-bg);
}

@media (max-width: 768px) {
  .ai-chat-page {
    height: calc(100vh - 88px);
  }
}
</style>
