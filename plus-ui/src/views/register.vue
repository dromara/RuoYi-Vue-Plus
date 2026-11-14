<template>
  <div class="register">
    <div class="register-shell">
      <section class="register-brand">
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

      <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
        <div class="title-box">
          <div>
            <p class="eyebrow">Workspace Register</p>
            <h3 class="title">{{ title }}</h3>
            <p class="subtitle">创建新的业务工作台账号，接入当前系统权限与登录体系。</p>
          </div>
          <lang-select />
        </div>

        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            type="text"
            size="large"
            auto-complete="off"
            :placeholder="$t('register.username')"
          >
            <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            size="large"
            auto-complete="off"
            :placeholder="$t('register.password')"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            size="large"
            auto-complete="off"
            :placeholder="$t('register.confirmPassword')"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaEnabled" prop="code" class="captcha-row">
          <el-input
            v-model="registerForm.code"
            size="large"
            auto-complete="off"
            :placeholder="$t('register.code')"
            @keyup.enter="handleRegister"
          >
            <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
          </el-input>
          <div class="register-code">
            <img :src="codeUrl" class="register-code-img" @click="getCode" />
          </div>
        </el-form-item>

        <div class="form-meta">
          <span class="register-tip">注册后将返回登录页继续完成认证</span>
          <router-link class="link-type" :to="'/login'">{{ $t('register.switchLoginPage') }}</router-link>
        </div>

        <el-form-item class="submit-row">
          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="submit-button"
            @click.prevent="handleRegister"
          >
            <span v-if="!loading">{{ $t('register.register') }}</span>
            <span v-else>{{ $t('register.registering') }}</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="el-register-footer">
      <span>Copyright © 2018-{{ currentYear }} 疯狂的狮子Li All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { to } from 'await-to-js';
import { useI18n } from 'vue-i18n';
import { getCodeImg, register } from '@/api/login';
import { RegisterForm } from '@/api/types';

const title = import.meta.env.VITE_APP_TITLE;
const currentYear = new Date().getFullYear();
const quickStats = [
  { label: '细粒度权限管理', value: '动态权限控制' },
  { label: '主流技术栈', value: '全栈技术集成' },
  { label: 'UI样式', value: '卡片式' }
];
const highlights = ['技术栈全面升级', '动态菜单', '多主题布局', '深浅色主题'];
const router = useRouter();

const { t } = useI18n();

const registerForm = ref<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
  code: '',
  uuid: '',
  userType: 'sys_user'
});

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (registerForm.value.password !== value) {
    callback(new Error(t('register.rule.confirmPassword.equalToPassword')));
  } else {
    callback();
  }
};

const registerRules: ElFormRules = {
  username: [
    {
      required: true,
      trigger: 'blur',
      message: t('register.rule.username.required')
    },
    {
      min: 2,
      max: 20,
      message: t('register.rule.username.length', { min: 2, max: 20 }),
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      trigger: 'blur',
      message: t('register.rule.password.required')
    },
    {
      min: 5,
      max: 20,
      message: t('register.rule.password.length', { min: 5, max: 20 }),
      trigger: 'blur'
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: t('register.rule.password.pattern', {
        strings: '< > " \' \\ |'
      }),
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      required: true,
      trigger: 'blur',
      message: t('register.rule.confirmPassword.required')
    },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ],
  code: [
    {
      required: true,
      trigger: 'change',
      message: t('register.rule.code.required')
    }
  ]
};
const codeUrl = ref('');
const loading = ref(false);
const captchaEnabled = ref(true);
const registerRef = ref<ElFormInstance>();

const handleRegister = () => {
  registerRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true;
      const [err] = await to(register(registerForm.value));
      if (!err) {
        const username = registerForm.value.username;
        await ElMessageBox.alert(t('register.registerSuccess', { username }), '系统提示', {
          type: 'success'
        });
        await router.push('/login');
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          getCode();
        }
      }
    }
  });
};

const getCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  captchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (captchaEnabled.value) {
    codeUrl.value = 'data:image/gif;base64,' + data.img;
    registerForm.value.uuid = data.uuid;
  }
};

onMounted(() => {
  getCode();
});
</script>

<style lang="scss" scoped>
.register {
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

.register-shell {
  width: min(1180px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 440px);
  gap: 26px;
  align-items: stretch;
}

.register-brand,
.register-form {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 30px 80px rgba(2, 8, 23, 0.32);
  backdrop-filter: blur(18px);
}

.register-brand {
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

.register-form {
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

.register-form .el-input {
  height: 48px;
}

.register-form .input-icon {
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

.register-tip {
  color: var(--app-text-muted);
  font-size: 13px;
}

.register-code {
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

.submit-row {
  margin-bottom: 0;
}

.submit-button {
  width: 100%;
  height: 50px;
  border-radius: 16px;
  box-shadow: 0 18px 34px rgba(53, 109, 255, 0.22);
}

.register-form :deep(.el-input__wrapper) {
  min-height: 48px;
  background-color: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 0 0 1px var(--app-surface-border) inset;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(53, 109, 255, 0.24) inset,
    0 0 0 4px rgba(53, 109, 255, 0.12);
}

.el-register-footer {
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

.register-code-img {
  height: 48px;
  padding-left: 0;
}

@media (max-width: 960px) {
  .register {
    padding: 24px 14px 80px;
  }

  .register-shell {
    grid-template-columns: 1fr;
  }

  .register-brand {
    padding: 28px 24px;
  }

  .brand-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .register-brand {
    display: none;
  }

  .register-form {
    padding: 26px 18px 20px;
  }

  .title-box {
    flex-direction: column;
  }

  .form-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .captcha-row :deep(.el-form-item__content) {
    grid-template-columns: 1fr;
  }
}
</style>
