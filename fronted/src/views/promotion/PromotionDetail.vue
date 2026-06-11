<script setup lang="ts">
/**
 * PromotionDetail —— 活动详情页
 *
 * 路径: /promotion/:id
 * 权限: 管理员 / 审核员
 *
 * 展示:
 * - 活动基本信息（名称、双状态标签、时间范围、创建人/操作人）
 * - 关联 SKU 列表（名称、原价、折扣、折后价）
 * - 事件时间线（EventTimeline 组件）
 * - 动态操作按钮（基于双状态机矩阵）
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import EventTimeline from '@/components/common/EventTimeline.vue'
import { useConfirmDialog } from '@/components/common/ConfirmDialog.vue'
import { usePromotionStore } from '@/stores/promotion'
import { useAuthStore } from '@/stores/auth'
import { getPromotionEventsApi } from '@/api/promotion'
import { useStatusMap } from '@/composables/useStatusMap'
import EmptyState from '@/components/common/EmptyState.vue'
import type { PromotionEvent } from '@/types/event'
import type { PromotionSku } from '@/types/promotion'

// ---- Route & Stores ----
const route = useRoute()
const router = useRouter()
const promotionStore = usePromotionStore()
const authStore = useAuthStore()
const { getPromotionLabel, getPromotionColor, getAuditLabel, getAuditColor } = useStatusMap()
const { confirm } = useConfirmDialog()

// ---- State ----
const loading = ref(false)
const loadError = ref('')
const events = ref<PromotionEvent[]>([])
const eventsLoading = ref(false)
const actionLoading = ref(false)

const promotionId = route.params.id as string

// ---- 数据加载 ----
onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      promotionStore.fetchDetail(promotionId),
      fetchEvents(),
    ])
  } catch {
    loadError.value = '活动数据加载失败'
  } finally {
    loading.value = false
  }
})

async function fetchEvents() {
  eventsLoading.value = true
  try {
    const res = await getPromotionEventsApi(promotionId)
    events.value = res.data
  } catch {
    // 事件时间线为辅助功能，加载失败不影响主流程
    events.value = []
  } finally {
    eventsLoading.value = false
  }
}

// ---- 计算属性 ----
const promotion = computed(() => promotionStore.current)

const promotionStatusLabel = computed(() =>
  promotion.value ? getPromotionLabel(promotion.value.status) : ''
)
const promotionStatusColor = computed(() =>
  promotion.value ? getPromotionColor(promotion.value.status) : 'info'
)
const auditStatusLabel = computed(() =>
  promotion.value ? getAuditLabel(promotion.value.auditStatus) : ''
)
const auditStatusColor = computed(() =>
  promotion.value ? getAuditColor(promotion.value.auditStatus) : 'info'
)

// ---- 格式化 ----
function formatDateTime(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDiscount(discount: number): string {
  return `${(discount * 100).toFixed(0)}折`
}

// ---- 操作 ----
async function handleSubmitAudit() {
  actionLoading.value = true
  try {
    await promotionStore.submitAudit(promotionId)
    ElMessage.success('已提交审核')
    await fetchEvents()
  } catch {
    // 错误已在 store action 中通过 ElMessage 提示
  } finally {
    actionLoading.value = false
  }
}

async function handleOffline() {
  try {
    await confirm('确认下线', '下线后活动将进入终态，无法恢复，确认操作？', '确认下线', true)
  } catch {
    return // 用户取消
  }

  actionLoading.value = true
  try {
    await promotionStore.offline(promotionId)
    ElMessage.success('活动已下线')
    await fetchEvents()
  } catch {
    // 错误已在 store action 中通过 ElMessage 提示
  } finally {
    actionLoading.value = false
  }
}

async function handleDelete() {
  try {
    await confirm('确认删除', '删除后不可恢复，确认删除该活动？', '确认删除', true)
  } catch {
    return
  }

  actionLoading.value = true
  try {
    await promotionStore.deletePromotion(promotionId)
    ElMessage.success('活动已删除')
    router.push('/promotion')
  } catch {
    // 错误已在 store action 中通过 ElMessage 提示
  } finally {
    actionLoading.value = false
  }
}

// ---- 是否显示操作按钮区 ----
const showActions = computed(() =>
  promotionStore.canEdit ||
  promotionStore.canSubmitAudit ||
  promotionStore.canOffline ||
  promotionStore.canDelete
)
</script>

<template>
  <div class="promotion-detail">
    <!-- 页头 -->
    <div class="pd-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="pd-title">活动详情</span>
        </template>
      </el-page-header>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="pd-loading">
      <el-skeleton :rows="8" />
    </div>

    <!-- 加载失败 -->
    <el-result
      v-else-if="loadError"
      icon="error"
      title="加载失败"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="router.back()">返回</el-button>
      </template>
    </el-result>

    <!-- 主体内容 -->
    <template v-else-if="promotion">
      <!-- 活动信息卡片 -->
      <el-card shadow="never" class="pd-section">
        <template #header>
          <div class="pd-card-header">
            <span class="pd-section-title">活动信息</span>
            <div class="pd-status-tags">
              <el-tag
                :type="promotionStatusColor as any"
                size="default"
                effect="dark"
              >
                活动: {{ promotionStatusLabel }}
              </el-tag>
              <el-tag
                :type="auditStatusColor as any"
                size="default"
                effect="plain"
              >
                审核: {{ auditStatusLabel }}
              </el-tag>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="活动名称" :span="2">
            {{ promotion.name }}
          </el-descriptions-item>
          <el-descriptions-item label="活动 ID">
            {{ promotion.promotionId }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ promotion.creator }}
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ formatDateTime(promotion.stime) }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间">
            {{ formatDateTime(promotion.etime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(promotion.ctime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">
            {{ formatDateTime(promotion.utime) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 关联 SKU -->
      <el-card shadow="never" class="pd-section">
        <template #header>
          <span class="pd-section-title">关联 SKU（{{ promotion.promotionSkus?.length ?? 0 }}）</span>
        </template>

        <EmptyState
          v-if="!promotion.promotionSkus || promotion.promotionSkus.length === 0"
          description="暂无关联 SKU"
          :image-size="80"
        />
        <el-table
          v-else
          :data="promotion.promotionSkus"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="skuId" label="SKU ID" width="200" />
          <el-table-column label="折扣" width="120" align="center">
            <template #default="{ row }: { row: PromotionSku }">
              <el-tag size="small" type="warning" effect="plain">
                {{ formatDiscount(row.discount) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="折扣系数" width="120" align="center">
            <template #default="{ row }: { row: PromotionSku }">
              {{ row.discount.toFixed(2) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 事件时间线 -->
      <el-card shadow="never" class="pd-section">
        <template #header>
          <span class="pd-section-title">事件时间线</span>
        </template>
        <div v-loading="eventsLoading">
          <EventTimeline :events="events" />
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div v-if="showActions" class="pd-actions">
        <el-button
          v-if="promotionStore.canEdit"
          type="primary"
          @click="router.push(`/promotion/${promotionId}/edit`)"
        >
          编辑活动
        </el-button>
        <el-button
          v-if="promotionStore.canSubmitAudit"
          type="success"
          :loading="actionLoading"
          @click="handleSubmitAudit"
        >
          提交审核
        </el-button>
        <el-button
          v-if="promotionStore.canOffline"
          type="warning"
          :loading="actionLoading"
          @click="handleOffline"
        >
          手动下线
        </el-button>
        <el-button
          v-if="promotionStore.canDelete"
          type="danger"
          :loading="actionLoading"
          @click="handleDelete"
        >
          删除活动
        </el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.promotion-detail {
  max-width: 960px;
}

.pd-header {
  margin-bottom: 20px;
}

.pd-title {
  font-size: 18px;
  font-weight: 600;
}

.pd-loading {
  max-width: 960px;
}

.pd-section {
  margin-bottom: 16px;
}

.pd-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pd-section-title {
  font-weight: 600;
  font-size: 15px;
}

.pd-status-tags {
  display: flex;
  gap: 8px;
}

.pd-discount-price {
  color: #f56c6c;
  font-weight: 600;
}

.pd-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 16px 0;
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
}
</style>
