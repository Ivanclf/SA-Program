<script setup lang="ts">
/**
 * SkuCreate —— SKU 创建页
 *
 * 路由: /sku/create
 * 权限: 管理员
 *
 * 表单字段: SKU 名称、原价
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useSkuStore } from '@/stores/sku'
import type { CreateSkuRequest } from '@/types/sku'

const router = useRouter()
const skuStore = useSkuStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<CreateSkuRequest>({
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

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await skuStore.create({ ...form })
    ElMessage.success('SKU 创建成功')
    router.push('/sku')
  } catch {
    ElMessage.error('创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="sku-create">
    <!-- 页头 -->
    <el-page-header @back="router.push('/sku')">
      <template #content>
        <span class="sc-title">创建 SKU</span>
      </template>
    </el-page-header>

    <!-- 表单 -->
    <el-card shadow="never" class="sc-form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 520px"
      >
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
.sku-create {
  max-width: 720px;
}

.sc-title {
  font-size: 18px;
  font-weight: 600;
}

.sc-form-card {
  margin-top: 20px;
}
</style>
