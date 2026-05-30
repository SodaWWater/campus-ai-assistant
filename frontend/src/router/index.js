import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', name: 'StudentDashboard', component: () => import('../views/student/StudentDashboard.vue') },
      { path: 'knowledge-bases', name: 'KnowledgeBaseExplore', component: () => import('../views/student/KnowledgeBaseExplore.vue') },
      { path: 'chat', name: 'ChatWorkspace', component: () => import('../views/student/ChatWorkspace.vue') },
      { path: 'academic', name: 'StudentAcademic', component: () => import('../views/student/StudentAcademic.vue') }
    ]
  },
  {
    path: '/teacher',
    component: () => import('../layouts/TeacherLayout.vue'),
    meta: { role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/dashboard' },
      { path: 'dashboard', name: 'TeacherDashboard', component: () => import('../views/teacher/TeacherDashboard.vue') },
      { path: 'knowledge-bases', name: 'KnowledgeBaseManage', component: () => import('../views/teacher/KnowledgeBaseManage.vue') },
      { path: 'documents/:kbId', name: 'DocumentCenter', component: () => import('../views/teacher/DocumentCenter.vue'), props: true },
      { path: 'questions', name: 'QuestionAnalytics', component: () => import('../views/teacher/QuestionAnalytics.vue') },
      { path: 'chat-preview', name: 'TeacherChatPreview', component: () => import('../views/teacher/TeacherChatPreview.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/AdminDashboard.vue') },
      { path: 'users', name: 'UserManage', component: () => import('../views/admin/UserManage.vue') },
      { path: 'knowledge-bases', name: 'KnowledgeBaseAudit', component: () => import('../views/admin/KnowledgeBaseAudit.vue') },
      { path: 'documents', name: 'DocumentTasks', component: () => import('../views/admin/DocumentTasks.vue') },
      { path: 'system', name: 'SystemSettings', component: () => import('../views/admin/SystemSettings.vue') }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 导航守卫：未登录跳转登录页，角色不匹配跳转首页
router.beforeEach((to, from, next) => {
  if (to.name === 'Login') {
    next()
    return
  }

  const userStr = localStorage.getItem('user')
  if (!userStr) {
    next({ name: 'Login' })
    return
  }

  try {
    const user = JSON.parse(userStr)
    const requiredRole = to.meta.role

    if (requiredRole && user.role !== requiredRole) {
      // 允许 ADMIN 访问所有
      if (user.role === 'ADMIN') {
        next()
        return
      }
      // 跳转到对应角色的首页
      if (user.role === 'STUDENT') next('/student/dashboard')
      else if (user.role === 'TEACHER') next('/teacher/dashboard')
      else next('/login')
      return
    }

    next()
  } catch {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    next({ name: 'Login' })
  }
})

export default router
