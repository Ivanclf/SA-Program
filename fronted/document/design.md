# 促销活动管理标准化系统 —— 前端设计文档

## 1. 文档概述

### 1.1 文档目的

本文档基于后端事件驱动架构设计文档及需求文档，针对促销活动管理系统前端进行完整设计。定义前端技术选型、架构分层、路由设计、组件树、状态管理、API 对接方案及关键交互流程，为前端开发提供完整依据。

### 1.2 后端架构背景

本系统后端采用**事件驱动架构（EDA）+ DDD 领域驱动设计**，核心特征为**双状态机联动**：

- **活动状态机**：草稿(0) → 待生效(2) → 生效中(3) → 过时(4)/下线(5) → 结束
- **审核状态机**：等待审核(0) → 审核中(1) → 审核通过(2)/驳回(3)/不通过(4)/作废(5)

前端需围绕双状态机流转设计交互，所有业务操作通过调用后端 API 产生事件，由后端驱动状态变更。

### 1.3 用户角色

| 角色 | 编码 | 权限说明 |
|:---|:---|:---|
| 管理员 | 1 | 创建/编辑/删除活动、提交审核、手动下线、管理 SKU |
| 审核员 | 2 | 审核活动（通过/驳回/不通过/作废） |
| 外部客户 | - | 只读查看活动详情与 SKU 折扣信息 |

---

## 2. 技术选型

| 类别 | 技术 | 版本 | 说明 |
|:---|:---|:---|:---|
| 框架 | Vue 3 | ^3.4 | Composition API + `<script setup>` |
| 构建工具 | Vite | ^5 | 快速开发与构建 |
| 语言 | TypeScript | ^5.3 | 类型安全 |
| 状态管理 | Pinia | ^2.1 | Vue 3 官方推荐 |
| 路由 | Vue Router | ^4.3 | SPA 路由 |
| HTTP 客户端 | Axios | ^1.6 | 请求拦截、统一错误处理 |
| UI 组件库 | Element Plus | ^2.5 | 企业级中后台组件库 |
| CSS 方案 | SCSS + Element Plus 主题变量 | - | 样式覆盖与定制 |
| 表单验证 | Element Plus 内置 + 自定义规则 | - | - |
| 图标 | @element-plus/icons-vue | - | - |
| 工具库 | dayjs | ^1.11 | 时间格式化与计算 |

---

## 3. 架构设计

### 3.1 整体分层架构

```
┌─────────────────────────────────────────────────────────┐
│                      视图层 (Views)                       │
│  登录页 │ 活动管理 │ 活动详情 │ 审核管理 │ SKU管理 │ 用户管理  │
├─────────────────────────────────────────────────────────┤
│                    组件层 (Components)                     │
│  状态标签 │ 活动表单 │ 审核面板 │ SKU选择器 │ 事件时间线 │ 布局  │
├─────────────────────────────────────────────────────────┤
│                    状态管理层 (Pinia Stores)               │
│  authStore │ promotionStore │ auditStore │ skuStore      │
├─────────────────────────────────────────────────────────┤
│                    服务层 (API Services)                   │
│  authApi │ promotionApi │ auditApi │ skuApi │ customerApi │
├─────────────────────────────────────────────────────────┤
│                    工具层 (Utils)                          │
│  request.ts (Axios实例) │ enums.ts │ validators.ts        │
└─────────────────────────────────────────────────────────┘
```

### 3.2 数据流设计

```
用户操作 → 视图层 → Pinia Action → API Service → 后端
                                                      ↓
视图更新 ← Pinia State ← 响应处理 ← API Response ←───┘
```

- **单向数据流**：视图触发 Action → Action 调用 API → 更新 State → 视图响应式更新
- **跨组件通信**：通过 Pinia Store 共享状态，避免 props 多层传递
- **请求拦截**：Axios 拦截器统一处理 Token 注入、401 重定向、异常提示

---

## 4. 目录结构

```
fronted/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                          # API 服务层
│   │   ├── index.ts                  # Axios 实例 + 拦截器
│   │   ├── auth.ts                   # 用户认证 API
│   │   ├── promotion.ts              # 活动管理 API
│   │   ├── audit.ts                  # 审核流程 API
│   │   ├── sku.ts                    # SKU 管理 API
│   │   └── customer.ts              # 客户查询 API
│   ├── assets/                       # 静态资源
│   │   ├── styles/
│   │   │   ├── variables.scss        # SCSS 变量
│   │   │   ├── global.scss           # 全局样式
│   │   │   └── element-override.scss # Element Plus 样式覆盖
│   │   └── images/
│   ├── components/                   # 公共组件
│   │   ├── layout/                   # 布局组件
│   │   │   ├── AppLayout.vue         # 主布局（侧边栏+顶栏+内容）
│   │   │   ├── Sidebar.vue           # 侧边导航栏
│   │   │   └── HeaderBar.vue         # 顶部栏（用户信息/登出）
│   │   ├── common/                   # 通用业务组件
│   │   │   ├── StatusTag.vue         # 状态标签（活动/审核状态）
│   │   │   ├── EventTimeline.vue     # 事件时间线
│   │   │   ├── PromotionForm.vue     # 活动创建/编辑表单
│   │   │   ├── SkuSelector.vue       # SKU 选择器（带搜索/分页）
│   │   │   ├── AuditPanel.vue        # 审核操作面板
│   │   │   ├── ConfirmDialog.vue     # 确认弹窗
│   │   │   └── EmptyState.vue        # 空状态占位
│   │   └── customer/                 # 客户端组件
│   │       ├── CustomerLayout.vue    # 客户端布局
│   │       └── DiscountBadge.vue     # 折扣标签
│   ├── composables/                  # 组合式函数
│   │   ├── usePagination.ts          # 分页逻辑
│   │   ├── usePermission.ts          # 权限判断
│   │   └── useStatusMap.ts           # 状态码映射
│   ├── router/                       # 路由配置
│   │   └── index.ts
│   ├── stores/                       # Pinia 状态管理
│   │   ├── auth.ts                   # 用户认证状态
│   │   ├── promotion.ts              # 活动管理状态
│   │   ├── audit.ts                  # 审核管理状态
│   │   └── sku.ts                   # SKU 管理状态
│   ├── types/                        # TypeScript 类型定义
│   │   ├── api.ts                    # API 响应泛型
│   │   ├── promotion.ts              # 活动相关类型
│   │   ├── audit.ts                  # 审核相关类型
│   │   ├── sku.ts                   # SKU 相关类型
│   │   └── user.ts                   # 用户相关类型
│   ├── utils/                        # 工具函数
│   │   ├── enums.ts                  # 前端枚举定义
│   │   └── validators.ts             # 表单验证规则
│   ├── views/                        # 页面视图
│   │   ├── login/
│   │   │   └── LoginView.vue         # 登录页
│   │   ├── promotion/
│   │   │   ├── PromotionList.vue     # 活动列表页
│   │   │   ├── PromotionCreate.vue   # 活动创建页
│   │   │   ├── PromotionEdit.vue     # 活动编辑页
│   │   │   └── PromotionDetail.vue   # 活动详情页
│   │   ├── audit/
│   │   │   ├── AuditList.vue         # 审核列表页
│   │   │   └── AuditDetail.vue       # 审核详情页
│   │   ├── sku/
│   │   │   ├── SkuList.vue           # SKU 列表页
│   │   │   ├── SkuCreate.vue         # SKU 创建页
│   │   │   └── SkuEdit.vue           # SKU 编辑页
│   │   ├── customer/
│   │   │   ├── PromotionView.vue     # 客户活动详情页
│   │   │   └── SkuDiscount.vue       # 客户 SKU 折扣页
│   │   └── error/
│   │       ├── NotFound.vue          # 404
│   │       └── Forbidden.vue         # 403
│   ├── App.vue                       # 根组件
│   └── main.ts                       # 入口文件
├── .env.development                  # 开发环境变量
├── .env.production                   # 生产环境变量
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## 5. 路由设计

### 5.1 路由表

| 路径 | 页面 | 权限 | 说明 |
|:---|:---|:---|:---|
| `/login` | LoginView | 公开 | 登录页 |
| `/promotion` | PromotionList | 管理员 | 活动列表 |
| `/promotion/create` | PromotionCreate | 管理员 | 创建活动 |
| `/promotion/:id/edit` | PromotionEdit | 管理员 | 编辑活动（仅草稿状态） |
| `/promotion/:id` | PromotionDetail | 管理员/审核员 | 活动详情 + 事件时间线 |
| `/audit` | AuditList | 审核员 | 审核任务列表 |
| `/audit/:id` | AuditDetail | 审核员 | 审核详情 + 操作面板 |
| `/sku` | SkuList | 管理员 | SKU 列表 |
| `/sku/create` | SkuCreate | 管理员 | 创建 SKU |
| `/sku/:id/edit` | SkuEdit | 管理员 | 编辑 SKU |
| `/customer/promotion/:id` | PromotionView | 公开 | 客户查看活动 |
| `/customer/sku/:id` | SkuDiscount | 公开 | 客户查看折扣 |

### 5.2 路由守卫逻辑

```
1. 从 localStorage 读取 token
2. token 不存在 → 跳转 /login
3. token 存在 → 从 authStore 获取用户信息
4. 用户角色不匹配路由权限 → 跳转 403
5. 用户角色为管理员 → 默认跳转 /promotion
6. 用户角色为审核员 → 默认跳转 /audit
```

### 5.3 路由配置结构

```typescript
// 管理端路由（需登录 + 角色）
{
  path: '/',
  component: AppLayout,
  meta: { requiresAuth: true },
  children: [
    // 管理员路由 (role: 1)
    { path: 'promotion', ... meta: { role: 1 } },
    { path: 'sku', ...        meta: { role: 1 } },
    // 审核员路由 (role: 2)
    { path: 'audit', ...      meta: { role: 2 } },
  ]
}
// 客户路由（无需登录）
{
  path: '/customer',
  component: CustomerLayout,
  children: [...]
}
// 登录页
{ path: '/login', component: LoginView }
// 错误页
{ path: '/:pathMatch(.*)*', component: NotFound }
```

---

## 6. 状态管理设计（Pinia Stores）

### 6.1 authStore —— 认证状态

| 字段/方法 | 类型 | 说明 |
|:---|:---|:---|
| `token` | `string \| null` | JWT Token，持久化 localStorage |
| `user` | `User \| null` | 当前用户信息 |
| `isLoggedIn` | `computed` | 是否已登录 |
| `isAdmin` | `computed` | 是否为管理员 (role === 1) |
| `isAuditor` | `computed` | 是否为审核员 (role === 2) |
| `login(username, password)` | `action` | 登录，获取 token |
| `logout()` | `action` | 登出，清除 token |
| `fetchUser()` | `action` | 获取当前用户信息 |
| `register(data)` | `action` | 注册新用户 |

### 6.2 promotionStore —— 活动状态

| 字段/方法 | 类型 | 说明 |
|:---|:---|:---|
| `list` | `Promotion[]` | 活动列表 |
| `current` | `Promotion \| null` | 当前查看的活动详情 |
| `loading` | `boolean` | 列表加载状态 |
| `fetchList()` | `action` | 获取活动列表 |
| `fetchDetail(id)` | `action` | 获取活动详情 |
| `create(data)` | `action` | 创建活动草稿 |
| `update(id, data)` | `action` | 更新活动信息 |
| `delete(id)` | `action` | 删除活动 |
| `submitAudit(id)` | `action` | 提交审核 |
| `offline(id)` | `action` | 手动下线活动 |
| `canEdit(promotion)` | `computed` | 判断活动可否编辑（仅草稿状态） |
| `canSubmit(promotion)` | `computed` | 判断活动可否提交审核 |
| `canOffline(promotion)` | `computed` | 判断活动可否手动下线 |
| `canDelete(promotion)` | `computed` | 判断活动可否删除 |

### 6.3 auditStore —— 审核状态

| 字段/方法 | 类型 | 说明 |
|:---|:---|:---|
| `list` | `AuditRecord[]` | 审核任务列表 |
| `current` | `AuditRecord \| null` | 当前审核记录 |
| `pass(promotionId, data)` | `action` | 审核通过 |
| `reject(promotionId, data)` | `action` | 审核驳回 |
| `notPass(promotionId, data)` | `action` | 审核不通过 |
| `cancel(promotionId, data)` | `action` | 审核作废 |
| `fetchAuditList()` | `action` | 获取审核列表 |
| `fetchAuditStatus(id)` | `action` | 查询审核状态 |

### 6.4 skuStore —— SKU 状态

| 字段/方法 | 类型 | 说明 |
|:---|:---|:---|
| `list` | `Sku[]` | SKU 列表 |
| `current` | `Sku \| null` | 当前 SKU 详情 |
| `fetchList()` | `action` | 获取 SKU 列表 |
| `fetchDetail(id)` | `action` | 获取 SKU 详情 |
| `create(data)` | `action` | 创建 SKU |
| `update(id, data)` | `action` | 更新 SKU |
| `delete(id)` | `action` | 删除 SKU |

---

## 7. API 对接设计

### 7.1 Axios 实例配置

```typescript
// src/api/index.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,  // 开发: http://localhost:8080
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器 — 注入 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 统一错误处理
request.interceptors.response.use(
  response => response.data,           // 直接返回 ApiResponse
  error => {
    const { status, data } = error.response || {}
    switch (status) {
      case 400: ElMessage.error(data?.message || '请求参数错误'); break
      case 401: /* 跳转登录 */; break
      case 403: ElMessage.error('无权限'); break
      case 404: ElMessage.error('资源不存在'); break
      default:  ElMessage.error('服务器错误')
    }
    return Promise.reject(error)
  }
)

export default request
```

### 7.2 API 接口清单

#### 用户认证 (auth.ts)

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/user/login` | 用户登录 |
| POST | `/api/user/logout` | 用户登出 |
| POST | `/api/user/register` | 用户注册 |
| GET | `/api/user/{id}` | 查询用户详情 |
| PUT | `/api/user/update/{id}` | 更新用户信息 |

#### 活动管理 (promotion.ts)

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/promotion/create` | 创建活动草稿 |
| PUT | `/api/promotion/update/{id}` | 更新活动信息 |
| DELETE | `/api/promotion/delete/{id}` | 删除活动 |
| POST | `/api/promotion/submit-audit/{id}` | 提交审核 |
| POST | `/api/promotion/offline/{id}` | 手动下线 |
| GET | `/api/promotion/list` | 活动列表查询 |
| GET | `/api/promotion/{id}` | 活动详情查询 |

#### 审核流程 (audit.ts)

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/audit/pass/{promotionId}` | 审核通过 |
| POST | `/api/audit/reject/{promotionId}` | 审核驳回 |
| POST | `/api/audit/notpass/{promotionId}` | 审核不通过 |
| POST | `/api/audit/cancel/{promotionId}` | 审核作废 |
| GET | `/api/audit/status/{promotionId}` | 查询审核状态 |

#### SKU 管理 (sku.ts)

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/sku/create` | 创建 SKU |
| PUT | `/api/sku/update/{id}` | 更新 SKU |
| DELETE | `/api/sku/delete/{id}` | 删除 SKU |
| GET | `/api/sku/{id}` | 查询 SKU 详情 |
| GET | `/api/sku/list` | 查询 SKU 列表 |

#### 客户查询 (customer.ts)

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| GET | `/api/customer/promotion/{id}` | 查询活动详情 |
| GET | `/api/customer/sku/{id}` | 查询 SKU 折扣 |
| GET | `/api/customer/sku` | 查询活动所有 SKU |

### 7.3 统一响应结构

后端所有接口返回统一结构 `ApiResponse<T>`：

```typescript
// src/types/api.ts
interface ApiResponse<T> {
  code: number        // 200 = 成功，400 = 参数错误，404 = 未找到
  message: string     // 提示信息
  data: T             // 业务数据
}
```

---

## 8. 核心 TypeScript 类型定义

### 8.1 活动相关

```typescript
// src/types/promotion.ts
interface Promotion {
  promotionId: string
  name: string
  stime: string           // ISO datetime
  etime: string           // ISO datetime
  creator: string         // 创建人 userId
  operator: string        // 最近操作人 userId
  status: PromotionStatus // 活动状态码
  auditStatus: AuditStatus // 审核状态码
  ctime: string
  utime: string
  promotionSkus: PromotionSku[]
}

interface PromotionSku {
  id: string
  promotionId: string
  skuId: string
  discount: number        // 0.01 ~ 1.00
}

interface CreatePromotionRequest {
  name: string
  stime: string
  etime: string
  creatorId: string
}

interface UpdatePromotionRequest {
  name?: string
  stime?: string
  etime?: string
  operatorId: string
}
```

### 8.2 审核相关

```typescript
// src/types/audit.ts
interface AuditRecord {
  auditId: string
  promotionId: string
  auditStatus: AuditStatus
  submitTime: string
  completeTime: string
  auditorId: string
  comment: string
  ctime: string
  utime: string
}

interface AuditRequest {
  auditorId: string
  comment: string
}
```

### 8.3 事件相关

```typescript
// src/types/event.ts
interface PromotionEvent {
  eventId: string
  eventType: string
  promotionId: string
  prevActivityStatus: number
  prevAuditStatus: number
  operator: string
  eventTime: string
  params: Record<string, any>
}
```

### 8.4 用户相关

```typescript
// src/types/user.ts
interface User {
  userId: string
  username: string
  role: 1 | 2            // 1=管理员, 2=审核员
  ctime: string
  utime: string
}

interface LoginRequest {
  username: string
  password: string
}

interface RegisterRequest {
  username: string
  password: string
  role: 1 | 2
}
```

### 8.5 SKU 相关

```typescript
// src/types/sku.ts
interface Sku {
  skuId: string
  skuName: string
  originalPrice: number
}

interface CreateSkuRequest {
  skuName: string
  originalPrice: number
}
```

---

## 9. 前端枚举定义

```typescript
// src/utils/enums.ts

/** 活动状态 */
export enum PromotionStatus {
  DRAFT = 0,      // 草稿
  AUDITING = 1,   // 审核中（注意：后端实际用 auditStatus 表示，此处对齐后端枚举）
  INIT = 2,       // 待生效
  ONLINE = 3,     // 生效中
  EXPIRE = 4,     // 过时
  OFFLINE = 5,    // 下线
}

export const PromotionStatusMap: Record<number, { label: string; color: string }> = {
  [PromotionStatus.DRAFT]:    { label: '草稿',   color: 'info' },
  [PromotionStatus.AUDITING]: { label: '审核中', color: 'warning' },
  [PromotionStatus.INIT]:     { label: '待生效', color: 'primary' },
  [PromotionStatus.ONLINE]:   { label: '生效中', color: 'success' },
  [PromotionStatus.EXPIRE]:   { label: '已过期', color: 'default' },
  [PromotionStatus.OFFLINE]:  { label: '已下线', color: 'danger' },
}

/** 审核状态 */
export enum AuditStatus {
  WAITING = 0,     // 等待审核
  AUDITING = 1,    // 审核中
  PASSED = 2,      // 审核通过
  REJECTED = 3,    // 审核驳回
  NOT_PASSED = 4,  // 审核不通过
  CANCELLED = 5,   // 审核拟作废
}

export const AuditStatusMap: Record<number, { label: string; color: string }> = {
  [AuditStatus.WAITING]:    { label: '等待审核',  color: 'info' },
  [AuditStatus.AUDITING]:   { label: '审核中',    color: 'warning' },
  [AuditStatus.PASSED]:     { label: '审核通过',  color: 'success' },
  [AuditStatus.REJECTED]:   { label: '审核驳回',  color: 'danger' },
  [AuditStatus.NOT_PASSED]: { label: '审核不通过', color: 'danger' },
  [AuditStatus.CANCELLED]:  { label: '已作废',    color: 'default' },
}

/** 事件类型 */
export enum EventType {
  E_CREATE_DRAFT = 'E_CREATE_DRAFT',
  E_SUBMIT_AUDIT = 'E_SUBMIT_AUDIT',
  E_AUDIT_PASS = 'E_AUDIT_PASS',
  E_AUDIT_REJECT = 'E_AUDIT_REJECT',
  E_AUDIT_NOTPASS = 'E_AUDIT_NOTPASS',
  E_AUDIT_CANCEL = 'E_AUDIT_CANCEL',
  E_ACTIVE_ONLINE = 'E_ACTIVE_ONLINE',
  E_ACTIVE_EXPIRE = 'E_ACTIVE_EXPIRE',
  E_MANUAL_OFFLINE = 'E_MANUAL_OFFLINE',
  E_UPDATE_ACTIVITY = 'E_UPDATE_ACTIVITY',
  E_DELETE_ACTIVITY = 'E_DELETE_ACTIVITY',
}

export const EventTypeMap: Record<string, string> = {
  [EventType.E_CREATE_DRAFT]: '创建草稿',
  [EventType.E_SUBMIT_AUDIT]: '提交审核',
  [EventType.E_AUDIT_PASS]: '审核通过',
  [EventType.E_AUDIT_REJECT]: '审核驳回',
  [EventType.E_AUDIT_NOTPASS]: '审核不通过',
  [EventType.E_AUDIT_CANCEL]: '审核作废',
  [EventType.E_ACTIVE_ONLINE]: '活动自动生效',
  [EventType.E_ACTIVE_EXPIRE]: '活动过期',
  [EventType.E_MANUAL_OFFLINE]: '手动下线',
  [EventType.E_UPDATE_ACTIVITY]: '更新活动',
  [EventType.E_DELETE_ACTIVITY]: '删除活动',
}
```

---

## 10. 页面设计

### 10.1 登录页 (LoginView)

**路径**: `/login`
**用户**: 管理员、审核员

**布局**: 居中卡片式登录表单

```
┌─────────────────────────────────┐
│                                 │
│      促销活动管理系统            │
│                                 │
│     ┌───────────────────┐       │
│     │  用户名           │       │
│     │  [____________]   │       │
│     │  密码             │       │
│     │  [____________]   │       │
│     │                   │       │
│     │  [  登  录  ]     │       │
│     └───────────────────┘       │
│                                 │
│       没有账号？立即注册        │
│                                 │
└─────────────────────────────────┘
```

**交互要点**:
- 登录成功 → 管理员跳转 `/promotion`，审核员跳转 `/audit`
- 登录失败 → 表单内联错误提示
- 支持 Enter 键提交

### 10.2 活动列表页 (PromotionList)

**路径**: `/promotion`
**用户**: 管理员

**布局**: 表格 + 工具栏

```
┌──────────────────────────────────────────────┐
│  侧边栏  │  [活动管理]                       │
│          │                                   │
│ 活动管理  │  [搜索: ___] [状态筛选: v]         │
│ SKU管理  │  [+ 创建活动]                      │
│          │                                   │
│          │  ┌───────────────────────────────┐ │
│          │  │ ID | 名称 | 状态 | 审核 | 时间 │ │
│          │  │───────────────────────────────│ │
│          │  │ ... | ... | ... | ... | ...   │ │
│          │  │ ... | ... | ... | ... | ...   │ │
│          │  └───────────────────────────────┘ │
│          │  [< 1 2 3 ... >]                  │
└──────────────────────────────────────────────┘
```

**表格列**:
| 列名 | 宽度 | 说明 |
|:---|:---|:---|
| 活动ID | 120 | 缩略显示，hover 展开 |
| 名称 | 200 | 可点击跳转详情 |
| 活动状态 | 100 | `StatusTag` 组件 |
| 审核状态 | 100 | `StatusTag` 组件 |
| 开始时间 | 160 | 格式化显示 |
| 结束时间 | 160 | 格式化显示 |
| 创建人 | 100 | - |
| 操作 | 180 | 动态按钮（见下方） |

**操作按钮逻辑（根据状态组合动态显示）**:

| 活动状态 \ 审核状态 | 可用操作 |
|:---|:---|
| 草稿 + 等待审核 | 编辑、提交审核、删除 |
| 草稿 + 驳回 | 编辑、提交审核、删除 |
| 待生效 + 通过 | 查看（不可操作） |
| 生效中 + 通过 | 手动下线 |
| 过时/下线 + 终态 | 查看（不可操作） |
| 任意 + 终态 | 查看（不可操作） |

### 10.3 活动创建/编辑页 (PromotionCreate / PromotionEdit)

**路径**: `/promotion/create`、`/promotion/:id/edit`
**用户**: 管理员

**布局**: 表单页

```
┌──────────────────────────────────────────────┐
│  侧边栏  │  [< 返回] 创建活动 / 编辑活动      │
│          │                                   │
│          │  基本信息                          │
│          │  ┌─────────────────────────────┐  │
│          │  │ 活动名称: [_______________] │  │
│          │  │ 开始时间: [📅 选择日期时间]  │  │
│          │  │ 结束时间: [📅 选择日期时间]  │  │
│          │  └─────────────────────────────┘  │
│          │                                   │
│          │  关联SKU（编辑时可修改）            │
│          │  ┌─────────────────────────────┐  │
│          │  │ [+ 添加SKU]                 │  │
│          │  │ SKU名称    | 原价  | 折扣   │  │
│          │  │────────────────────────────│  │
│          │  │ SKU-001    | ¥100 | [0.8] │  │
│          │  │ [移除]                     │  │
│          │  └─────────────────────────────┘  │
│          │                                   │
│          │  [保 存 草 稿]                    │
└──────────────────────────────────────────────┘
```

**验证规则**:
| 字段 | 规则 |
|:---|:---|
| 活动名称 | 必填，1-100 字符 |
| 开始时间 | 必填，不早于当前时间 |
| 结束时间 | 必填，必须晚于开始时间 |
| 关联 SKU | 至少选择 1 个 |
| SKU 折扣 | 0.01 ~ 1.00，保留两位小数 |

### 10.4 活动详情页 (PromotionDetail)

**路径**: `/promotion/:id`
**用户**: 管理员、审核员

**布局**: 信息展示 + 事件时间线

```
┌──────────────────────────────────────────────┐
│  侧边栏  │  [< 返回] 活动详情                 │
│          │                                   │
│          │  活动信息                          │
│          │  ┌─────────────────────────────┐  │
│          │  │ 名称: XXX 状态: [草稿]      │  │
│          │  │ 审核: [等待审核]            │  │
│          │  │ 时间: 2026-06-01 ~ 2026-07-01│ │
│          │  │ 创建人: admin | 操作人: admin│  │
│          │  └─────────────────────────────┘  │
│          │                                   │
│          │  关联SKU                          │
│          │  ┌─────────────────────────────┐  │
│          │  │ SKU-001 | ¥100 | 8折 | ¥80 │  │
│          │  │ SKU-002 | ¥200 | 7折 | ¥140│  │
│          │  └─────────────────────────────┘  │
│          │                                   │
│          │  事件时间线                        │
│          │  ● 2026-06-01 10:00 创建草稿     │
│          │  │                              │
│          │  ● 2026-06-01 10:05 提交审核     │
│          │  │                              │
│          │  ○ 待发生：审核通过               │
│          │                                   │
│          │  [操作按钮区域：提交审核/编辑等]    │
└──────────────────────────────────────────────┘
```

### 10.5 审核列表页 (AuditList)

**路径**: `/audit`
**用户**: 审核员

**布局**: 表格 + 筛选

```
┌──────────────────────────────────────────────┐
│  侧边栏  │  [审核任务]                       │
│          │                                   │
│ 审核任务  │  [状态筛选: 全部/待审核/已审核]     │
│          │                                   │
│          │  ┌───────────────────────────────┐ │
│          │  │ 活动ID | 名称 | 审核状态      │ │
│          │  │───────────────────────────────│ │
│          │  │ P001 | 618大促 | 审核中      │ │
│          │  │ P002 | 双11   | 等待审核     │ │
│          │  └───────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

### 10.6 审核详情页 (AuditDetail)

**路径**: `/audit/:id`
**用户**: 审核员

**布局**: 活动信息 + 审核操作面板

```
┌──────────────────────────────────────────────┐
│  侧边栏  │  [< 返回] 审核详情                 │
│          │                                   │
│          │  活动信息（只读，同活动详情页）      │
│          │  ┌─────────────────────────────┐  │
│          │  │ 名称: XXX | 时间: ...       │  │
│          │  │ SKU列表...                  │  │
│          │  └─────────────────────────────┘  │
│          │                                   │
│          │  审核操作（仅审核中状态可见）        │
│          │  ┌─────────────────────────────┐  │
│          │  │ 审核意见: [_____________]   │  │
│          │  │                             │  │
│          │  │ [通过] [驳回] [不通过] [作废] │  │
│          │  └─────────────────────────────┘  │
│          │                                   │
│          │  审核历史                          │
│          │  ● 2026-06-01 提交审核            │
└──────────────────────────────────────────────┘
```

**按钮权限控制**:

| 当前审核状态 | 可用按钮 |
|:---|:---|
| 审核中 | 通过、驳回、不通过 |
| 等待审核 | 作废 |
| 已驳回 | 作废 |
| 终态（通过/不通过/已作废） | 不显示操作面板 |

### 10.7 SKU 管理页 (SkuList / SkuCreate / SkuEdit)

**路径**: `/sku`、`/sku/create`、`/sku/:id/edit`
**用户**: 管理员

功能较简单，列表页提供 CRUD 表格，创建/编辑页提供表单。

### 10.8 客户查询页（外部用户）

**路径**: `/customer/promotion/:id`、`/customer/sku/:id`
**用户**: 公开访问

简洁布局，只读展示活动信息和 SKU 折扣价格，无侧边栏和操作按钮。

### 10.9 错误页

- **404**: "页面不存在" + 返回首页按钮
- **403**: "无权限访问" + 返回登录页按钮

---

## 11. 核心组件设计

### 11.1 StatusTag.vue —— 状态标签

**Props**:
| 属性 | 类型 | 说明 |
|:---|:---|:---|
| `status` | `number` | 状态码 |
| `type` | `'promotion' \| 'audit'` | 状态类型 |

根据 `type` 和 `status` 从枚举映射表取 `{ label, color }`，渲染 `<el-tag :type="color">`

### 11.2 EventTimeline.vue —— 事件时间线

**Props**:
| 属性 | 类型 | 说明 |
|:---|:---|:---|
| `events` | `PromotionEvent[]` | 事件列表 |

使用 `<el-timeline>` 渲染事件流：
- 已完成事件：实心圆点 + 蓝色
- 当前事件：实心圆点 + 绿色（带呼吸动画）
- 未来事件：空心圆点 + 灰色

### 11.3 PromotionForm.vue —— 活动表单

**Props**:
| 属性 | 类型 | 说明 |
|:---|:---|:---|
| `mode` | `'create' \| 'edit'` | 表单模式 |
| `initialData` | `Promotion?` | 编辑时的初始值 |

**Emits**:
| 事件 | 参数 | 说明 |
|:---|:---|:---|
| `submit` | `CreatePromotionRequest \| UpdatePromotionRequest` | 提交表单 |

封装活动名称、时间范围、SKU 选择的完整表单逻辑。

### 11.4 SkuSelector.vue —— SKU 选择器

**Props**:
| 属性 | 类型 | 说明 |
|:---|:---|:---|
| `modelValue` | `PromotionSku[]` | 已选 SKU 列表（v-model） |

内嵌 `<el-table>` + `<el-pagination>`，支持搜索和批量选择 SKU 并设置折扣。

### 11.5 AuditPanel.vue —— 审核操作面板

**Props**:
| 属性 | 类型 | 说明 |
|:---|:---|:---|
| `promotionId` | `string` | 活动 ID |
| `auditStatus` | `AuditStatus` | 当前审核状态 |

**Emits**:
| 事件 | 参数 | 说明 |
|:---|:---|:---|
| `audited` | - | 审核操作完成后通知父组件刷新 |

根据 `auditStatus` 动态渲染可用按钮（通过/驳回/不通过/作废），点击弹出 `ConfirmDialog` 确认。

### 11.6 ConfirmDialog.vue —— 确认弹窗

**Props**:
| 属性 | 类型 | 说明 |
|:---|:---|:---|
| `title` | `string` | 弹窗标题 |
| `message` | `string` | 确认内容 |
| `confirmText` | `string` | 确认按钮文字 |
| `danger` | `boolean` | 是否为危险操作（按钮变红） |

封装 `<el-message-box>`，统一确认交互。

---

## 12. 交互流程

### 12.1 完整活动创建至生效流程（前端视角）

```
管理员
  │
  ├─ 1. 进入活动列表 → 点击「创建活动」
  │     ↓
  │   填写表单（名称、时间、SKU+折扣）
  │     ↓
  │   点击「保存草稿」→ POST /api/promotion/create
  │     ↓
  │   活动创建成功（状态: 草稿, 审核: 等待审核）
  │     ↓
  ├─ 2. 在列表页找到活动 → 点击「提交审核」
  │     ↓
  │   POST /api/promotion/submit-audit/{id}
  │     ↓
  │   审核状态变为「审核中」
  │
  │   ─────── 审核员操作 ───────
  │
  │ 3. 审核员登录 → 审核列表页看到待审核活动
  │     ↓
  │   点击进入审核详情 → 查看活动信息
  │     ↓
  │   输入审核意见 → 点击「审核通过」
  │     ↓
  │   POST /api/audit/pass/{promotionId}
  │     ↓
  │   活动状态变为「待生效」
  │
  │   ─────── 定时任务 ───────
  │
  │ 4. 到达开始时间 → 后端自动触发 E_ACTIVE_ONLINE
  │     ↓
  │   活动状态变为「生效中」
  │     ↓
  │ 5. 到达结束时间 → 后端自动触发 E_ACTIVE_EXPIRE
  │     ↓
  │   活动状态变为「已过期」→ 终态
```

### 12.2 审核驳回后重新提交流程

```
审核员驳回 → 活动审核状态变为「审核驳回」
  ↓
管理员在活动列表看到活动（状态: 草稿, 审核: 驳回）
  ↓
点击「编辑」→ 修改活动内容
  ↓
保存后 → 点击「提交审核」
  ↓
审核状态重新变为「审核中」
```

### 12.3 手动下线流程

```
管理员在活动列表找到生效中的活动
  ↓
点击「手动下线」→ 弹出确认对话框
  ↓
确认 → POST /api/promotion/offline/{id}
  ↓
活动状态变为「已下线」→ 终态
```

### 12.4 错误处理流程

```
API 请求
  ├─ 200 → 正常展示数据或 ElMessage.success
  ├─ 400 → ElMessage.error(后端message) 或表单内联错误
  ├─ 401 → 清除 token → 跳转登录页
  ├─ 403 → ElMessage.error('无权限')
  ├─ 404 → ElMessage.error('资源不存在')
  └─ 5xx → ElMessage.error('服务器错误，请稍后重试')
```

---

## 13. 权限控制设计

### 13.1 路由级权限

在 `router/index.ts` 的 `beforeEach` 守卫中：

```typescript
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 1. 公开路由直接放行
  if (!to.meta.requiresAuth) return next()

  // 2. 未登录 → 登录页
  if (!authStore.token) return next('/login')

  // 3. 角色检查
  const requiredRole = to.meta.role as number
  if (requiredRole && authStore.user?.role !== requiredRole) {
    return next('/403')
  }

  next()
})
```

### 13.2 组件级权限

通过 `usePermission` composable 控制按钮显隐：

```typescript
// composables/usePermission.ts
export function usePermission() {
  const authStore = useAuthStore()
  const isAdmin = computed(() => authStore.user?.role === 1)
  const isAuditor = computed(() => authStore.user?.role === 2)
  return { isAdmin, isAuditor }
}
```

模板中使用 `v-if="isAdmin"` 控制按钮可见性。

### 13.3 操作级权限

按状态组合控制操作按钮可用性（见 10.2 节操作按钮逻辑表），通过 computed 属性计算：

```typescript
const canEdit = computed(() =>
  current.value?.status === PromotionStatus.DRAFT
)
const canSubmitAudit = computed(() =>
  current.value?.status === PromotionStatus.DRAFT
  && [AuditStatus.WAITING, AuditStatus.REJECTED].includes(current.value.auditStatus)
)
const canOffline = computed(() =>
  current.value?.status === PromotionStatus.ONLINE
)
```

---

## 14. 开发规范

### 14.1 命名规范

| 类型 | 规范 | 示例 |
|:---|:---|:---|
| 组件文件 | PascalCase | `PromotionList.vue` |
| 组合式函数 | camelCase，use 前缀 | `usePermission.ts` |
| Store | camelCase | `promotionStore` |
| API 函数 | camelCase | `createPromotion()` |
| 类型/接口 | PascalCase | `Promotion`, `ApiResponse<T>` |
| 常量/枚举 | UPPER_SNAKE 或 PascalCase | `PromotionStatus`, `API_BASE_URL` |
| CSS class | kebab-case | `.promotion-list` |

### 14.2 组件开发规范

- 使用 `<script setup lang="ts">` 语法
- Props 和 Emits 使用 TypeScript 类型推导
- 表单统一使用 Element Plus 的 `el-form` + 验证规则
- 列表页使用 `el-table` + `el-pagination`
- 所有异步操作需要 loading 状态和错误处理

### 14.3 Git 提交规范

- `feat: 新增活动列表页`
- `fix: 修复审核状态标签显示错误`
- `refactor: 重构 API 请求拦截器`
- `style: 调整侧边栏宽度`

---

## 15. 环境变量

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080

# .env.production
VITE_API_BASE_URL=https://api.example.com
```

---

## 16. 开发计划

| 阶段 | 内容 | 预估工时 |
|:---|:---|:---|
| 第1阶段 | 项目脚手架搭建：Vite + Vue3 + TS + Element Plus + 路由 + Pinia + Axios | 0.5天 |
| 第2阶段 | 登录/注册 + 权限路由守卫 + Layout 布局 | 1天 |
| 第3阶段 | 活动管理模块（列表/创建/编辑/详情）+ 状态标签组件 | 2天 |
| 第4阶段 | 审核模块（审核列表/审核详情/审核操作面板）+ 事件时间线组件 | 1.5天 |
| 第5阶段 | SKU 管理模块 + SKU 选择器组件 | 1天 |
| 第6阶段 | 客户查询页（活动详情/SKU 折扣） | 0.5天 |
| 第7阶段 | 联调测试 + 错误处理完善 + UI 细节打磨 | 1天 |
| **合计** | | **7.5天** |

---

## 17. 附录

### 17.1 引用文档

- 促销活动管理标准化系统需求文档（PRD）
- 促销活动管理标准化系统——用例分析与质量属性需求设计文档
- 促销活动管理标准化系统——事件驱动架构设计文档
- 促销活动管理标准化系统 架构 ATAM 评估意见书

### 17.2 后端接口完整对照（附录）

见第 7.2 节及后端 `requirement.md` 第 5 章接口设计。

### 17.3 双状态机前端位图

| 活动状态 ↓ \ 审核状态 → | 等待审核(0) | 审核中(1) | 通过(2) | 驳回(3) | 不通过(4) | 作废(5) |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| 草稿(0) | ✓ 编辑/提交/删除 | - | - | ✓ 编辑/提交/删除 | 终态-只读 | 终态-只读 |
| 待生效(2) | - | - | ✓ 只读 | - | - | - |
| 生效中(3) | - | - | ✓ 手动下线 | - | - | - |
| 过时(4) | - | - | 终态-只读 | - | - | - |
| 下线(5) | - | - | 终态-只读 | - | - | - |
