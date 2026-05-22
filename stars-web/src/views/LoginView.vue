<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const captchaEnabled = ref(true)
const captchaImg = ref('')
const form = reactive({
  username: 'admin',
  password: 'admin123',
  code: '',
  uuid: '',
})

async function loadCaptcha() {
  try {
    const data = await getCaptcha()
    captchaEnabled.value = data.captchaEnabled !== false
    form.uuid = data.uuid ?? ''
    captchaImg.value = data.img ? `data:image/png;base64,${data.img}` : ''
    form.code = ''
  } catch {
    captchaEnabled.value = false
  }
}

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (captchaEnabled.value && !form.code) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    await authStore.login(
      form.username,
      form.password,
      captchaEnabled.value ? form.code : undefined,
      captchaEnabled.value ? form.uuid : undefined,
    )
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/repos'
    router.replace(redirect)
  } catch {
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">★</div>
        <div>
          <h1>Stars 知识库</h1>
          <p>登录 RuoYi 账号以管理 GitHub Stars</p>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="请输入计算结果" />
            <img
              v-if="captchaImg"
              class="captcha-img"
              :src="captchaImg"
              alt="验证码"
              title="点击刷新"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #1e3a8a 0%, #111827 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 32px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.25);
}

.login-header {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;

  h1 {
    margin: 0 0 8px;
    font-size: 24px;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
  }
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: #fef3c7;
  color: #d97706;
  font-size: 28px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
