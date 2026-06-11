<script setup lang="ts">
/**
 * SkuEdit —— SKU 编辑页
 *
 * 路由: /sku/:id/edit
 * 权限: 管理员
 *
 * 加载已有 SKU 数据，支持修改名称和原价。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useSkuStore } from '@/stores/sku'
import type { UpdateSkuRequest } from '@/types/sku'

const route = useRoute()
const router = useRouter()
const skuStore = useSkuStore()

const skuId = route.params.id as string

const formRef = ref<FormInstance>()
const submitting = ref(false)
const loading = ref(false)
const loadError = ref('')

const form = reactive<UpdateSkuRequest>({
  skuName: '',
  originalPrice: 0,
})

const rules: FormRules = {
  skuName: [
    { required: true, message: '请输入 SKU 名称', trigger: 'blur' },
    { min: 1, max: 100, message: '名称长度 1-100 字符', trigger: 'blur' },
  ],
  originalPrice: [
    { required: true, message: '请输入原价', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value === undefined || value === null || value <= 0) {
          callback(new Error('原价必须大于 0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// ---- 初始化 ----
onMounted(async () => {
  loading.value = true
  try {
    await skuStore.fetchDetail(skuId)
    if (skuStore.current) {
      form.skuName = skuStore.current.skuName
      form.originalPrice = skuStore.current.originalPrice
    } else {
      loadError.value = 'SKU 不存在'
    }
  } catch {
    loadError.value = '加载 SKU 数据失败'
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await skuStore.update(skuId, { ...form })
    ElMessage.success('SKU 更新成功')
    router.push('/sku')
  } catch {
    ElMessage.error('更新失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sku-edit">
    <!-- 页头 -->
    <el-page-header @back="router.push('/sku')">
      <template #content>
        <span class="se-title">编辑 SKU</span>
      </template>
    </el-page-header>

    <!-- 加载中 -->
    <div v-if="loading" class="se-loading">
      <el-skeleton :rows="4" />
    </div>

    <!-- 加载失败 -->
    <el-result
      v-else-if="loadError"
      icon="error"
      title="加载失败"
      :sub-title="loadError"
    >
      <template #extra>
        <el-button type="primary" @click="router.push('/sku')">返回列表</el-button>
      </template>
    </el-result>

    <!-- 表单 -->
    <el-card v-else shadow="never" class="se-form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 520px"
      >
        <el-form-item label="SKU ID">
          <el-input :model-value="skuId" disabled />
        </el-form-item>

        <el-form-item label="SKU 名称" prop="skuName">
          <el-input
            v-model="form.skuName"
            placeholder="请输入 SKU 名称"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="原价" prop="originalPrice">
          <el-input-number
            v-model="form.originalPrice"
            :min="0.01"
            :precision="2"
            :step="1"
            placeholder="请输入原价"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存
          </el-button>
          <el-button @click="router.push('/sku')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.sku-edit {
  max-width: 720px;
}

.se-title {
  font-size: 18px;
  font-weight: 600;
}

.se-loading {
  margin-top: 20px;
}

.se-form-card {
  margin-top: 20px;
}
</style>
