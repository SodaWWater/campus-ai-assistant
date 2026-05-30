<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #304156">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold">
        🎓 Campus KB
      </div>
      <el-menu :default-active="activeMenu" background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff" router>
        <el-menu-item index="/student/dashboard">📊 学习首页</el-menu-item>
        <el-menu-item index="/student/knowledge-bases">📚 知识库浏览</el-menu-item>
        <el-menu-item index="/student/chat">💬 AI 问答</el-menu-item>
        <el-menu-item index="/student/academic">📋 学业查询</el-menu-item>
      </el-menu>
      <div style="position: absolute; bottom: 0; width: 220px; padding: 12px; color: #bfcbd9; border-top: 1px solid #4a5a6a">
        <span>🧑‍🎓 {{ nickname }} (学生)</span>
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
