<script setup lang="ts">
/**
 * PromotionList —— 活动列表页
 *
 * 路由: /promotion
 * 权限: 管理员
 *
 * 功能:
 * - 表格展示所有活动（ID / 名称 / 活动状态 / 审核状态 / 时间 / 创建人 / 操作）
 * - 搜索栏：按名称模糊搜索 + 按活动状态下拉筛选
 * - 分页：客户端分页，默认每页 10 条
 * - 动态操作按钮：根据 活动状态 × 审核状态 组合决定可见按钮
 * - 创建按钮 → 跳转 /promotion/create
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { usePromotionStore } from '@/stores/promotion'
import { usePermission } from '@/composables/usePermission'
import { PromotionStatus, AuditStatus, PromotionStatusMap } from '@/utils/enums'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { Promotion } from '@/types/promotion'

const router = useRouter()
const store = usePromotionStore()
const { isAdmin } = usePermission()

// ---- 搜索 & 筛选 ----
const searchName = ref('')
const filterStatus = ref<string | null>(null)

// 活动状态下拉选项（从枚举映射表生成）
const statusOptions = Object.entries(PromotionStatusMap).map(([code, item]) => ({
  value: code,
  label: item.label,
}))

// ---- 客户端分页 ----
const page = ref(1)
const pageSize = ref(10)

// 筛选后的数据
const filteredList = computed<Promotion[]>(() => {
  let result = store.list
  if (searchName.value) {
    const keyword = searchName.value.toLowerCase()
    result = result.filter(p => p.name.toLowerCase().includes(keyword))
  }
  if (filterStatus.value !== null) {
    result = result.filter(p => p.status === filterStatus.value)
  }
  return result
})

// 当前页数据
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// 筛选条件变化时重置到第 1 页
function onFilterChange() {
  page.value = 1
}

// ---- 操作权限判断（逐行） ----

/** 终态判断：活动过期/下线，或审核不通过/作废 */
function isTerminal(p: Promotion): boolean {
  return (
    p.status === PromotionStatus.EXPIRE ||
    p.status === PromotionStatus.OFFLINE ||
    p.auditStatus === AuditStatus.NOT_PASSED ||
    p.auditStatus === AuditStatus.CANCELLED
  )
}

function rowCanEdit(p: Promotion): boolean {
  return !isTerminal(p) && p.status === PromotionStatus.DRAFT
}

function rowCanSubmit(p: Promotion): boolean {
  return (
    p.status === PromotionStatus.DRAFT &&
    [AuditStatus.WAITING, AuditStatus.REJECTED].includes(p.auditStatus)
  )
}

function rowCanDelete(p: Promotion): boolean {
  return !isTerminal(p) && p.status === PromotionStatus.DRAFT
}

function rowCanOffline(p: Promotion): boolean {
  return !isTerminal(p) && p.status === PromotionStatus.ONLINE
}

// ---- 操作处理 ----
function handleCreate() {
  router.push('/promotion/create')
}

function handleView(id: string) {
  router.push(`/promotion/${id}`)
}

function handleEdit(id: string) {
  router.push(`/promotion/${id}/edit`)
}

async function handleSubmit(id: string, name: string) {
  try {
    await ElMessageBox.confirm(
      `确认将「${name}」提交审核？提交后将无法编辑。`,
      '提交审核',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await store.submitAudit(id)
    ElMessage.success('已提交审核')
  } catch {
    // 用户取消
  }
}

async function handleOffline(id: string, name: string) {
  try {
    await ElMessageBox.confirm(
      `确认将「${name}」手动下线？下线后活动立即终止，不可恢复。`,
      '手动下线',
      { confirmButtonText: '确认下线', cancelButtonText: '取消', type: 'warning' }
    )
    await store.offline(id)
    ElMessage.success('已下线')
  } catch {
    // 用户取消
  }
}

async function handleDelete(id: string, name: string) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${name}」？此操作不可恢复。`,
      '删除活动',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await store.deletePromotion(id)
    ElMessage.success('已删除')
  } catch {
    // 用户取消
  }
}

// ---- 工具函数 ----
function fmtTime(iso: string): string {
  return iso ? dayjs(iso).format('YYYY-MM-DD HH:mm') : '-'
}

// ---- 初始化 ----
onMounted(() => {
  store.fetchList()
})
</script>

<template>
  <div class="promotion-list">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchName"
          placeholder="搜索活动名称"
          clearable
          style="width: 240px"
          @input="onFilterChange"
        />
        <el-select
          v-model="filterStatus"
          placeholder="活动状态筛选"
          clearable
          style="width: 160px; margin-left: 12px"
          @change="onFilterChange"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="handleCreate">创建活动</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      :data="pagedList"
      v-loading="store.loading"
      stripe
      style="width: 100%"
      row-class-name="promotion-row"
      @row-click="(row: Promotion) => handleView(row.promotionId)"
    >
      <el-table-column prop="promotionId" label="活动ID" width="140">
        <template #default="{ row }">
          <el-tooltip :content="row.promotionId" placement="top">
            <span class="cell-id">{{ row.promotionId }}</span>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="名称" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click.stop="handleView(row.promotionId)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column label="活动状态" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" type="promotion" />
        </template>
      </el-table-column>

      <el-table-column label="审核状态" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.auditStatus" type="audit" />
        </template>
      </el-table-column>

      <el-table-column label="开始时间" width="150">
        <template #default="{ row }">
          {{ fmtTime(row.stime) }}
        </template>
      </el-table-column>

      <el-table-column label="结束时间" width="150">
        <template #default="{ row }">
          {{ fmtTime(row.etime) }}
        </template>
      </el-table-column>

      <el-table-column prop="creator" label="创建人" width="100" />

      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <div class="cell-actions" @click.stop>
            <el-button
              v-if="rowCanEdit(row)"
              size="small"
              type="primary"
              link
              @click="handleEdit(row.promotionId)"
            >
              编辑
            </el-button>
            <el-button
              v-if="rowCanSubmit(row)"
              size="small"
              type="success"
              link
              @click="handleSubmit(row.promotionId, row.name)"
            >
              提交审核
            </el-button>
            <el-button
              v-if="rowCanOffline(row)"
              size="small"
              type="warning"
              link
              @click="handleOffline(row.promotionId, row.name)"
            >
              手动下线
            </el-button>
            <el-button
              v-if="rowCanDelete(row)"
              size="small"
              type="danger"
              link
              @click="handleDelete(row.promotionId, row.name)"
            >
              删除
            </el-button>
            <span
              v-if="
                !rowCanEdit(row) &&
                !rowCanSubmit(row) &&
                !rowCanOffline(row) &&
                !rowCanDelete(row)
              "
              class="text-muted"
            >
              仅查看
            </span>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="filteredList.length > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredList.length"
        layout="total, sizes, prev, pager, next"
        background
      />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-if="!store.loading && filteredList.length === 0"
      description="暂无活动数据"
    />
  </div>
</template>

<style scoped>
.promotion-list {
  padding: 8px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.cell-id {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
  display: inline-block;
  cursor: default;
}

.cell-actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.promotion-row) {
  cursor: pointer;
}

/* ---- 响应式 ---- */
@media screen and (max-width: 767px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .toolbar-left {
    flex-direction: column;
    gap: 8px;
  }

  .toolbar-left .el-input,
  .toolbar-left .el-select {
    width: 100% !important;
    margin-left: 0 !important;
  }

  .toolbar-right {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
