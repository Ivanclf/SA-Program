<script setup lang="ts">
/**
 * DiscountBadge —— 折扣标签组件
 *
 * Props:
 *   discount - 折扣系数 (0.01 ~ 1.00)
 *
 * 展示:
 *   - < 1.0 → "X折" 醒目标签
 *   - = 1.0 → "原价" 默认标签
 */
import { computed } from 'vue'

const props = defineProps<{
  discount: number
}>()

const label = computed(() => {
  const d = props.discount
  if (d >= 1) return '原价'
  // 转换为中文折扣：0.8 → "8折", 0.75 → "7.5折"
  const zhe = d * 10
  if (zhe % 1 === 0) return `${zhe}折`
  return `${zhe.toFixed(1)}折`
})

const tagType = computed(() => {
  const d = props.discount
  if (d >= 1) return 'info'
  if (d >= 0.8) return 'warning'
  if (d >= 0.5) return 'danger'
  return 'danger'
})
</script>

<template>
  <el-tag :type="tagType" size="default" effect="dark">
    {{ label }}
  </el-tag>
</template>
