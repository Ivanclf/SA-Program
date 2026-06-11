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
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { usePromotionStore } from '@/stores/promotion'
import { AuditStatusMap } from '@/utils/enums'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { Promotion } from '@/types/promotion'

const router = useRouter()
const promotionStore = usePromotionStore()

// ---- 筛选 ----
const filterStatus = ref<number | null>(null)

const statusOptions = Object.entries(AuditStatusMap).map(([code, item]) => ({
  value: Number(code),
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

      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            type="primary"
            link
            @click.stop="handleView(row.promotionId)"
          >
            审核
          </el-button>
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.audit-row) {
  cursor: pointer;
}
</style>
