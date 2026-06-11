# 促销活动管理标准化系统

> **软件体系结构课程项目作业**
>
> 基于事件驱动架构（EDA）+ 领域驱动设计（DDD）+ 双状态机联动的促销活动全生命周期管理平台。

---

## 目录

- [1. 项目简介](#1-项目简介)
- [2. 技术栈](#2-技术栈)
- [3. 架构设计](#3-架构设计)
- [4. 目录结构](#4-目录结构)
- [5. 快速开始](#5-快速开始)
- [6. 功能模块](#6-功能模块)
- [7. API 接口](#7-api-接口)
- [8. 测试](#8-测试)
- [9. 设计文档](#9-设计文档)

---

## 1. 项目简介

本系统为促销活动全生命周期管理平台，核心特征为**状态驱动业务、事件触发流转**。系统包含两套独立且联动的状态机：

- **活动状态机**：草稿(0) → 待生效(2) → 生效中(3) → 过时(4) / 下线(5) → 结束
- **审核状态机**：等待审核(0) → 审核中(1) → 通过(2) / 驳回(3) / 不通过(4) / 作废(5) → 结束

系统支持三种用户角色：

| 角色 | 编码 | 权限说明 |
|:---|:---|:---|
| 管理员 | 1 | 创建/编辑/删除活动、提交审核、手动下线、管理 SKU |
| 审核员 | 2 | 审核活动（通过/驳回/不通过/作废） |
| 外部客户 | - | 只读查看活动详情与 SKU 折扣信息 |

### 双状态机联动位图

| 活动 ↓ \ 审核 → | 等待审核(0) | 审核中(1) | 通过(2) | 驳回(3) | 不通过(4) | 作废(5) |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| 草稿(0) | ✓ 编辑/提交/删除 | — | — | ✓ 编辑/提交/删除 | 终态-只读 | 终态-只读 |
| 待生效(2) | — | — | ✓ 只读 | — | — | — |
| 生效中(3) | — | — | ✓ 手动下线 | — | — | — |
| 过时(4) | — | — | 终态-只读 | — | — | — |
| 下线(5) | — | — | 终态-只读 | — | — | — |

---

## 2. 技术栈

### 后端

| 类别 | 技术 | 版本 | 说明 |
|:---|:---|:---|:---|
| 框架 | Spring Boot | 4.0.6 | WebMVC + 定时任务 |
| 语言 | Java | 17 | — |
| ORM | MyBatis | 4.0.1 | 注解 + XML 混合映射 |
| 数据库 | MySQL | 8.0 | 关系型持久化 |
| 安全 | Spring Security Crypto | — | BCrypt 密码加密 |
| JSON | Jackson | — | 序列化/反序列化 |
| 工具 | Lombok | — | 减少样板代码 |
| 测试 | Spring Boot Test + MyBatis Test | — | 单元测试 + 集成测试 |

### 前端

| 类别 | 技术 | 版本 | 说明 |
|:---|:---|:---|:---|
| 框架 | Vue 3 | ^3.4 | Composition API + `<script setup>` |
| 构建工具 | Vite | ^5 | 开发服务器 + 生产构建 |
| 语言 | TypeScript | ^5.3 | 类型安全 |
| 状态管理 | Pinia | ^2.1 | Vue 3 官方推荐 |
| 路由 | Vue Router | ^4.3 | Memory History 模式 |
| HTTP | Axios | ^1.6 | 拦截器 + 统一错误处理 |
| UI 组件库 | Element Plus | ^2.5 | 企业级中后台组件库 |
| CSS | SCSS | ^1.70 | 变量 + 嵌套 + 覆盖 |
| 工具库 | dayjs | ^1.11 | 时间格式化 |

---

## 3. 架构设计

### 3.1 整体架构

```
┌──────────────────────────────────────────────────────┐
│                    前端 (Vue 3 SPA)                    │
│  Views → Components → Pinia Stores → API Services     │
│                       Axios + Vite Proxy               │
├──────────────────────────────────────────────────────┤
│                   HTTP REST API (:8080)                │
├──────────────────────────────────────────────────────┤
│           后端 (Spring Boot + EDA + DDD)               │
│                                                        │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Interfaces │  │ Application  │  │    Domain    │  │
│  │ Controller │→│   Service    │→│   Service    │  │
│  │  (REST)   │  │  (编排/查询)  │  │ (核心业务)   │  │
│  └────────────┘  └──────────────┘  └──────┬───────┘  │
│                                           │           │
│                              ┌────────────┴───────┐   │
│                              │   Event Bus        │   │
│                              │  (事件发布/订阅)    │   │
│                              └────────┬───────────┘   │
│                         ┌────────────┴───────────┐    │
│                         │  State Engines         │    │
│                         │  (活动引擎 + 审核引擎)  │    │
│                         └────────────────────────┘    │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │           Infrastructure (MyBatis / MySQL)        │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

### 3.2 后端分层说明

| 层 | 包路径 | 职责 |
|:---|:---|:---|
| **Interfaces** | `interfaces/controller` | REST 接口，参数校验，DTO 转换 |
| **Application** | `application/service` | 应用编排，事务管理，跨域协调 |
| **Domain** | `domain/*/service` | 核心业务逻辑，状态机流转 |
| **Domain** | `domain/*/entity` | 领域实体（Promotion, AuditRecord, Sku, User） |
| **Domain** | `domain/*/engine` | 状态机引擎（PromotionStateEngine, AuditStateEngine） |
| **Domain** | `domain/engine` | 双状态机联动校验器 |
| **Infrastructure** | `infrastructure/persistence` | MyBatis 映射，TypeHandler |

### 3.3 事件驱动流程

```
用户操作 → Controller → AppService → DomainService
                                         ↓
                                    发布事件 (EventBus)
                                         ↓
                              状态机引擎接收事件
                                         ↓
                          校验合法性 → 更新状态 → 持久化
                                         ↓
                              写入事件日志 (EventLog)
```

### 3.4 前端分层说明

| 层 | 路径 | 职责 |
|:---|:---|:---|
| **Views** | `views/` | 页面视图，组装组件与调用 Store |
| **Components** | `components/` | 可复用 UI 组件（StatusTag, PromotionForm, AuditPanel 等） |
| **Stores** | `stores/` | Pinia 状态管理，封装 API 调用与业务状态 |
| **API** | `api/` | Axios 实例 + 各模块 API 函数 |
| **Composables** | `composables/` | 组合式函数（权限判断、状态映射、分页） |
| **Router** | `router/` | 路由表 + beforeEach 权限守卫 |

---

## 4. 目录结构

```
code/
├── README.md                          # 项目说明（本文件）
├── backed/                            # 后端 Spring Boot 项目
│   ├── pom.xml                        # Maven 配置
│   ├── documents/
│   │   └── requirement.md             # 需求文档
│   └── src/
│       ├── main/
│       │   ├── java/com/sa/promotion/
│       │   │   ├── PromotionApplication.java        # 启动入口
│       │   │   ├── application/
│       │   │   │   ├── dto/request/                 # 请求 DTO
│       │   │   │   ├── dto/response/                # 响应 DTO
│       │   │   │   └── service/                     # 应用服务层
│       │   │   ├── domain/
│       │   │   │   ├── audit/                       # 审核领域
│       │   │   │   │   ├── engine/AuditStateEngine.java
│       │   │   │   │   ├── entity/AuditRecord.java
│       │   │   │   │   ├── enums/AuditStatus.java
│       │   │   │   │   └── service/AuditDomainService.java
│       │   │   │   ├── engine/StateMachineLinkageValidator.java
│       │   │   │   ├── event/                       # 事件领域
│       │   │   │   │   ├── entity/Event.java, EventLog.java
│       │   │   │   │   ├── enums/EventType.java
│       │   │   │   │   └── service/EventBusService.java
│       │   │   │   ├── promotion/                   # 活动领域
│       │   │   │   │   ├── engine/PromotionStateEngine.java
│       │   │   │   │   ├── entity/Promotion.java
│       │   │   │   │   ├── enums/PromotionStatus.java
│       │   │   │   │   └── service/PromotionDomainService.java
│       │   │   │   ├── sku/                         # SKU 领域
│       │   │   │   └── user/                        # 用户领域
│       │   │   ├── infrastructure/persistence/      # MyBatis TypeHandler
│       │   │   └── interfaces/
│       │   │       ├── controller/                  # REST 控制器
│       │   │       └── exception/                   # 全局异常处理
│       │   └── resources/
│       │       ├── application.yml                  # 应用配置
│       │       ├── db/schema.sql                    # 建表 + 测试数据
│       │       └── mapper/                          # MyBatis XML 映射
│       └── test/                                    # 单元测试 + 集成测试
│
├── fronted/                            # 前端 Vue 3 项目
│   ├── package.json                    # 依赖与脚本
│   ├── vite.config.ts                  # Vite 配置 + 代理
│   ├── tsconfig.json                   # TypeScript 配置
│   ├── index.html                      # HTML 入口
│   ├── .env.development                # 开发环境变量
│   ├── document/design.md              # 前端设计文档
│   └── src/
│       ├── main.ts                     # 应用入口
│       ├── App.vue                     # 根组件
│       ├── api/                        # API 服务层
│       │   ├── index.ts                # Axios 实例 + 拦截器
│       │   ├── auth.ts                 # 用户认证
│       │   ├── promotion.ts            # 活动管理
│       │   ├── audit.ts                # 审核流程
│       │   ├── sku.ts                 # SKU 管理
│       │   └── customer.ts             # 客户查询
│       ├── assets/styles/              # 全局样式
│       ├── components/
│       │   ├── common/                 # 通用业务组件
│       │   │   ├── StatusTag.vue       # 状态标签
│       │   │   ├── PromotionForm.vue   # 活动表单
│       │   │   ├── SkuSelector.vue     # SKU 选择器
│       │   │   ├── AuditPanel.vue      # 审核面板
│       │   │   ├── EventTimeline.vue   # 事件时间线
│       │   │   ├── ConfirmDialog.vue   # 确认弹窗
│       │   │   └── EmptyState.vue      # 空状态占位
│       │   ├── customer/               # 客户端组件
│       │   └── layout/                 # 布局组件
│       ├── composables/                # 组合式函数
│       ├── router/index.ts             # 路由配置 + 守卫
│       ├── stores/                     # Pinia 状态管理
│       ├── types/                      # TypeScript 类型定义
│       ├── utils/                      # 工具函数
│       └── views/                      # 页面视图
│           ├── login/                  # 登录页
│           ├── promotion/              # 活动管理（列表/创建/编辑/详情）
│           ├── audit/                  # 审核管理（列表/详情）
│           ├── sku/                   # SKU 管理（列表/创建/编辑）
│           ├── customer/               # 客户查询
│           └── error/                  # 404 / 403
```

---

## 5. 快速开始

### 5.1 环境要求

| 依赖 | 版本要求 |
|:---|:---|
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| npm | 9+ |

### 5.2 数据库初始化

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 创建数据库
CREATE DATABASE IF NOT EXISTS promotion DEFAULT CHARACTER SET utf8mb4;

# 3. 导入建表脚本与测试数据
USE promotion;
SOURCE backed/src/main/resources/db/schema.sql;
```

> 测试账号密码均为 `123456`：管理员 `admin`，审核员 `auditor01`。

### 5.3 启动后端

```bash
cd backed

# 编译并启动
mvn clean spring-boot:run

# 后端运行在 http://localhost:8080
# 健康检查: curl http://localhost:8080/api/promotion/list
```

### 5.4 启动前端

```bash
cd fronted

# 安装依赖（首次）
npm install

# 启动开发服务器
npm run dev

# 前端运行在 http://localhost:5173
# Vite 自动代理 /api → http://localhost:8080
```

### 5.5 生产构建

```bash
# 前端构建
cd fronted && npm run build
# 产物在 fronted/dist/，部署到任意静态服务器或 Nginx

# 后端构建
cd backed && mvn clean package -DskipTests
# 产物在 backed/target/promotion-0.0.1-SNAPSHOT.jar
```

---

## 6. 功能模块

### 6.1 活动管理

| 功能 | 路径 | 权限 | 说明 |
|:---|:---|:---|:---|
| 活动列表 | `/promotion` | 管理员 | 搜索 + 状态筛选 + 分页 + 动态操作按钮 |
| 创建活动 | `/promotion/create` | 管理员 | 填写名称/时间/关联 SKU+折扣 |
| 编辑活动 | `/promotion/:id/edit` | 管理员 | 仅草稿状态可编辑 |
| 活动详情 | `/promotion/:id` | 管理员/审核员 | 信息展示 + SKU 列表 + 事件时间线 + 操作按钮 |
| 提交审核 | — | 管理员 | 草稿提交审核 |
| 手动下线 | — | 管理员 | 强制下线生效中活动 |

### 6.2 审核管理

| 功能 | 路径 | 权限 | 说明 |
|:---|:---|:---|:---|
| 审核列表 | `/audit` | 审核员 | 审核状态筛选 + 分页 |
| 审核详情 | `/audit/:id` | 审核员 | 活动信息 + 审核操作面板 + 审核历史 |
| 审核通过 | — | 审核员 | 活动进入待生效状态 |
| 审核驳回 | — | 审核员 | 活动回到草稿可重新提交 |
| 审核不通过 | — | 审核员 | 活动终止 |
| 审核作废 | — | 审核员 | 活动终止 |

### 6.3 SKU 管理

| 功能 | 路径 | 权限 | 说明 |
|:---|:---|:---|:---|
| SKU 列表 | `/sku` | 管理员 | 搜索 + 分页 + CRUD |
| 创建 SKU | `/sku/create` | 管理员 | 名称 + 原价 |
| 编辑 SKU | `/sku/:id/edit` | 管理员 | 修改名称/原价 |

### 6.4 客户查询

| 功能 | 路径 | 权限 | 说明 |
|:---|:---|:---|:---|
| 活动详情 | `/customer/promotion/:id` | 公开 | 只读查看活动信息与 SKU 折扣 |
| SKU 折扣 | `/customer/sku/:id` | 公开 | 只读查看 SKU 信息 |

### 6.5 定时任务

| 任务 | 说明 |
|:---|:---|
| 自动生效 | 到达 `stime` 时活动自动从待生效 → 生效中 |
| 自动过期 | 到达 `etime` 时活动自动从生效中 → 过时 → 结束 |

---

## 7. API 接口

### 7.1 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 说明 |
|:---|:---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录/token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

### 7.2 接口清单

#### 用户认证

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/user/login` | 用户登录 |
| POST | `/api/user/logout` | 用户登出 |
| POST | `/api/user/register` | 用户注册 |

#### 活动管理

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/promotion/create` | 创建活动草稿 |
| PUT | `/api/promotion/update/{id}` | 更新活动信息 |
| DELETE | `/api/promotion/delete/{id}` | 删除活动 |
| POST | `/api/promotion/submit-audit/{id}` | 提交审核 |
| POST | `/api/promotion/offline/{id}` | 手动下线 |
| GET | `/api/promotion/list` | 活动列表查询 |
| GET | `/api/promotion/{id}` | 活动详情查询 |

#### 审核流程

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/audit/pass/{promotionId}` | 审核通过 |
| POST | `/api/audit/reject/{promotionId}` | 审核驳回 |
| POST | `/api/audit/notpass/{promotionId}` | 审核不通过 |
| POST | `/api/audit/cancel/{promotionId}` | 审核作废 |
| GET | `/api/audit/status/{promotionId}` | 查询审核状态 |

#### SKU 管理

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| POST | `/api/sku/create` | 创建 SKU |
| PUT | `/api/sku/update/{id}` | 更新 SKU |
| DELETE | `/api/sku/delete/{id}` | 删除 SKU |
| GET | `/api/sku/{id}` | 查询 SKU 详情 |
| GET | `/api/sku/list` | 查询 SKU 列表 |

#### 客户查询

| 方法 | 路径 | 说明 |
|:---|:---|:---|
| GET | `/api/customer/promotion/{id}` | 查询活动详情 |
| GET | `/api/customer/sku/{id}` | 查询 SKU 折扣 |
| GET | `/api/customer/sku` | 查询活动所有 SKU |

---

## 8. 测试

### 后端测试

```bash
cd backed
mvn clean test
```

覆盖范围：领域服务、状态机引擎、应用服务、Controller 集成测试。

### 前端测试

```bash
cd fronted

# TypeScript 类型检查
npx vue-tsc --noEmit

# 生产构建验证
npm run build
```

---

## 9. 设计文档

| 文档 | 路径 |
|:---|:---|
| 需求文档 | `backed/documents/requirement.md` |
| 前端设计文档 | `fronted/document/design.md` |
| 数据库建表脚本 | `backed/src/main/resources/db/schema.sql` |
| 应用配置文件 | `backed/src/main/resources/application.yml` |

---

## 10. 开发日志

| 阶段 | 内容 | 状态 |
|:---|:---|:---|
| 阶段 1 | 项目脚手架搭建：Vite + Vue3 + TS + Element Plus + 路由 + Pinia + Axios | ✅ |
| 阶段 2 | 登录/注册 + 权限路由守卫 + Layout 布局 | ✅ |
| 阶段 3 | 活动管理模块（列表/创建/编辑/详情）+ 状态标签组件 | ✅ |
| 阶段 4 | 审核模块（审核列表/审核详情/审核操作面板）+ 事件时间线组件 | ✅ |
| 阶段 5 | SKU 管理模块 + SKU 选择器组件 | ✅ |
| 阶段 6 | 客户查询页（活动详情/SKU 折扣） | ✅ |
| 阶段 7 | 联调测试 + UI 细节打磨 + 代码整理 + 构建验证 | ✅ |
