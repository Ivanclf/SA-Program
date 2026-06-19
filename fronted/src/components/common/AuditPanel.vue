<script setup lang="ts">
/**
 * AuditPanel —— 审核操作面板
 *
 * Props:
 *   promotionId - 活动 ID
 *   auditStatus - 当前审核状态码
 *
 * Emits:
 *   audited - 审核操作完成，通知父组件刷新
 *
 * 根据 auditStatus 动态渲染可用按钮：
 *   审核中 → 通过 / 驳回 / 不通过
 *   等待审核 / 驳回 → 作废
 *   终态 → 不显示操作面板
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmDialog } from '@/components/common/ConfirmDialog.vue'
import { useAuditStore } from '@/stores/audit'
import { AuditStatus } from '@/utils/enums'

const props = defineProps<{
  promotionId: string
  auditStatus: string
}>()

const emit = defineEmits<{
  audited: []
}>()

const { confirm } = useConfirmDialog()
const auditStore = useAuditStore()

const comment = ref('')
const actionLoading = ref(false)

// ---- 可见性 ----

/** 审核中 → 可通过/驳回/不通过 */
const canPass = computed(() => props.auditStatus === AuditStatus.AUDITING)
const canReject = computed(() => props.auditStatus === AuditStatus.AUDITING)
const canNotPass = computed(() => props.auditStatus === AuditStatus.AUDITING)

/** 等待审核 / 驳回 → 可作废 */
const canCancel = computed(() =>
  props.auditStatus === AuditStatus.WAITING || props.auditStatus === AuditStatus.REJECTED
)

/** 是否显示操作面板 */
const visible = computed(() => canPass.value || canCancel.value)

// ---- 操作 ----

const actionMap: Record<string, { title: string; message: string; confirmText: string; fn: (id: string, c: string) => Promise<any> }> = {
  pass: { title: '审核通过', message: '确认通过该活动的审核？', confirmText: '确认通过', fn: auditStore.pass },
  reject: { title: '审核驳回', message: '确认驳回该活动？驳回后管理员可修改并重新提交。', confirmText: '确认驳回', fn: auditStore.reject },
  notPass: { title: '审核不通过', message: '确认该活动审核不通过？此操作为终态，不可恢复。', confirmText: '确认不通过', fn: auditStore.notPass },
  cancel: { title: '审核作废', message: '确认作废该审核？此操作为终态，不可恢复。', confirmText: '确认作废', fn: auditStore.cancel },
}

async function handleAction(action: string) {
  const cfg = actionMap[action]
  if (!cfg) return

  // 需要审核意见的操作（通过/驳回/不通过）
  const needsComment = ['pass', 'reject', 'notPass'].includes(action)
  if (needsComment && !comment.value.trim()) {
    ElMessage.warning('请填写审核意见')
    return
  }

  try {
    await confirm(cfg.title, cfg.message, cfg.confirmText, ['reject', 'notPass', 'cancel'].includes(action))
  } catch {
    return // 用户取消
  }

  actionLoading.value = true
  try {
    await cfg.fn(props.promotionId, comment.value.trim())
    ElMessage.success(`${cfg.title}操作成功`)
    comment.value = ''
    emit('audited')
  } catch {
    // 错误已在 store action 中通过 ElMessage 提示
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <div v-if="visible" class="audit-panel">
    <div class="ap-title">审核操作</div>

    <!-- 审核意见 -->
    <div class="ap-comment">
      <el-input
        v-model="comment"
        type="textarea"
        :rows="3"
        placeholder="请输入审核意见（通过/驳回/不通过时必填）"
        :disabled="actionLoading"
        maxlength="500"
        show-word-limit
      />
    </div>

    <!-- 操作按钮 -->
    <div class="ap-actions">
      <el-button
        v-if="canPass"
        type="success"
        :loading="actionLoading"
        @click="handleAction('pass')"
      >
        审核通过
      </el-button>
      <el-button
        v-if="canReject"
        type="warning"
        :loading="actionLoading"
        @click="handleAction('reject')"
      >
        审核驳回
      </el-button>
      <el-button
        v-if="canNotPass"
        type="danger"
        :loading="actionLoading"
        @click="handleAction('notPass')"
      >
        审核不通过
      </el-button>
      <el-button
        v-if="canCancel"
        type="info"
        :loading="actionLoading"
        @click="handleAction('cancel')"
      >
        审核作废
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.audit-panel {
  padding: 0;
}

.ap-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 12px;
}

.ap-comment {
  margin-bottom: 16px;
}

.ap-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
