<script setup lang="ts">
/**
 * StatusTag —— 状态标签组件
 *
 * Props:
 *   status - 状态码 (string，对应后端枚举名)
 *   type   - 'promotion' | 'audit'
 *
 * 根据 type + status 从枚举映射表取 { label, color }，渲染 <el-tag>
 */
import { computed } from 'vue'
import { PromotionStatusMap, AuditStatusMap } from '@/utils/enums'

const props = defineProps<{
  status: string
  type: 'promotion' | 'audit'
}>()

const map = computed(() =>
  props.type === 'promotion' ? PromotionStatusMap : AuditStatusMap
)

const info = computed(() => map.value[props.status] ?? { label: '未知', color: 'info' })
</script>

<template>
  <el-tag :type="info.color" size="small" disable-transitions>
    {{ info.label }}
  </el-tag>
</template>
