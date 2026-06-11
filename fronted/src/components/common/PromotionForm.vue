<script setup lang="ts">
/**
 * PromotionForm —— 活动创建/编辑表单组件
 *
 * Props:
 *   mode        - 'create' | 'edit'，表单模式
 *   initialData - 编辑模式下的初始活动数据
 *
 * Emits:
 *   submit - 表单验证通过后触发，携带 { name, stime, etime, promotionSkus }
 *
 * 功能:
 * - 活动名称输入（1-100 字符）
 * - 开始/结束时间选择（开始不早于当前，结束晚于开始）
 * - 内嵌 SkuSelector 选择关联 SKU 并设置折扣
 * - Element Plus 表单验证
 */
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import SkuSelector from './SkuSelector.vue'
import type { Promotion, PromotionSku } from '@/types/promotion'

// ---- Props & Emits ----
const props = withDefaults(defineProps<{
  mode: 'create' | 'edit'
  initialData?: Promotion | null
  /** 由父组件控制提交 loading 状态，避免 emit 后立即解除 loading */
  submitting?: boolean
}>(), {
  initialData: null,
  submitting: false,
})

const emit = defineEmits<{
  submit: [data: {
    name: string
    stime: string
    etime: string
    promotionSkus: PromotionSku[]
  }]
}>()

// ---- Form Ref ----
const formRef = ref<FormInstance>()

// ---- Form State ----
const form = reactive({
  name: '',
  stime: '',
  etime: '',
})

/** 关联的 SKU 列表（v-model 绑定 SkuSelector） */
const selectedSkus = ref<PromotionSku[]>([])

// ---- 初始化数据填充（编辑模式） ----
watch(
  () => props.initialData,
  (data) => {
    if (data) {
      form.name = data.name
      form.stime = data.stime ? toDatetimeLocal(data.stime) : ''
      form.etime = data.etime ? toDatetimeLocal(data.etime) : ''
      selectedSkus.value = data.promotionSkus ?? []
    }
  },
  { immediate: true }
)

// ---- 验证规则 ----
const rules = computed<FormRules>(() => {
  const now = new Date()
  const common: FormRules = {
    name: [
      { required: true, message: '请输入活动名称', trigger: 'blur' },
      { min: 1, max: 100, message: '活动名称长度 1-100 字符', trigger: 'blur' },
    ],
    stime: [
      { required: true, message: '请选择开始时间', trigger: 'change' },
    ],
    etime: [
      { required: true, message: '请选择结束时间', trigger: 'change' },
      {
        validator: (_rule, value, callback) => {
          if (value && form.stime && new Date(value) <= new Date(form.stime)) {
            callback(new Error('结束时间必须晚于开始时间'))
          } else {
            callback()
          }
        },
        trigger: 'change',
      },
    ],
  }

  // 创建模式额外校验：开始时间不早于当前时间
  if (props.mode === 'create') {
    common.stime = [
      { required: true, message: '请选择开始时间', trigger: 'change' },
      {
        validator: (_rule, value, callback) => {
          if (value && new Date(value) <= now) {
            callback(new Error('开始时间不能早于当前时间'))
          } else {
            callback()
          }
        },
        trigger: 'change',
      },
    ]
  }

  return common
})

// ---- 提交 ----
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善表单信息')
    return
  }

  // 校验至少选择 1 个 SKU
  if (selectedSkus.value.length === 0) {
    ElMessage.warning('请至少选择一个关联 SKU')
    return
  }

  // 校验 SKU 折扣范围（0.01 ~ 1.00）
  for (const sku of selectedSkus.value) {
    if (sku.discount < 0.01 || sku.discount > 1.0) {
      ElMessage.warning(`SKU 折扣须在 0.01 ~ 1.00 之间，当前值: ${sku.discount}`)
      return
    }
  }

  emit('submit', {
    name: form.name.trim(),
    stime: new Date(form.stime).toISOString(),
    etime: new Date(form.etime).toISOString(),
    promotionSkus: selectedSkus.value,
  })
}

// ---- 工具 ----
/** ISO datetime → datetime-local input 格式 */
function toDatetimeLocal(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 生成 datetime-local 可用的 min 值（当前时间） */
const nowLocal = toDatetimeLocal(new Date().toISOString())
</script>

<template>
  <div class="promotion-form">
    <!-- 基本信息 -->
    <el-card shadow="never" class="pf-section">
      <template #header>
        <span class="pf-section-title">基本信息</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
        size="default"
      >
        <el-form-item label="活动名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入活动名称（1-100 字符）"
            maxlength="100"
            show-word-limit
            clearable
          />
        </el-form-item>

        <el-form-item label="开始时间" prop="stime">
          <el-date-picker
            v-model="form.stime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm"
            :default-time="new Date()"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="etime">
          <el-date-picker
            v-model="form.etime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm"
            :default-time="new Date()"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 关联 SKU -->
    <el-card shadow="never" class="pf-section">
      <template #header>
        <span class="pf-section-title">关联 SKU</span>
      </template>

      <SkuSelector v-model="selectedSkus" />
    </el-card>

    <!-- 操作按钮 -->
    <div class="pf-actions">
      <el-button @click="$router.back()">返回</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        {{ mode === 'create' ? '保存草稿' : '保存修改' }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.promotion-form {
  max-width: 900px;
}

.pf-section {
  margin-bottom: 16px;
}

.pf-section-title {
  font-weight: 600;
  font-size: 15px;
}

.pf-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
</style>
