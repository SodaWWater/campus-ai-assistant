<template>
  <div>
    <h2>❓ 问题分析</h2>
    <p style="color: #909399">学生问答记录概览，了解常见问题和关注点</p>

    <el-table :data="records" style="margin-top: 16px" v-loading="loading" empty-text="暂无问答记录">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户" width="100" />
      <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="answer" label="回答" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ truncate(row.answer, 80) }}</template>
      </el-table-column>
      <el-table-column prop="sourceType" label="来源" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.sourceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="llmMode" label="模型" width="80" />
      <el-table-column prop="createdAt" label="时间" width="160" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../api/http'

const records = ref([])
const loading = ref(false)

function truncate(text, len) {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}

onMounted(async () => {
  loading.value = true
  try { records.value = await http.get('/chat/history') || [] } catch {}
  finally { loading.value = false }
})
</script>
