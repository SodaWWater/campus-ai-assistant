<template>
  <div>
    <h2>📊 教师工作台</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6" v-for="m in metrics" :key="m.label">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div :style="{ fontSize: '36px', color: m.color }">{{ m.value }}</div>
            <div style="color: #909399; margin-top: 8px">{{ m.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 20px">
      <template #header><strong>📚 我的知识库</strong></template>
      <el-table :data="knowledgeBases" empty-text="暂无知识库">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="visibility" label="可见性" width="100">
          <template #default="{ row }"><el-tag size="small">{{ row.visibility }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="documentCount" label="文档数" width="80" />
        <el-table-column prop="chunkCount" label="片段数" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTeacherDashboard } from '../../api/teacher'

const knowledgeBases = ref([])
const metrics = ref([
  { label: '我的知识库', value: 0, color: '#409eff' },
  { label: '总文档数', value: 0, color: '#67c23a' },
  { label: '处理中', value: 0, color: '#e6a23c' },
  { label: '失败文档', value: 0, color: '#f56c6c' }
])

onMounted(async () => {
  try {
    const data = await getTeacherDashboard()
    knowledgeBases.value = data.knowledgeBases || []
    metrics.value[0].value = data.kbCount || 0
    metrics.value[1].value = data.docCount || 0
    metrics.value[2].value = data.processingCount || 0
    metrics.value[3].value = data.failedCount || 0
  } catch { /* 兼容 */ }
})
</script>
