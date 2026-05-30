<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #1f3a5f">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold">
        📖 教师工作台
      </div>
      <el-menu :default-active="activeMenu" background-color="#1f3a5f" text-color="#bfcbd9" active-text-color="#67c23a" router>
        <el-menu-item index="/teacher/dashboard">📊 工作台首页</el-menu-item>
        <el-menu-item index="/teacher/knowledge-bases">📚 知识库管理</el-menu-item>
        <el-menu-item index="/teacher/documents/0">📄 文档中心</el-menu-item>
        <el-menu-item index="/teacher/questions">❓ 问题分析</el-menu-item>
        <el-menu-item index="/teacher/chat-preview">💬 问答预览</el-menu-item>
      </el-menu>
      <div style="position: absolute; bottom: 0; width: 220px; padding: 12px; color: #bfcbd9; border-top: 1px solid #3a5a7a">
        <span>👨‍🏫 {{ nickname }} (教师)</span>
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
