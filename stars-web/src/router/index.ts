import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '@/constants'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      redirect: '/repos',
    },
    {
      path: '/repos',
      name: 'repos',
      component: () => import('@/views/RepoListView.vue'),
      meta: { title: '仓库列表' },
    },
    {
      path: '/repos/:id',
      name: 'repo-detail',
      component: () => import('@/views/RepoDetailView.vue'),
      meta: { title: '仓库详情' },
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('@/views/ImportCenterView.vue'),
      meta: { title: '导入中心' },
    },
    {
      path: '/tags',
      name: 'tags',
      component: () => import('@/views/TagManageView.vue'),
      meta: { title: '标签管理' },
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!to.meta.public && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && token) {
    return { name: 'repos' }
  }
  if (to.meta.title) {
    document.title = `${String(to.meta.title)} · Stars 知识库`
  }
  return true
})

export default router
