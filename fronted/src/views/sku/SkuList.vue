<script setup lang="ts">
/**
 * SkuList —— SKU 列表页
 *
 * 路由: /sku
 * 权限: 管理员
 *
 * 功能:
 * - 表格展示所有 SKU（ID / 名称 / 原价）
 * - 前端搜索（按名称模糊匹配）
 * - 分页
 * - 创建 / 编辑 / 删除操作
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { useSkuStore } from '@/stores/sku'
import { useConfirmDialog } from '@/components/common/ConfirmDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import type { Sku } from '@/types/sku'

const router = useRouter()
const skuStore = useSkuStore()
const { confirm } = useConfirmDialog()

// ---- 搜索 ----
const searchText = ref('')

// ---- 分页 ----
const page = ref(1)
const pageSize = ref(10)

// 筛选后的数据
const filteredList = computed<Sku[]>(() => {
  if (!searchText.value.trim()) return skuStore.list
  const keyword = searchText.value.trim().toLowerCase()
  return skuStore.list.filter(
    s => s.skuName.toLowerCase().includes(keyword) || s.skuId.toLowerCase().includes(keyword)
  )
})

const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

function onSearch() {
  page.value = 1
}

// ---- 导航 ----
function handleCreate() {
  router.push('/sku/create')
}

function handleEdit(id: string) {
  router.push(`/sku/${id}/edit`)
}

// ---- 删除 ----
async function handleDelete(row: Sku) {
  try {
    await confirm('删除 SKU', `确认删除 "${row.skuName}"？删除后不可恢复。`, '删除', true)
  } catch {
    return // 用户取消
  }

  try {
    await skuStore.deleteSku(row.skuId)
    ElMessage.success('删除成功')
    // 如果当前页无数据，回到上一页
    if (pagedList.value.length === 0 && page.value > 1) {
      page.value--
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

function formatPrice(price: number): string {
  return `¥${price.toFixed(2)}`
}

// ---- 初始化 ----
onMounted(() => {
  skuStore.fetchList()
})
</script>

<template>
  <div class="sku-list">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="searchText"
        placeholder="搜索 SKU 名称或 ID"
        clearable
        style="width: 280px"
        @input="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        创建 SKU
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="pagedList"
      v-loading="skuStore.loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="skuId" label="SKU ID" width="200">
        <template #default="{ row }">
          <el-tooltip :content="row.skuId" placement="top">
            <span class="cell-id">{{ row.skuId }}</span>
          </el-tooltip>
        </template>
      </el-table-column>

      <el-table-column prop="skuName" label="SKU 名称" min-width="200" />

      <el-table-column label="原价" width="150" align="center">
        <template #default="{ row }">
          <span class="cell-price">{{ formatPrice(row.originalPrice) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="handleEdit(row.skuId)">
            编辑
          </el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div v-if="filteredList.length > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredList.length"
        layout="total, sizes, prev, pager, next"
        background
      />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-if="!skuStore.loading && filteredList.length === 0"
      :description="searchText ? '未找到匹配的 SKU' : '暂无 SKU 数据，点击「创建 SKU」添加'"
    />
  </div>
</template>

<style scoped>
.sku-list {
  padding: 8px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.cell-id {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
  display: inline-block;
}

.cell-price {
  font-weight: 500;
  color: var(--el-color-warning);
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ---- 响应式 ---- */
@media screen and (max-width: 767px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .toolbar .el-input {
    width: 100% !important;
  }
}
</style>
