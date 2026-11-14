<template>
  <div class="home">
    <section class="hero-panel">
      <div class="hero-copy">
        <h1>欢迎使用管理工作台</h1>
        <p>在这里查看和管理系统功能，使用左侧菜单开始工作。</p>
      </div>
    </section>

    <section class="section-card capability-card">
      <div class="section-head">
        <h2>常用功能</h2>
      </div>
      <div class="capability-groups">
        <article v-for="group in capabilityGroups" :key="group.title" class="capability-group">
          <h3>{{ group.title }}</h3>
          <ul>
            <li v-for="item in group.items" :key="item">{{ item }}</li>
          </ul>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup name="Index" lang="ts">
const capabilityGroups = [
  { title: '系统管理', items: ['用户管理', '角色权限', '菜单配置'] },
  { title: '运行监控', items: ['在线用户', '操作日志', '系统状态'] },
  { title: '业务工具', items: ['工作流程', '文件管理', '代码生成'] }
];
</script>

<style lang="scss" scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-panel,
.section-card {
  border-radius: 28px;
  border: 1px solid var(--app-surface-border);
  background: var(--app-surface-bg);
  box-shadow: var(--app-shadow-sm);
  backdrop-filter: blur(18px);
}

.hero-panel {
  gap: 18px;
  padding: 30px;
  background: radial-gradient(circle at top left, rgba(53, 109, 255, 0.16), transparent 30%), var(--app-surface-bg);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;

  h1 {
    margin: 0;
    font-size: clamp(30px, 4vw, 46px);
    line-height: 1.06;
    letter-spacing: -0.04em;
    color: var(--app-text-title);
  }

  p {
    margin: 0;
    max-width: 760px;
    color: var(--app-text-muted);
    font-size: 15px;
    line-height: 1.8;
  }
}

.section-card {
  padding: 24px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;

  h2 {
    margin: 6px 0 0;
    color: var(--app-text-title);
    font-size: 26px;
    letter-spacing: -0.03em;
  }
}

.capability-card {
  background: radial-gradient(circle at top right, rgba(14, 165, 233, 0.12), transparent 28%), var(--app-surface-bg);
}

.capability-group {
  padding: 18px 18px 18px 20px;
  border-radius: 22px;
  background: var(--app-elevated-soft-bg);
  border: 1px solid var(--app-surface-border);

  h3 {
    margin: 0 0 12px;
    color: var(--app-text-title);
    font-size: 18px;
  }

  ul {
    margin: 0;
    padding: 0;
    list-style: none;
    display: grid;
    gap: 10px;
  }

  li {
    position: relative;
    padding-left: 16px;
    color: var(--app-text-muted);
    line-height: 1.7;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 10px;
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--app-accent-strong);
      box-shadow: 0 0 0 5px rgba(53, 109, 255, 0.12);
    }
  }
}

@media (max-width: 640px) {
  .hero-panel,
  .section-card {
    padding: 20px;
    border-radius: 22px;
  }
}

html.dark {
  .hero-panel {
    background: radial-gradient(circle at top left, rgba(53, 109, 255, 0.18), transparent 30%), var(--app-surface-bg);
  }
}
</style>
