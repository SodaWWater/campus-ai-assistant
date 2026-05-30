<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
    <el-card style="width: 420px; border-radius: 12px">
      <template #header>
        <div style="text-align: center">
          <h1 style="margin: 0; color: #303133">🎓 Campus Knowledge Hub</h1>
          <p style="color: #909399; margin: 8px 0 0">校园知识库智能问答平台</p>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <div style="text-align: center">
        <p style="color: #909399; font-size: 13px; margin-bottom: 8px">演示账号</p>
        <el-space>
          <el-tag type="success" style="cursor: pointer" @click="quickLogin('student', '123456')">student / 123456</el-tag>
          <el-tag type="warning" style="cursor: pointer" @click="quickLogin('teacher', '123456')">teacher / 123456</el-tag>
          <el-tag type="danger" style="cursor: pointer" @click="quickLogin('admin', '123456')">admin / 123456</el-tag>
        </el-space>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function quickLogin(username, password) {
  form.username = username
  form.password = password
  handleLogin()
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login(form.username, form.password)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    ElMessage.success(`欢迎，${data.nickname || data.username}！`)

    // 按角色跳转
    if (data.role === 'STUDENT') router.push('/student/dashboard')
    else if (data.role === 'TEACHER') router.push('/teacher/dashboard')
    else if (data.role === 'ADMIN') router.push('/admin/dashboard')
    else router.push('/student/dashboard')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>
