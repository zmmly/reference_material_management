<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="login-card glass-card">
      <div class="login-header">
        <div class="login-icon">🎯</div>
        <h2 class="login-title">标准物质管理系统</h2>
        <p class="login-subtitle">Reference Material Management</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" class="login-btn">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await userStore.login(form)
    ElMessage.success('登录成功')
    // 检查是否需要修改密码
    if (res.data.needChangePassword) {
      router.push('/change-password')
    } else {
      router.push('/dashboard')
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
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
    width: 500px;
    height: 500px;
    background: var(--accent-purple);
    top: -200px;
    right: -100px;
    animation: float 8s ease-in-out infinite;
  }

  &-2 {
    width: 400px;
    height: 400px;
    background: var(--accent-cyan);
    bottom: -150px;
    left: -100px;
    animation: float 10s ease-in-out infinite reverse;
  }

  &-3 {
    width: 300px;
    height: 300px;
    background: var(--accent-pink);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation: pulse 6s ease-in-out infinite;
  }
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 30px); }
}

@keyframes pulse {
  0%, 100% { opacity: 0.2; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 0.4; transform: translate(-50%, -50%) scale(1.1); }
}

.login-card {
  width: 420px;
  padding: 48px 40px;
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

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
  background: var(--gradient-purple);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: var(--gradient-purple) !important;
  border: none !important;
  border-radius: var(--radius-md);
  transition: var(--transition-normal);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(139, 92, 246, 0.4);
  }

  &:active {
    transform: scale(0.98);
  }
}

// 表单样式覆盖
:deep(.el-input__wrapper) {
  height: 48px;
  border-radius: var(--radius-md);
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-form-item__error) {
  padding-top: 4px;
}
</style>
