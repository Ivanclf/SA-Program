<script setup lang="ts">
/**
 * PromotionEdit —— 活动编辑页
 *
 * 路径: /promotion/:id/edit
 * 权限: 管理员
 *
 * 组装 PromotionForm (mode='edit')，处理初始数据加载与提交更新：
 *   1. onMounted 时调用 promotionStore.fetchDetail(id) 获取初始数据
 *   2. 提交时调用 promotionStore.update(id, data) 更新活动信息
 *   3. 更新成功后跳转活动详情页
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PromotionForm from '@/components/common/PromotionForm.vue'
import { usePromotionStore } from '@/stores/promotion'
import { useAuthStore } from '@/stores/auth'
import { addSkuToPromotionApi, removeSkuFromPromotionApi } from '@/api/promotion'
import type { PromotionSku } from '@/types/promotion'

const route = useRoute()
const router = useRouter()
const promotionStore = usePromotionStore()
const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')

const promotionId = route.params.id as string

onMounted(async () => {
  loading.value = true
  try {
    await promotionStore.fetchDetail(promotionId)
  } catch {
    loadError.value = '活动数据加载失败'
  } finally {
    loading.value = false
  }
})

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
    await promotionStore.update(promotionId, {
      name: data.name,
      stime: data.stime,
      etime: data.etime,
      operatorId: authStore.user.userId,
    })

    // 同步 SKU 变更：对比新旧 SKU 列表
    const oldSkuIds = new Set(
      (promotionStore.current?.promotionSkus ?? []).map(s => s.skuId)
    )
    const newSkus = data.promotionSkus
    const newSkuIds = new Set(newSkus.map(s => s.skuId))
    const operatorId = authStore.user.userId

    const ops: Promise<any>[] = []
    // 新增的 SKU
    for (const sku of newSkus) {
      if (!oldSkuIds.has(sku.skuId)) {
        ops.push(addSkuToPromotionApi(promotionId, sku.skuId, sku.discount, operatorId))
      }
    }
    // 移除的 SKU
    for (const oldSku of (promotionStore.current?.promotionSkus ?? [])) {
      if (!newSkuIds.has(oldSku.skuId)) {
        ops.push(removeSkuFromPromotionApi(promotionId, oldSku.skuId, operatorId))
      }
    }

    if (ops.length > 0) {
      const results = await Promise.allSettled(ops)
      const failed = results.filter(r => r.status === 'rejected').length
      if (failed > 0) {
        ElMessage.warning(`${ops.length} 个 SKU 变更中 ${failed} 个操作失败`)
      }
    }

    ElMessage.success('活动更新成功')
    router.push(`/promotion/${promotionId}`)
  } catch {
    // 错误已在 store action 中通过 ElMessage 提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="promotion-edit">
    <div class="pe-header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="pe-title">编辑活动</span>
        </template>
      </el-page-header>
    </div>

    <!-- 初始数据加载中 -->
    <div v-if="loading" class="pe-loading">
      <el-skeleton :rows="5" />
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

    <!-- 仅草稿状态可编辑 -->
    <el-result
      v-else-if="promotionStore.current && !promotionStore.canEdit"
      icon="warning"
      title="无法编辑"
      sub-title="仅草稿状态的活动可以编辑"
    >
      <template #extra>
        <el-button type="primary" @click="router.push(`/promotion/${promotionId}`)">
          返回详情
        </el-button>
      </template>
    </el-result>

    <!-- 正常表单 -->
    <PromotionForm
      v-else
      mode="edit"
      :initial-data="promotionStore.current"
      :submitting="submitting"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.promotion-edit {
  padding: 0;
}

.pe-header {
  margin-bottom: 20px;
}

.pe-title {
  font-size: 18px;
  font-weight: 600;
}

.pe-loading {
  max-width: 900px;
}
</style>
