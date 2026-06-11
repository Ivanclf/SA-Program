<script setup lang="ts">
/**
 * PromotionView —— 客户活动详情页
 *
 * 路由: /customer/promotion/:id
 * 权限: 公开（无需登录）
 *
 * 展示:
 * - 活动基本信息（名称、时间、状态）
 * - 关联 SKU 列表（含原价、折扣、折后价）
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getCustomerPromotionApi,
  getCustomerSkuListApi,
} from '@/api/customer'
import { useStatusMap } from '@/composables/useStatusMap'
import DiscountBadge from '@/components/customer/DiscountBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { Promotion, PromotionSku } from '@/types/promotion'
import type { Sku } from '@/types/sku'

const route = useRoute()
const { getPromotionLabel, getPromotionColor } = useStatusMap()

const loading = ref(false)
const loadError = ref('')
const promotion = ref<Promotion | null>(null)
const skuMap = ref<Map<string, Sku>>(new Map())

const promotionId = route.params.id as string

onMounted(async () => {
  loading.value = true
  try {
    const [promoRes, skusRes] = await Promise.all([
      getCustomerPromotionApi(promotionId),
      getCustomerSkuListApi(),
    ])
    promotion.value = promoRes.data

    // 构建 SKU ID → SKU 映射
    const map = new Map<string, Sku>()
    for (const sku of skusRes.data) {
      map.set(sku.skuId, sku)
    }
    skuMap.value = map
  } catch {
    loadError.value = '活动信息加载失败'
  } finally {
    loading.value = false
  }
})

/** 带 SKU 名称和原价的增强型列表 */
const enrichedSkus = computed(() => {
  if (!promotion.value) return []
  return promotion.value.promotionSkus.map((ps: PromotionSku) => {
    const sku = skuMap.value.get(ps.skuId)
    return {
      ...ps,
      skuName: sku?.skuName ?? '未知 SKU',
      originalPrice: sku?.originalPrice ?? 0,
      finalPrice: sku ? (sku.originalPrice * ps.discount).toFixed(2) : '0.00',
    }
  })
})

const promotionStatusLabel = computed(() =>
  promotion.value ? getPromotionLabel(promotion.value.status) : ''
)
const promotionStatusColor = computed(() =>
  promotion.value ? getPromotionColor(promotion.value.status) : 'info'
)

function formatDateTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`
}

function formatDiscount(discount: number): string {
  return discount.toFixed(2)
}
</script>

<template>
  <div class="customer-promotion">
    <!-- 加载中 -->
    <div v-if="loading" class="cp-loading">
      <el-skeleton :rows="6" />
    </div>

    <!-- 加载失败 -->
    <el-result
      v-else-if="loadError"
      icon="error"
      title="加载失败"
      :sub-title="loadError"
    />

    <!-- 主体内容 -->
    <template v-else-if="promotion">
      <!-- 活动信息卡片 -->
      <el-card shadow="never" class="cp-section">
        <template #header>
          <div class="cp-card-header">
            <span class="cp-title">{{ promotion.name }}</span>
            <el-tag
              :type="promotionStatusColor as any"
              size="default"
              effect="dark"
            >
              {{ promotionStatusLabel }}
            </el-tag>
          </div>
        </template>

        <el-descriptions :column="2" border size="default">
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
        </el-descriptions>
      </el-card>

      <!-- SKU 折扣列表 -->
      <el-card shadow="never" class="cp-section">
        <template #header>
          <span class="cp-section-title">
            参与 SKU（{{ enrichedSkus.length }}）
          </span>
        </template>

        <EmptyState
          v-if="enrichedSkus.length === 0"
          description="暂无 SKU 参与此活动"
          :image-size="80"
        />

        <el-table
          v-else
          :data="enrichedSkus"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="skuName" label="SKU 名称" min-width="200" />
          <el-table-column label="原价" width="130" align="center">
            <template #default="{ row }">
              {{ formatPrice(row.originalPrice) }}
            </template>
          </el-table-column>
          <el-table-column label="折扣" width="100" align="center">
            <template #default="{ row }">
              <DiscountBadge :discount="row.discount" />
            </template>
          </el-table-column>
          <el-table-column label="折后价" width="130" align="center">
            <template #default="{ row }">
              <span class="cp-final-price">{{ formatPrice(+row.finalPrice) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.customer-promotion {
  max-width: 960px;
}

.cp-loading {
  max-width: 960px;
}

.cp-section {
  margin-bottom: 16px;
}

.cp-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cp-title {
  font-size: 18px;
  font-weight: 600;
}

.cp-section-title {
  font-weight: 600;
  font-size: 15px;
}

.cp-final-price {
  font-weight: 600;
  color: var(--el-color-danger);
  font-size: 15px;
}
</style>
