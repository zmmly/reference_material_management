<template>
  <div class="change-password-container">
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>

    <div class="change-password-card glass-card">
      <div class="card-header">
        <div class="warning-icon">⚠️</div>
        <h2>首次登录需修改密码</h2>
        <p>为了您的账户安全，请修改初始密码</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleSubmit">
        <el-form-item prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入原密码"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码（6-20位）"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" class="submit-btn">
            {{ loading ? '提交中...' : '确认修改' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        <el-button text @click="handleLogout">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { changePassword } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功')
    userStore.passwordChanged()
    router.push('/dashboard')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.change-password-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg-primary);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;

  &-1 {
    width: 400px;
    height: 400px;
    background: #f59e0b;
    top: -150px;
    right: -100px;
    animation: float 8s ease-in-out infinite;
  }

  &-2 {
    width: 300px;
    height: 300px;
    background: #ef4444;
    bottom: -100px;
    left: -50px;
    animation: float 10s ease-in-out infinite reverse;
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, 20px); }
}

.change-password-card {
  width: 420px;
  padding: 40px;
  position: relative;
  z-index: 1;
  animation: fade-in-up 0.6s ease forwards;
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-header {
  text-align: center;
  margin-bottom: 32px;

  .warning-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: var(--text-muted);
    margin: 0;
  }
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #f59e0b, #d97706) !important;
  border: none !important;
  border-radius: var(--radius-md);
  transition: var(--transition-normal);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(245, 158, 11, 0.4);
  }

  &:active {
    transform: scale(0.98);
  }
}

.card-footer {
  text-align: center;
  margin-top: 16px;
}

:deep(.el-input__wrapper) {
  height: 48px;
  border-radius: var(--radius-md);
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}
</style>
