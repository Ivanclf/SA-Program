import { createRouter, createMemoryHistory, type RouteRecordRaw } from 'vue-router'
import { parseToken } from '@/utils/token'
import type { User } from '@/types/user'

/** 角色编码 */
const Role = { ADMIN: 1, AUDITOR: 2 } as const

// ---- 懒加载：已有页面直接引用，缺失页面用占位 stub ----
const LoginView = () => import('@/views/login/LoginView.vue')
const NotFound = () => import('@/views/error/NotFound.vue')
const Forbidden = () => import('@/views/error/Forbidden.vue')
const AppLayout = () => import('@/components/layout/AppLayout.vue')
const CustomerLayout = () => import('@/components/customer/CustomerLayout.vue')

// 占位 stub（阶段 3-6 实现后替换）
const PromotionList = () => import('@/views/promotion/PromotionList.vue')
const PromotionCreate = () => import('@/views/promotion/PromotionCreate.vue')
const PromotionEdit = () => import('@/views/promotion/PromotionEdit.vue')
const PromotionDetail = () => import('@/views/promotion/PromotionDetail.vue')
const AuditList = () => import('@/views/audit/AuditList.vue')
const AuditDetail = () => import('@/views/audit/AuditDetail.vue')
const SkuList = () => import('@/views/sku/SkuList.vue')
const SkuCreate = () => import('@/views/sku/SkuCreate.vue')
const SkuEdit = () => import('@/views/sku/SkuEdit.vue')
const PromotionView = () => import('@/views/customer/PromotionView.vue')
const SkuDiscount = () => import('@/views/customer/SkuDiscount.vue')

// ---- 路由表 ----
const routes: RouteRecordRaw[] = [
  // ===== 公开路由 =====
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: Forbidden,
  },

  // ===== 管理端路由（需登录）=====
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/promotion' },
      {
        path: 'promotion',
        name: 'PromotionList',
        component: PromotionList,
        meta: { requiresAuth: true },
      },
      {
        path: 'promotion/create',
        name: 'PromotionCreate',
        component: PromotionCreate,
        meta: { requiresAuth: true, requireRole: Role.ADMIN },
      },
      {
        path: 'promotion/:id/edit',
        name: 'PromotionEdit',
        component: PromotionEdit,
        meta: { requiresAuth: true, requireRole: Role.ADMIN },
      },
      {
        path: 'promotion/:id',
        name: 'PromotionDetail',
        component: PromotionDetail,
        meta: { requiresAuth: true },
      },
      {
        path: 'audit',
        name: 'AuditList',
        component: AuditList,
        meta: { requiresAuth: true, requireRole: Role.AUDITOR },
      },
      {
        path: 'audit/:id',
        name: 'AuditDetail',
        component: AuditDetail,
        meta: { requiresAuth: true, requireRole: Role.AUDITOR },
      },
      {
        path: 'sku',
        name: 'SkuList',
        component: SkuList,
        meta: { requiresAuth: true },
      },
      {
        path: 'sku/create',
        name: 'SkuCreate',
        component: SkuCreate,
        meta: { requiresAuth: true, requireRole: Role.ADMIN },
      },
      {
        path: 'sku/:id/edit',
        name: 'SkuEdit',
        component: SkuEdit,
        meta: { requiresAuth: true, requireRole: Role.ADMIN },
      },
    ],
  },

  // ===== 客户路由（公开）=====
  {
    path: '/customer',
    component: CustomerLayout,
    children: [
      {
        path: 'promotion/:id',
        name: 'CustomerPromotionView',
        component: PromotionView,
      },
      {
        path: 'sku/:id',
        name: 'CustomerSkuDiscount',
        component: SkuDiscount,
      },
    ],
  },

  // ===== 404 兜底 =====
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
  },
]

/** 刷新恢复 key */
const LAST_PATH_KEY = 'lastPath'

// ---- 路由实例 ----
const router = createRouter({
  // Windows 兼容：Memory 模式不修改浏览器 URL，避免触发窗口强制恢复
  history: createMemoryHistory(),
  routes,
})

/** 从 localStorage 获取当前用户角色，失败返回 null */
function getUserRole(): number | null {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    const user: User = JSON.parse(raw)
    return user.role ?? null
  } catch {
    return null
  }
}

// ---- 全局前置守卫 ----
router.beforeEach((to, _from, next) => {
  // 不需要登录 → 直接放行
  if (!to.meta.requiresAuth) return next()

  // 需要登录 → 检查 token 是否有效（含 24h 过期判断）
  const token = localStorage.getItem('token')
  if (!token || !parseToken(token)) {
    // token 不存在或已过期 → 清除残留，跳登录
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    return next('/login')
  }

  // 需要特定角色 → 检查用户角色
  // - requireRole=1 (ADMIN): 仅管理员可访问（创建/编辑活动、管理 SKU）
  // - requireRole=2 (AUDITOR): 审核员可访问，管理员也可访问（管理员监控审核状态）
  const requireRole = to.meta.requireRole as number | undefined
  if (requireRole) {
    const userRole = getUserRole()
    if (userRole === Role.ADMIN) return next()   // 管理员拥有所有权限
    if (userRole !== requireRole) {
      return next('/403')
    }
  }

  next()
})

// 每次导航后保存路径，用于刷新恢复（MemoryHistory 刷新后丢失路由状态）
router.afterEach((to) => {
  if (to.path !== '/login' && to.path !== '/403') {
    sessionStorage.setItem(LAST_PATH_KEY, to.fullPath)
  }
})

// 刷新后恢复上次路径
router.isReady().then(() => {
  const saved = sessionStorage.getItem(LAST_PATH_KEY)
  if (saved && saved !== '/' && router.currentRoute.value.path === '/') {
    router.push(saved)
  }
})

export default router
