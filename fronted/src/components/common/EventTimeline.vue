<script setup lang="ts">
/**
 * EventTimeline —— 事件时间线组件
 *
 * Props:
 *   events - 事件列表 (PromotionEvent[])
 *
 * 使用 <el-timeline> 渲染事件流：
 *   - 已完成事件：实心圆点 + 按事件类型着色
 *   - 最新事件（最后一项）：绿色高亮 + 呼吸动画
 *   - 未来事件（可选）：空心圆点 + 灰色
 */
import { computed } from 'vue'
import { EventTypeMap } from '@/utils/enums'
import type { PromotionEvent } from '@/types/event'

// ---- Props ----
const props = defineProps<{
  events: PromotionEvent[]
}>()

// ---- 计算 ----
/** 事件列表（最近的在最上面） */
const reversedEvents = computed(() => [...props.events].reverse())

/** 事件类型 → 时间线颜色 */
function getEventColor(eventType: string): string {
  switch (eventType) {
    case 'E_CREATE_DRAFT':
    case 'E_UPDATE_ACTIVITY':
      return '#409eff' // 蓝色 - 编辑类
    case 'E_SUBMIT_AUDIT':
      return '#e6a23c' // 橙色 - 审核提交
    case 'E_AUDIT_PASS':
      return '#67c23a' // 绿色 - 通过
    case 'E_AUDIT_REJECT':
      return '#f56c6c' // 红色 - 驳回
    case 'E_AUDIT_NOTPASS':
      return '#f56c6c' // 红色 - 不通过
    case 'E_AUDIT_CANCEL':
      return '#909399' // 灰色 - 作废
    case 'E_ACTIVE_ONLINE':
      return '#67c23a' // 绿色 - 上线
    case 'E_ACTIVE_EXPIRE':
      return '#909399' // 灰色 - 过期
    case 'E_MANUAL_OFFLINE':
      return '#e6a23c' // 橙色 - 手动下线
    case 'E_DELETE_ACTIVITY':
      return '#f56c6c' // 红色 - 删除
    default:
      return '#909399'
  }
}

/** 格式化时间 */
function formatTime(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 事件标签 */
function getLabel(eventType: string): string {
  return EventTypeMap[eventType] ?? eventType
}
</script>

<template>
  <div class="event-timeline">
    <el-empty
      v-if="events.length === 0"
      description="暂无事件记录"
      :image-size="80"
    />

    <el-timeline v-else>
      <el-timeline-item
        v-for="(event, index) in reversedEvents"
        :key="event.eventId"
        :color="index === 0 ? '#67c23a' : getEventColor(event.eventType)"
        :hollow="false"
        :timestamp="formatTime(event.eventTime)"
        placement="top"
      >
        <div class="et-item" :class="{ 'et-latest': index === 0 }">
          <span class="et-label">{{ getLabel(event.eventType) }}</span>
          <span v-if="event.operator" class="et-operator">
            操作人: {{ event.operator }}
          </span>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped>
.event-timeline {
  padding: 8px 0;
}

.et-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.et-label {
  font-weight: 500;
  font-size: 14px;
}

.et-latest .et-label {
  color: #67c23a;
  font-weight: 600;
}

.et-operator {
  font-size: 12px;
  color: #909399;
}
</style>
