<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #1a1a2e">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold">
        ⚙️ 系统管理
      </div>
      <el-menu :default-active="activeMenu" background-color="#1a1a2e" text-color="#bfcbd9" active-text-color="#e6a23c" router>
        <el-menu-item index="/admin/dashboard">📊 系统看板</el-menu-item>
        <el-menu-item index="/admin/users">👥 用户管理</el-menu-item>
        <el-menu-item index="/admin/knowledge-bases">📚 知识库审计</el-menu-item>
        <el-menu-item index="/admin/documents">📄 文档任务</el-menu-item>
        <el-menu-item index="/admin/system">⚙️ 系统设置</el-menu-item>
      </el-menu>
      <div style="position: absolute; bottom: 0; width: 220px; padding: 12px; color: #bfcbd9; border-top: 1px solid #3a3a5e">
        <span>🛡️ {{ nickname }} (管理员)</span>
        <el-button type="danger" size="small" style="margin-left: 8px" @click="logout">退出</el-button>
      </div>
    </el-aside>
    <el-main style="background: #f0f2f5; overflow-y: auto">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const nickname = user.nickname || user.username
const activeMenu = computed(() => route.path)

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>
