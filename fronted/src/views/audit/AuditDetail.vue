<script setup lang="ts">
/**
 * AuditDetail —— 审核详情页
 *
 * 路径: /audit/:id (id = promotionId)
 * 权限: 审核员
 *
 * 展示:
 * - 活动基本信息（只读，同活动详情页信息卡片）
 * - 关联 SKU 列表（只读）
 * - 审核操作面板（AuditPanel 组件，根据审核状态动态渲染）
 * - 事件时间线
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EventTimeline from '@/components/common/EventTimeline.vue'
import AuditPanel from '@/components/common/AuditPanel.vue'
import { usePromotionStore } from '@/stores/promotion'
import { useAuditStore } from '@/stores/audit'
import { getPromotionEventsApi } from '@/api/promotion'
import { useStatusMap } from '@/composables/useStatusMap'
import EmptyState from '@/components/common/EmptyState.vue'
import type { PromotionEvent } from '@/types/event'
import type { PromotionSku } from '@/types/promotion'

// ---- Route & Stores ----
const route = useRoute()
const router = useRouter()
const promotionStore = usePromotionStore()
const auditStore = useAuditStore()
const { getPromotionLabel, getPromotionColor, getAuditLabel, getAuditColor } = useStatusMap()

// ---- State ----
const loading = ref(false)
const loadError = ref('')
const events = ref<PromotionEvent[]>([])
const eventsLoading = ref(false)

const promotionId = route.params.id as string

// ---- 数据加载 ----
onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      promotionStore.fetchDetail(promotionId),
      auditStore.fetchStatus(promotionId),
      fetchEvents(),
    ])
  } catch {
    loadError.value = '审核数据加载失败'
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
    events.value = []
  } finally {
    eventsLoading.value = false
  }
}

/** 审核操作完成后刷新数据 */
async function onAudited() {
  await Promise.all([
    promotionStore.fetchDetail(promotionId),
    auditStore.fetchStatus(promotionId),
    fetchEvents(),
  ])
}

// ---- 计算属性 ----
const promotion = computed(() => promotionStore.current)
const auditRecord = computed(() => auditStore.current)

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
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDiscount(discount: number): string {
  return `${(discount * 100).toFixed(0)}折`
}
</script>

<template>
  <div class="audit-detail">
    <!-- 页头 -->
    <div class="ad-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="ad-title">审核详情</span>
        </template>
      </el-page-header>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="ad-loading">
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
      <el-card shadow="never" class="ad-section">
        <template #header>
          <div class="ad-card-header">
            <span class="ad-section-title">活动信息</span>
            <div class="ad-status-tags">
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

      <!-- 审核记录信息 -->
      <el-card v-if="auditRecord" shadow="never" class="ad-section">
        <template #header>
          <span class="ad-section-title">审核记录</span>
        </template>

        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(auditRecord.submitTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间">
            {{ formatDateTime(auditRecord.completeTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核员">
            {{ auditRecord.auditorId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">
            {{ auditRecord.comment || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 关联 SKU -->
      <el-card shadow="never" class="ad-section">
        <template #header>
          <span class="ad-section-title">关联 SKU（{{ promotion.promotionSkus?.length ?? 0 }}）</span>
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
      <el-card shadow="never" class="ad-section">
        <template #header>
          <span class="ad-section-title">事件时间线</span>
        </template>
        <div v-loading="eventsLoading">
          <EventTimeline :events="events" />
        </div>
      </el-card>

      <!-- 审核操作面板 -->
      <el-card shadow="never" class="ad-section">
        <AuditPanel
          :promotion-id="promotionId"
          :audit-status="promotion.auditStatus"
          @audited="onAudited"
        />
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.audit-detail {
  max-width: 960px;
}

.ad-header {
  margin-bottom: 20px;
}

.ad-title {
  font-size: 18px;
  font-weight: 600;
}

.ad-loading {
  max-width: 960px;
}

.ad-section {
  margin-bottom: 16px;
}

.ad-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ad-section-title {
  font-weight: 600;
  font-size: 15px;
}

.ad-status-tags {
  display: flex;
  gap: 8px;
}
</style>
