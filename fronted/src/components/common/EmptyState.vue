<script setup lang="ts">
/**
 * EmptyState —— 空状态占位组件
 *
 * Props:
 *   description - 空状态提示文字
 *   imageSize   - 图标尺寸，默认 80
 *
 * 封装 el-empty，统一项目内空状态样式与间距。
 */
withDefaults(defineProps<{
  description?: string
  imageSize?: number
}>(), {
  description: '暂无数据',
  imageSize: 80,
})
</script>

<template>
  <div class="empty-state">
    <el-empty
      :description="description"
      :image-size="imageSize"
    />
  </div>
</template>

<style scoped>
.empty-state {
  padding: 40px 0;
}
</style>
