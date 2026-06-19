<script setup lang="ts">
/**
 * AuditList —— 审核任务列表页
 *
 * 路由: /audit
 * 权限: 审核员
 *
 * 功能:
 * - 复用 promotionStore 列表，按审核状态筛选展示
 * - 表格列：活动ID / 名称 / 审核状态 / 活动状态 / 时间范围
 * - 状态筛选：全部 / 等待审核 / 审核中 / 已驳回 / 已通过 / 终态
 * - 点击行跳转审核详情 /audit/:promotionId
 * - 操作列：根据审核状态动态显示 审核通过/审核驳回/审核作废 按钮
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { usePromotionStore } from '@/stores/promotion'
import { useAuditStore } from '@/stores/audit'
import { AuditStatus, AuditStatusMap } from '@/utils/enums'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { Promotion } from '@/types/promotion'

const router = useRouter()
const promotionStore = usePromotionStore()
const auditStore = useAuditStore()

// ---- 筛选 ----
const filterStatus = ref<string | null>(null)

const statusOptions = Object.entries(AuditStatusMap).map(([code, item]) => ({
  value: code,
  label: item.label,
}))

// ---- 分页 ----
const page = ref(1)
const pageSize = ref(10)

// 筛选后的数据
const filteredList = computed<Promotion[]>(() => {
  let result = promotionStore.list
  if (filterStatus.value !== null) {
    result = result.filter(p => p.auditStatus === filterStatus.value)
  }
  return result
})

const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

function onFilterChange() {
  page.value = 1
}

// ---- 审核操作权限判断（逐行） ----

/** 审核中 → 可通过/驳回 */
function canAudit(p: Promotion): boolean {
  return p.auditStatus === AuditStatus.AUDITING
}

/** 等待审核 或 驳回 → 可作废 */
function canCancel(p: Promotion): boolean {
  return p.auditStatus === AuditStatus.WAITING || p.auditStatus === AuditStatus.REJECTED
}

/** 是否有可用的审核操作 */
function hasAuditAction(p: Promotion): boolean {
  return canAudit(p) || canCancel(p)
}

// ---- 审核操作处理 ----

async function handlePass(id: string, name: string) {
  try {
    const { value: comment } = await ElMessageBox.prompt(
      `确认通过活动「${name}」的审核？`,
      '审核通过',
      {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        inputPlaceholder: '审核意见（选填）',
        inputType: 'textarea',
      },
    )
    await auditStore.pass(id, comment ?? '')
    ElMessage.success('审核已通过')
    promotionStore.fetchList()
  } catch {
    // 用户取消
  }
}

async function handleReject(id: string, name: string) {
  try {
    const { value: comment } = await ElMessageBox.prompt(
      `确认驳回活动「${name}」？驳回后管理员可修改并重新提交。`,
      '审核驳回',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '审核意见（选填）',
        inputType: 'textarea',
      },
    )
    await auditStore.reject(id, comment ?? '')
    ElMessage.success('审核已驳回')
    promotionStore.fetchList()
  } catch {
    // 用户取消
  }
}

async function handleCancel(id: string, name: string) {
  try {
    await ElMessageBox.confirm(
      `确认作废活动「${name}」的审核？此操作为终态，不可恢复。`,
      '审核作废',
      {
        confirmButtonText: '确认作废',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await auditStore.cancel(id, '作废')
    ElMessage.success('审核已作废')
    promotionStore.fetchList()
  } catch {
    // 用户取消
  }
}

// ---- 导航 ----
function handleView(id: string) {
  router.push(`/audit/${id}`)
}

function fmtTime(iso: string): string {
  return iso ? dayjs(iso).format('YYYY-MM-DD HH:mm') : '-'
}

// ---- 初始化 ----
onMounted(() => {
  promotionStore.fetchList()
})
</script>

<template>
  <div class="audit-list">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-select
        v-model="filterStatus"
        placeholder="审核状态筛选"
        clearable
        style="width: 180px"
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

    <!-- 表格 -->
    <el-table
      :data="pagedList"
      v-loading="promotionStore.loading"
      stripe
      style="width: 100%"
      row-class-name="audit-row"
      @row-click="(row: Promotion) => handleView(row.promotionId)"
    >
      <el-table-column prop="promotionId" label="活动ID" width="160">
        <template #default="{ row }">
          <el-tooltip :content="row.promotionId" placement="top">
            <span class="cell-id">{{ row.promotionId }}</span>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="活动名称" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click.stop="handleView(row.promotionId)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column label="审核状态" width="110" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.auditStatus" type="audit" />
        </template>
      </el-table-column>

      <el-table-column label="活动状态" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" type="promotion" />
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

      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <div class="cell-actions" @click.stop>
            <el-button
              v-if="canAudit(row)"
              size="small"
              type="success"
              @click="handlePass(row.promotionId, row.name)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="canAudit(row)"
              size="small"
              type="warning"
              @click="handleReject(row.promotionId, row.name)"
            >
              审核驳回
            </el-button>
            <el-button
              v-if="canCancel(row)"
              size="small"
              type="danger"
              plain
              @click="handleCancel(row.promotionId, row.name)"
            >
              审核作废
            </el-button>
            <el-button
              size="small"
              type="primary"
              link
              @click="handleView(row.promotionId)"
            >
              详情
            </el-button>
            <span
              v-if="!hasAuditAction(row)"
              class="text-muted"
            >
              终态
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
      v-if="!promotionStore.loading && filteredList.length === 0"
      description="暂无审核任务"
    />
  </div>
</template>

<style scoped>
.audit-list {
  padding: 8px 0;
}

.toolbar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 16px;
}

.cell-id {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
  display: inline-block;
  cursor: default;
}

.cell-actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  align-items: center;
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

:deep(.audit-row) {
  cursor: pointer;
}
</style>
