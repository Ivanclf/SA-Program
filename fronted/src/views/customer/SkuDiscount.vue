<script setup lang="ts">
/**
 * SkuDiscount —— 客户 SKU 详情页
 *
 * 路由: /customer/sku/:id
 * 权限: 公开（无需登录）
 *
 * 展示:
 * - SKU 基本信息（名称、原价）
 * - 折扣随促销活动绑定，具体折扣信息请查看对应活动
 */
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomerSkuApi } from '@/api/customer'
import type { Sku } from '@/types/sku'

const route = useRoute()

const loading = ref(false)
const loadError = ref('')
const sku = ref<Sku | null>(null)

const skuId = route.params.id as string

onMounted(async () => {
  loading.value = true
  try {
    const res = await getCustomerSkuApi(skuId)
    sku.value = res.data
  } catch {
    loadError.value = 'SKU 信息加载失败'
  } finally {
    loading.value = false
  }
})

function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`
}
</script>

<template>
  <div class="customer-sku">
    <!-- 加载中 -->
    <div v-if="loading" class="cs-loading">
      <el-skeleton :rows="4" />
    </div>

    <!-- 加载失败 -->
    <el-result
      v-else-if="loadError"
      icon="error"
      title="加载失败"
      :sub-title="loadError"
    />

    <!-- SKU 信息 -->
    <template v-else-if="sku">
      <el-card shadow="never" class="cs-section">
        <template #header>
          <span class="cs-title">SKU 详情</span>
        </template>

        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="SKU ID">
            {{ sku.skuId }}
          </el-descriptions-item>
          <el-descriptions-item label="SKU 名称">
            {{ sku.skuName }}
          </el-descriptions-item>
          <el-descriptions-item label="原价">
            <span class="cs-price">{{ formatPrice(sku.originalPrice) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="cs-section">
        <template #header>
          <span class="cs-section-title">关于折扣</span>
        </template>
        <p class="cs-note">
          折扣信息与促销活动绑定。请访问具体活动页面查看该 SKU 的实时折扣与折后价格。
        </p>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.customer-sku {
  max-width: 720px;
}

.cs-loading {
  max-width: 720px;
}

.cs-section {
  margin-bottom: 16px;
}

.cs-title {
  font-size: 18px;
  font-weight: 600;
}

.cs-section-title {
  font-weight: 600;
  font-size: 15px;
}

.cs-price {
  font-weight: 600;
  color: var(--el-color-warning);
  font-size: 16px;
}

.cs-note {
  color: #909399;
  line-height: 1.6;
  margin: 0;
}
</style>
