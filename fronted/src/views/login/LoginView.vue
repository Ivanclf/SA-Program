<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <h2 class="login-title">促销活动管理系统</h2>

      <!-- 登录表单 -->
      <el-form
        v-if="!showRegister"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="0"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="0"
        size="large"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item prop="role">
          <el-radio-group v-model="registerForm.role">
            <el-radio :value="1">管理员</el-radio>
            <el-radio :value="2">审核员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 切换 -->
      <p class="switch-tip">
        {{ showRegister ? '已有账号？' : '没有账号？' }}
        <el-link type="primary" @click="showRegister = !showRegister">
          {{ showRegister ? '立即登录' : '立即注册' }}
        </el-link>
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'
import type { LoginRequest, RegisterRequest } from '@/types/user'

const router = useRouter()
const authStore = useAuthStore()

// ---- 表单切换 ----
const showRegister = ref(false)
const loading = ref(false)

// ---- 登录表单 ----
const loginFormRef = ref<FormInstance>()
const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名 2-50 字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 50, message: '密码 4-50 字符', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login({ ...loginForm })
    ElMessage.success('登录成功')
    // 根据角色跳转
    if (authStore.isAdmin) {
      router.push('/promotion')
    } else if (authStore.isAuditor) {
      router.push('/audit')
    }
  } catch {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

// ---- 注册表单 ----
const registerFormRef = ref<FormInstance>()
const registerForm = reactive<RegisterRequest>({
  username: '',
  password: '',
  role: 1,
})

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名 2-50 字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 50, message: '密码 4-50 字符', trigger: 'blur' },
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' },
  ],
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.register({ ...registerForm })
    ElMessage.success('注册成功')
    if (authStore.isAdmin) {
      router.push('/promotion')
    } else if (authStore.isAuditor) {
      router.push('/audit')
    }
  } catch {
    ElMessage.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 20px;
  transition: box-shadow 0.3s ease, transform 0.2s ease;
}

.login-card:hover {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
  font-size: 1.5rem;
}

.login-btn {
  width: 100%;
  transition: all 0.25s ease;
}

.switch-tip {
  text-align: center;
  margin-top: 12px;
  color: #999;
  font-size: 0.9rem;
}

/* ---- 响应式 ---- */
@media screen and (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 16px;
  }

  .login-title {
    font-size: 1.25rem;
  }
}
</style>
