<script lang="ts">
/**
 * ConfirmDialog —— 确认弹窗 composable
 *
 * 使用方式:
 *   import { useConfirmDialog } from '@/components/common/ConfirmDialog.vue'
 *   const { confirm } = useConfirmDialog()
 *   await confirm('确认删除', '此操作不可撤销', '删除', true)
 *
 * 封装 ElMessageBox.confirm，统一确认交互样式。
 */
import { ElMessageBox } from 'element-plus'
import type { MessageBoxState } from 'element-plus'

export function useConfirmDialog() {
  function confirm(
    title: string,
    message: string,
    confirmText: string = '确定',
    danger: boolean = false,
  ): Promise<MessageBoxState> {
    return ElMessageBox.confirm(message, title, {
      confirmButtonText: confirmText,
      cancelButtonText: '取消',
      type: danger ? 'warning' : 'info',
      confirmButtonClass: danger ? 'el-button--danger' : '',
      buttonSize: 'default',
    })
  }

  return { confirm }
}
</script>

<script setup lang="ts">
// ConfirmDialog 为纯逻辑 composable，无组件自身逻辑
</script>

<template>
  <!-- 纯逻辑 composable，无渲染内容 -->
  <div />
</template>
