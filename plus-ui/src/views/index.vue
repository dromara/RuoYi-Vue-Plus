<template>
  <div class="home">
    <section class="hero-panel">
      <div class="hero-copy">
        <h1>RuoYi-Vue-Plus 控制台</h1>
        <p>
          企业级后台管理系统 重写 RuoYi-Vue 所有功能 集成 Sa-Token、Mybatis-Plus、WarmFlow、SpringDoc、Hutool、OSS
          等组件
        </p>
        <div class="hero-actions">
          <el-button type="primary" @click="goTarget('https://gitee.com/dromara/RuoYi-Vue-Plus')">查看源码</el-button>
          <el-button plain @click="goTarget('https://plus-doc.dromara.org/#/ruoyi-vue-plus/changlog')">
            更新日志
          </el-button>
        </div>
      </div>
    </section>

    <div class="content-grid">
      <section class="section-card">
        <div class="section-head">
          <div>
            <h2>项目矩阵</h2>
          </div>
        </div>
        <div class="product-list">
          <article v-for="product in products" :key="product.name" class="product-card">
            <div class="product-top">
              <div>
                <h3>{{ product.name }}</h3>
                <p>{{ product.summary }}</p>
              </div>
              <span class="product-version">{{ product.version }}</span>
            </div>
            <div class="product-tags">
              <el-tag v-for="tag in product.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
            </div>
            <div class="product-actions">
              <el-button type="primary" plain @click="goTarget(product.primaryUrl)">
                {{ product.primaryLabel }}
              </el-button>
              <el-button plain @click="goTarget(product.secondaryUrl)">{{ product.secondaryLabel }}</el-button>
            </div>
          </article>
        </div>
      </section>

      <section class="section-card capability-card">
        <div class="section-head">
          <div>
            <h2>能力地图</h2>
          </div>
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
  </div>
</template>

<script setup name="Index" lang="ts">
const products = [
  {
    name: 'RuoYi-Vue-Plus',
    version: 'v6.1.0',
    summary: '面向分布式集群场景的后台管理系统，保持现有业务接口与权限逻辑，适合先完成前端壳升级。',
    tags: ['Vue 3', 'Element Plus', 'Spring Boot', 'Sa-Token'],
    primaryLabel: '访问 GitHub',
    primaryUrl: 'https://github.com/dromara/RuoYi-Vue-Plus',
    secondaryLabel: '查看更新日志',
    secondaryUrl: 'https://plus-doc.dromara.org/#/ruoyi-vue-plus/changlog'
  },
  {
    name: 'RuoYi-Cloud-Plus',
    version: 'v6.1.0',
    summary: '微服务通用权限管理系统，适合更复杂的服务治理场景，也可以沿用同样的前端升级思路。',
    tags: ['Spring Cloud', 'Gateway', 'Nacos', 'Dubbo'],
    primaryLabel: '访问 GitHub',
    primaryUrl: 'https://github.com/dromara/RuoYi-Cloud-Plus',
    secondaryLabel: '查看更新日志',
    secondaryUrl: 'https://plus-doc.dromara.org/#/ruoyi-cloud-plus/changlog'
  }
];

const capabilityGroups = [
  {
    title: '后端基建',
    items: ['Spring Boot / Spring Cloud', 'Sa-Token 认证与权限', 'MySQL / Redis', '代码生成器']
  },
  {
    title: '平台能力',
    items: ['动态菜单与按钮权限', '监控、日志、在线用户', '任务调度与工作流', '文件存储与多云适配']
  },
  {
    title: '前端方向',
    items: ['UI 卡片化', '主题与布局统一', '通用页面容器规范化', '企业化布局']
  }
];

const goTarget = (url: string) => {
  window.open(url, '__blank');
};
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
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
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

.hero-badge {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(53, 109, 255, 0.12);
  color: var(--app-accent-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.hero-stats {
  display: grid;
  gap: 12px;
}

.stat-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: var(--app-elevated-soft-bg);
  border: 1px solid var(--app-surface-border);
  display: flex;
  flex-direction: column;
  gap: 6px;

  strong {
    color: var(--app-text-title);
    font-size: 24px;
    letter-spacing: -0.03em;
  }

  span {
    color: var(--app-text-muted);
    font-size: 13px;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 20px;
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

.section-kicker {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-list,
.capability-groups {
  display: grid;
  gap: 16px;
}

.product-card {
  padding: 22px;
  border-radius: 24px;
  background: var(--app-elevated-soft-bg);
  border: 1px solid var(--app-surface-border);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--app-shadow-sm);
    border-color: rgba(53, 109, 255, 0.2);
  }
}

.product-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h3 {
    margin: 0 0 8px;
    color: var(--app-text-title);
    font-size: 22px;
    letter-spacing: -0.03em;
  }

  p {
    margin: 0;
    color: var(--app-text-muted);
    line-height: 1.8;
  }
}

.product-version {
  flex-shrink: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(53, 109, 255, 0.12);
  color: var(--app-accent-strong);
  font-size: 12px;
  font-weight: 700;
}

.product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.product-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
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

@media (max-width: 960px) {
  .hero-panel,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-panel,
  .section-card {
    padding: 20px;
    border-radius: 22px;
  }

  .product-top {
    flex-direction: column;
  }
}

html.dark {
  .hero-panel {
    background: radial-gradient(circle at top left, rgba(53, 109, 255, 0.18), transparent 30%), var(--app-surface-bg);
  }
}
</style>
