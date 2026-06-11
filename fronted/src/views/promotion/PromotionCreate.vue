<script setup lang="ts">
/**
 * PromotionCreate —— 活动创建页
 *
 * 路径: /promotion/create
 * 权限: 管理员
 *
 * 组装 PromotionForm (mode='create')，处理提交逻辑：
 *   1. 调用 promotionStore.create() 创建活动草稿
 *   2. 创建成功后跳转活动详情页
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PromotionForm from '@/components/common/PromotionForm.vue'
import { usePromotionStore } from '@/stores/promotion'
import { useAuthStore } from '@/stores/auth'
import { addSkuToPromotionApi } from '@/api/promotion'
import type { PromotionSku } from '@/types/promotion'

const router = useRouter()
const promotionStore = usePromotionStore()
const authStore = useAuthStore()

const submitting = ref(false)

async function handleSubmit(data: {
  name: string
  stime: string
  etime: string
  promotionSkus: PromotionSku[]
}) {
  if (!authStore.user?.userId) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }

  submitting.value = true
  try {
    const created = await promotionStore.create({
      name: data.name,
      stime: data.stime,
      etime: data.etime,
      creatorId: authStore.user.userId,
    })

    // 逐个关联 SKU
    if (data.promotionSkus.length > 0) {
      const operatorId = authStore.user.userId
      const results = await Promise.allSettled(
        data.promotionSkus.map(sku =>
          addSkuToPromotionApi(created.promotionId, sku.skuId, sku.discount, operatorId)
        )
      )
      const failed = results.filter(r => r.status === 'rejected').length
      if (failed > 0) {
        ElMessage.warning(`${data.promotionSkus.length} 个 SKU 中 ${failed} 个关联失败`)
      }
    }

    ElMessage.success('活动创建成功')
    router.push(`/promotion/${created.promotionId}`)
  } catch {
    // 错误已在 store action 中通过 ElMessage 提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="promotion-create">
    <div class="pc-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="pc-title">创建活动</span>
        </template>
      </el-page-header>
    </div>

    <PromotionForm
      mode="create"
      :submitting="submitting"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.promotion-create {
  padding: 0;
}

.pc-header {
  margin-bottom: 20px;
}

.pc-title {
  font-size: 18px;
  font-weight: 600;
}
</style>
