<template>
  <div class="login">
    <div class="login-shell">
      <section class="login-brand">
        <span class="brand-pill">Plus UI Workspace</span>
        <h1 class="brand-title">企业级后台管理系统</h1>
        <p class="brand-desc">
          真正面向企业级的应用框架 组件化 模块化 轻耦合 高扩展 针对企业痛点 业界一流技术栈
          <br />
          重写 RuoYi-Vue 所有功能 集成 Sa-Token、Mybatis-Plus、WarmFlow、SpringDoc、Hutool、OSS 定期同步。
        </p>
        <div class="brand-highlights">
          <span v-for="item in highlights" :key="item" class="highlight-chip">{{ item }}</span>
        </div>
        <div class="brand-metrics">
          <article v-for="item in quickStats" :key="item.label" class="metric-card">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </article>
        </div>
      </section>

      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="title-box">
          <div>
            <p class="eyebrow">Workspace Sign In</p>
            <h3 class="title">{{ title }}</h3>
            <p class="subtitle">使用当前账号体系登录到业务工作台。</p>
          </div>
          <lang-select />
        </div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            size="large"
            auto-complete="off"
            :placeholder="$t('login.username')"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            auto-complete="off"
            :placeholder="$t('login.password')"
            @keyup.enter="handleLogin"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="captchaEnabled" prop="code" class="captcha-row">
          <el-input
            v-model="loginForm.code"
            size="large"
            auto-complete="off"
            :placeholder="$t('login.code')"
            @keyup.enter="handleLogin"
          >
            <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" class="login-code-img" @click="getCode" />
          </div>
        </el-form-item>

        <div class="form-meta">
          <el-checkbox v-model="loginForm.rememberMe">{{ $t('login.rememberPassword') }}</el-checkbox>
          <router-link v-if="register" class="link-type" :to="'/register'">
            {{ $t('login.switchRegisterPage') }}
          </router-link>
        </div>

        <div class="social-panel">
          <span class="social-label">第三方登录</span>
          <div class="social-actions">
            <el-button circle :title="$t('login.social.wechat')" @click="doSocialLogin('wechat')">
              <svg-icon icon-class="wechat" />
            </el-button>
            <el-button circle :title="$t('login.social.maxkey')" @click="doSocialLogin('maxkey')">
              <svg-icon icon-class="maxkey" />
            </el-button>
            <el-button circle :title="$t('login.social.topiam')" @click="doSocialLogin('topiam')">
              <svg-icon icon-class="topiam" />
            </el-button>
            <el-button circle :title="$t('login.social.gitee')" @click="doSocialLogin('gitee')">
              <svg-icon icon-class="gitee" />
            </el-button>
            <el-button circle :title="$t('login.social.github')" @click="doSocialLogin('github')">
              <svg-icon icon-class="github" />
            </el-button>
          </div>
        </div>

        <el-form-item class="submit-row">
          <el-button :loading="loading" size="large" type="primary" class="submit-button" @click.prevent="handleLogin">
            <span v-if="!loading">{{ $t('login.login') }}</span>
            <span v-else>{{ $t('login.logging') }}</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="el-login-footer">
      <span>Copyright © 2018-{{ currentYear }} 疯狂的狮子Li All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { to } from 'await-to-js';
import { useI18n } from 'vue-i18n';
import { getCodeImg } from '@/api/login';
import { authRouterUrl } from '@/api/system/social/auth';
import { LoginData } from '@/api/types';
import { HttpStatus } from '@/enums/RespEnum';
import { useUserStore } from '@/store/modules/user';

const title = import.meta.env.VITE_APP_TITLE;
const currentYear = new Date().getFullYear();
const quickStats = [
  { label: '细粒度权限管理', value: '动态权限控制' },
  { label: '主流技术栈', value: '全栈技术集成' },
  { label: 'UI样式', value: '卡片式' }
];
const highlights = ['技术栈全面升级', '动态菜单', '多主题布局', '深浅色主题'];
const userStore = useUserStore();
const router = useRouter();
const { t } = useI18n();

const loginForm = ref<LoginData>({
  username: 'admin',
  password: 'admin123',
  rememberMe: false,
  code: '',
  uuid: ''
} as LoginData);

const loginRules: ElFormRules = {
  username: [
    {
      required: true,
      trigger: 'blur',
      message: t('login.rule.username.required')
    }
  ],
  password: [
    {
      required: true,
      trigger: 'blur',
      message: t('login.rule.password.required')
    }
  ],
  code: [
    {
      required: true,
      trigger: 'change',
      message: t('login.rule.code.required')
    }
  ]
};

const codeUrl = ref('');
const loading = ref(false);
const captchaEnabled = ref(true);
const register = ref(false);
const redirect = ref('/');
const loginRef = ref<ElFormInstance>();

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    redirect.value = newRoute.query && newRoute.query.redirect && decodeURIComponent(newRoute.query.redirect);
  },
  { immediate: true }
);

const handleLogin = () => {
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      if (loginForm.value.rememberMe) {
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        localStorage.removeItem('username');
        localStorage.removeItem('rememberMe');
      }
      localStorage.removeItem('password');
      const [err] = await to(userStore.login(loginForm.value));
      if (!err) {
        const redirectUrl = redirect.value || '/';
        await router.push(redirectUrl);
        loading.value = false;
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          await getCode();
        }
      }
    } else {
      console.log('error submit!', fields);
    }
  });
};

const getCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  captchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (captchaEnabled.value) {
    loginForm.value.code = '';
    codeUrl.value = 'data:image/gif;base64,' + data.img;
    loginForm.value.uuid = data.uuid;
  }
};

const getLoginData = () => {
  const username = localStorage.getItem('username');
  const rememberMe = localStorage.getItem('rememberMe');
  localStorage.removeItem('password');
  loginForm.value = {
    username: username === null ? String(loginForm.value.username) : username,
    password: username === null ? String(loginForm.value.password) : '',
    rememberMe: rememberMe === 'true'
  } as LoginData;
};

const doSocialLogin = (type: string) => {
  authRouterUrl(type).then((res: any) => {
    if (res.code === HttpStatus.SUCCESS) {
      window.location.href = res.data;
    } else {
      ElMessage.error(res.msg);
    }
  });
};

onMounted(() => {
  getCode();
  getLoginData();
});
</script>

<style lang="scss" scoped>
.login {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px 88px;
  background:
    radial-gradient(circle at 12% 12%, rgba(53, 109, 255, 0.22), transparent 24%),
    radial-gradient(circle at 88% 18%, rgba(14, 165, 233, 0.18), transparent 24%),
    linear-gradient(135deg, #071120 0%, #0f1b33 42%, #15345f 100%);
}

.login-shell {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 440px);
  gap: 26px;
  align-items: stretch;
}

.login-brand,
.login-form {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 30px 80px rgba(2, 8, 23, 0.32);
  backdrop-filter: blur(18px);
}

.login-brand {
  padding: 42px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #eef4ff;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02)),
    linear-gradient(135deg, rgba(53, 109, 255, 0.32), rgba(15, 23, 42, 0.24));
}

.brand-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.brand-title {
  margin: 22px 0 14px;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.08;
  letter-spacing: -0.03em;
}

.brand-desc {
  margin: 0;
  max-width: 580px;
  color: rgba(226, 232, 240, 0.88);
  font-size: 15px;
  line-height: 1.8;
}

.brand-highlights {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 28px 0 34px;
}

.highlight-chip {
  padding: 9px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #f8fbff;
  font-size: 13px;
}

.brand-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  padding: 18px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  gap: 6px;

  strong {
    font-size: 24px;
    color: #fff;
  }

  span {
    color: rgba(226, 232, 240, 0.72);
    font-size: 13px;
  }
}

.login-form {
  width: 100%;
  padding: 34px 30px 26px;
  z-index: 1;
  background: var(--app-surface-bg);
}

.title-box {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 28px;

  .eyebrow {
    margin: 0 0 8px;
    color: var(--app-accent-strong);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .title {
    margin: 0;
    color: var(--app-text-title);
    font-weight: 700;
    font-size: 30px;
    letter-spacing: -0.03em;
  }

  .subtitle {
    margin: 8px 0 0;
    color: var(--app-text-muted);
    font-size: 14px;
    line-height: 1.7;
  }

  :deep(.lang-select--style) {
    line-height: 0;
    color: var(--app-text-muted);
    padding: 10px;
    border-radius: 14px;
    background: var(--app-elevated-soft-bg);
    border: 1px solid var(--app-surface-border);
  }
}

.login-form .el-input {
  height: 48px;
}

.login-form .input-icon {
  height: 46px;
  width: 14px;
  margin-left: 0;
}

.captcha-row {
  :deep(.el-form-item__content) {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 122px;
    gap: 12px;
  }
}

.form-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -2px 0 18px;
}

.social-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 22px;
  border-radius: 20px;
  background: var(--app-elevated-soft-bg);
  border: 1px solid var(--app-surface-border);
}

.social-label {
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.social-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.submit-row {
  margin-bottom: 0;
}

.submit-button {
  width: 100%;
  height: 50px;
  border-radius: 16px;
  box-shadow: 0 18px 34px rgba(53, 109, 255, 0.22);
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  background-color: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 0 0 1px var(--app-surface-border) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(53, 109, 255, 0.24) inset,
    0 0 0 4px rgba(53, 109, 255, 0.12);
}

.login-form :deep(.el-checkbox__label) {
  color: var(--app-text-muted);
}

.login-form :deep(.el-button.is-circle) {
  background: var(--app-elevated-soft-bg);
  border: 1px solid var(--app-surface-border);
  color: var(--app-text-muted);
}

.login-form :deep(.el-button.is-circle:hover) {
  background: rgba(53, 109, 255, 0.12);
  border-color: rgba(53, 109, 255, 0.2);
  color: var(--app-accent-strong);
}

.login-code {
  height: 48px;
  box-sizing: border-box;
  border-radius: 16px;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--app-surface-border);

  img {
    cursor: pointer;
    vertical-align: middle;
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(226, 232, 240, 0.68);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.login-code-img {
  height: 48px;
  padding-left: 0;
}

@media (max-width: 960px) {
  .login {
    padding: 24px 14px 80px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-brand {
    padding: 28px 24px;
  }

  .brand-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-brand {
    display: none;
  }

  .login-form {
    padding: 26px 18px 20px;
  }

  .title-box {
    flex-direction: column;
  }

  .social-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .social-actions {
    justify-content: flex-start;
  }

  .captcha-row :deep(.el-form-item__content) {
    grid-template-columns: 1fr;
  }
}
</style>
