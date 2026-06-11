<script setup lang="ts">
/**
 * SkuSelector —— SKU 选择器组件
 *
 * Props:
 *   modelValue - 已选 SKU 列表（v-model，PromotionSku[]）
 *
 * 功能:
 * - 搜索 SKU 名称 + 客户端分页
 * - 表格 checkbox 批量勾选 / 逐行添加移除
 * - 已选 SKU 可设置折扣（0.01~1.00）
 * - 通过 v-model 双向绑定已选 SKU 列表
 */
import { ref, computed, reactive, watch, onMounted, nextTick } from 'vue'
import { ElTable } from 'element-plus'
import request from '@/api/index'
import type { ApiResponse } from '@/types/api'
import type { Sku } from '@/types/sku'
import type { PromotionSku } from '@/types/promotion'

// ---- Props & Emits ----
const props = defineProps<{
  modelValue: PromotionSku[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PromotionSku[]]
}>()

// ---- 表格引用 ----
const tableRef = ref<InstanceType<typeof ElTable>>()

// ---- State ----
const allSkus = ref<Sku[]>([])
const loading = ref(false)
const searchName = ref('')
const page = ref(1)
const pageSize = ref(10)

/** 各 SKU 的折扣值 */
const discountInputs = reactive<Record<string, number>>({})

// 外部 modelValue 变化 → 同步折扣值 + 表格勾选状态
watch(
  () => props.modelValue,
  async (val) => {
    for (const item of val) {
      if (!(item.skuId in discountInputs)) {
        discountInputs[item.skuId] = item.discount
      }
    }
    await nextTick()
    syncTableSelection(val)
  },
  { immediate: true }
)

/** 根据 modelValue 同步 el-table 的勾选 */
function syncTableSelection(selected: PromotionSku[]) {
  if (!tableRef.value) return
  const idSet = new Set(selected.map(s => s.skuId))
  allSkus.value.forEach(sku => {
    const shouldCheck = idSet.has(sku.skuId)
    tableRef.value!.toggleRowSelection(sku, shouldCheck)
  })
}

// ---- 筛选 & 分页 ----
const filteredSkus = computed<Sku[]>(() => {
  if (!searchName.value) return allSkus.value
  const kw = searchName.value.toLowerCase()
  return allSkus.value.filter(s => s.skuName.toLowerCase().includes(kw))
})

const pagedSkus = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredSkus.value.slice(start, start + pageSize.value)
})

function onSearch() {
  page.value = 1
}

// ---- 选择 / 取消（统一由 checkbox 驱动） ----
/** el-table selection-change 回调：全量选中行 → 重建 modelValue */
function onSelectionChange(selection: Sku[]) {
  const selectedIds = new Set(selection.map(s => s.skuId))
  const newVal: PromotionSku[] = selection.map(sku => ({
    id: props.modelValue.find(p => p.skuId === sku.skuId)?.id ?? '',
    promotionId: props.modelValue.find(p => p.skuId === sku.skuId)?.promotionId ?? '',
    skuId: sku.skuId,
    discount: discountInputs[sku.skuId] ?? 1.0,
  }))
  // 保留被移除 SKU 的折扣值（不清除 discountInputs）
  emit('update:modelValue', newVal)
}

/** 折扣变更 → 更新 modelValue */
function onDiscountChange(skuId: string, val: number | undefined) {
  if (val === undefined) return
  discountInputs[skuId] = val
  emit('update:modelValue',
    props.modelValue.map(s => (s.skuId === skuId ? { ...s, discount: val } : s))
  )
}

/** 某个 SKU 是否已选 */
function isSelected(skuId: string): boolean {
  return props.modelValue.some(s => s.skuId === skuId)
}

/** 手动勾选某行 */
function selectRow(sku: Sku) {
  tableRef.value?.toggleRowSelection(sku, true)
}

/** 手动取消某行 */
function deselectRow(sku: Sku) {
  tableRef.value?.toggleRowSelection(sku, false)
}

// ---- 数据加载 ----
async function fetchSkus() {
  loading.value = true
  try {
    const res = (await request.get('/api/sku/list')) as ApiResponse<Sku[]>
    allSkus.value = res.data
    // 数据到齐后再同步一次勾选
    await nextTick()
    syncTableSelection(props.modelValue)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchSkus()
})

// ---- 工具 ----
function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`
}
</script>

<template>
  <div class="sku-selector">
    <!-- 搜索栏 -->
    <div class="ss-toolbar">
      <el-input
        v-model="searchName"
        placeholder="搜索 SKU 名称"
        clearable
        style="width: 260px"
        @input="onSearch"
      />
      <span class="ss-hint">已选 {{ modelValue.length }} 个 SKU</span>
    </div>

    <!-- 表格 -->
    <el-table
      ref="tableRef"
      :data="pagedSkus"
      v-loading="loading"
      stripe
      max-height="400"
      style="width: 100%"
      row-key="skuId"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" :reserve-selection="true" />

      <el-table-column prop="skuId" label="SKU ID" width="120" />

      <el-table-column prop="skuName" label="SKU 名称" min-width="180" />

      <el-table-column label="原价" width="110" align="right">
        <template #default="{ row }">
          {{ formatPrice(row.originalPrice) }}
        </template>
      </el-table-column>

      <el-table-column label="折扣" width="170" align="center">
        <template #default="{ row }">
          <el-input-number
            v-if="isSelected(row.skuId)"
            :model-value="discountInputs[row.skuId] ?? 1.0"
            :min="0.01"
            :max="1.0"
            :step="0.01"
            :precision="2"
            size="small"
            style="width: 130px"
            @update:model-value="(v: number | undefined) => onDiscountChange(row.skuId, v)"
          />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button
            v-if="isSelected(row.skuId)"
            size="small"
            type="danger"
            link
            @click="deselectRow(row)"
          >
            移除
          </el-button>
          <el-button
            v-else
            size="small"
            type="primary"
            link
            @click="selectRow(row)"
          >
            添加
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="filteredSkus.length > 0" class="ss-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredSkus.length"
        layout="total, sizes, prev, pager, next"
        background
        small
      />
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && allSkus.length === 0" description="暂无 SKU 数据" />
  </div>
</template>

<style scoped>
.sku-selector {
  padding: 4px 0;
}

.ss-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ss-hint {
  font-size: 13px;
  color: #909399;
}

.ss-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.text-muted {
  color: #c0c4cc;
}
</style>
