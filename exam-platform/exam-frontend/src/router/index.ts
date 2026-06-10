/**
 * 路由配置
 * 三端通过路由前缀隔离：
 *   /teacher/*   — 学员端（老师答题）
 *   /principal/* — 管理端（校长发布试卷、批阅）
 *   /admin/*     — 管理后台（管理员配置项目）
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  // ======================== 登录页 ========================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },

  // ======================== 注册页 ========================
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' },
  },

  // ======================== 学员端 /teacher ========================
  {
    path: '/teacher',
    component: () => import('@/components/AppLayout.vue'),
    meta: { title: '学员端', role: 'teacher' },
    children: [
      {
        path: '',
        redirect: '/teacher/projects',
      },
      {
        path: 'projects',
        name: 'TeacherProjects',
        component: () => import('@/views/teacher/ProjectList.vue'),
        meta: { title: '项目列表' },
      },
      {
        path: 'project/:projectId/tools',
        name: 'ProjectTools',
        component: () => import('@/views/teacher/ProjectTools.vue'),
        meta: { title: '项目工具' },
      },
      {
        path: 'papers/:projectId',
        name: 'TeacherPapers',
        component: () => import('@/views/teacher/PaperList.vue'),
        meta: { title: '试卷列表' },
      },
      {
        path: 'exam/:paperId',
        name: 'TeacherExam',
        component: () => import('@/views/teacher/ExamPage.vue'),
        meta: { title: '答题' },
      },
      {
        path: 'score/:paperId',
        name: 'TeacherScore',
        component: () => import('@/views/teacher/ScoreDetail.vue'),
        meta: { title: '成绩详情' },
      },
    ],
  },

  // ======================== 管理端 /principal ========================
  {
    path: '/principal',
    component: () => import('@/components/AppLayout.vue'),
    meta: { title: '管理端', role: 'principal' },
    children: [
      {
        path: '',
        redirect: '/principal/projects',
      },
      {
        path: 'projects',
        name: 'PrincipalProjects',
        component: () => import('@/views/teacher/ProjectList.vue'),
        meta: { title: '项目列表' },
      },
      {
        path: 'project/:projectId/tools',
        name: 'PrincipalProjectTools',
        component: () => import('@/views/principal/PrincipalProjectTools.vue'),
        meta: { title: '项目工具' },
      },
      {
        path: 'papers/:projectId',
        name: 'PrincipalPapers',
        component: () => import('@/views/principal/PaperManage.vue'),
        meta: { title: '试卷管理' },
      },
      {
        path: 'paper/create/:projectId',
        name: 'PaperCreate',
        component: () => import('@/views/principal/PaperCreate.vue'),
        meta: { title: '创建试卷' },
      },
      {
        path: 'paper/:paperId/questions',
        name: 'QuestionEdit',
        component: () => import('@/views/principal/QuestionEdit.vue'),
        meta: { title: '题目编辑' },
      },
      {
        path: 'review',
        name: 'ReviewList',
        component: () => import('@/views/principal/ReviewList.vue'),
        meta: { title: '批阅试卷' },
      },
      {
        path: 'review/:paperId/:userId',
        name: 'ReviewDetail',
        component: () => import('@/views/principal/ReviewDetail.vue'),
        meta: { title: '批阅详情' },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/principal/Statistics.vue'),
        meta: { title: '数据统计' },
      },
    ],
  },

  // ======================== 管理后台 /admin ========================
  {
    path: '/admin',
    component: () => import('@/components/AppLayout.vue'),
    meta: { title: '管理后台', role: 'admin' },
    children: [
      {
        path: '',
        redirect: '/admin/projects',
      },
      {
        path: 'projects',
        name: 'AdminProjects',
        component: () => import('@/views/admin/ProjectManage.vue'),
        meta: { title: '项目管理' },
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'project/:projectId/tools',
        name: 'AdminProjectTools',
        component: () => import('@/views/admin/AdminProjectTools.vue'),
        meta: { title: '项目管理' },
      },
      {
        path: 'project/:projectId/config',
        name: 'ToolConfig',
        component: () => import('@/views/admin/ToolConfig.vue'),
        meta: { title: '工具配置' },
      },
      {
        path: 'project/:projectId/roles',
        name: 'RoleConfig',
        component: () => import('@/views/admin/RoleConfig.vue'),
        meta: { title: '角色权限配置' },
      },
    ],
  },

  // ======================== 根路径重定向 ========================
  {
    path: '/',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局路由守卫：检查登录状态和角色权限
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 登录页和注册页不需要 Token
  if (to.path === '/login' || to.path === '/register') {
    if (token) {
      const role = localStorage.getItem('userRole')
      const rolePath = role === 'admin' ? '/admin' : role === 'principal' ? '/principal' : '/teacher'
      next(rolePath)
    } else {
      next()
    }
    return
  }
  
  // 其他页面需要 Token
  if (!token) {
    next('/login')
    return
  }
  
  // 角色路由保护：非管理员不能访问 /admin/*，非校长不能访问 /principal/*
  const role = localStorage.getItem('userRole') || 'teacher'
  if (to.path.startsWith('/admin') && role !== 'admin') {
    next(role === 'principal' ? '/principal/projects' : '/teacher/projects')
    return
  }
  if (to.path.startsWith('/principal') && role === 'teacher') {
    next('/teacher/projects')
    return
  }
  
  next()
})

// 全局路由守卫：设置页面标题
router.afterEach((to) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - 教师培训在线考试平台`
  }
})

export default router
