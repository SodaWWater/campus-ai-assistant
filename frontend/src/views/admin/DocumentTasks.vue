<template>
  <div>
    <h2>📄 文档任务</h2>
    <p style="color: #909399">查看所有知识库的文档处理状态，处理失败任务</p>

    <el-table :data="documents" style="margin-top: 16px" v-loading="loading" empty-text="暂无文档">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="fileName" label="文件名" width="180" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="60" />
      <el-table-column prop="fileSize" label="大小" width="80">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="片段数" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewChunks(row.id)">查看片段</el-button>
          <el-button size="small" type="warning" @click="reprocess(row.id)" v-if="row.status === 'FAILED'">重新解析</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 片段弹窗 -->
    <el-dialog v-model="chunksVisible" title="文档片段" width="700px">
      <div v-for="(chunk, i) in chunks" :key="i" style="margin-bottom: 12px; padding: 12px; background: #f5f7fa; border-radius: 8px">
        <p style="color: #909399; font-size: 13px">片段 #{{ chunk.chunkIndex }} | 关键词: {{ chunk.keywords || '无' }}</p>
        <p style="white-space: pre-wrap">{{ chunk.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const documents = ref([])
const chunks = ref([])
const chunksVisible = ref(false)
const loading = ref(false)
let pollTimer = null

onMounted(() => loadDocs())
onUnmounted(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
})

async function loadDocs() {
  loading.value = true
  try {
    const data = await http.get('/admin/documents')
    documents.value = data || []
    startPolling()
  } catch {}
  finally { loading.value = false }
}

/** 如果有 PROCESSING 状态的文档，每 3 秒自动刷新 */
function startPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  const hasProcessing = documents.value.some(d => d.status === 'PROCESSING')
  if (hasProcessing) {
    pollTimer = setInterval(async () => {
      try {
        const data = await http.get('/admin/documents')
        documents.value = data || []
        if (!documents.value.some(d => d.status === 'PROCESSING')) {
          clearInterval(pollTimer)
          pollTimer = null
        }
      } catch { /* 轮询静默失败 */ }
    }, 3000)
  }
}

function statusType(s) {
  if (s === 'DONE') return 'success'
  if (s === 'PROCESSING') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  return (bytes / 1024).toFixed(1) + 'KB'
}

async function viewChunks(docId) {
  try {
    const data = await http.get(`/document/${docId}/chunks`)
    chunks.value = data || []
    chunksVisible.value = true
  } catch {}
}

async function reprocess(docId) {
  try {
    await http.post(`/admin/document/${docId}/reprocess`)
    ElMessage.success('已提交重新解析')
    loadDocs()
  } catch {}
}

async function remove(docId) {
  try {
    await ElMessageBox.confirm('确定删除该文档？', '警告', { type: 'warning' })
    await http.delete(`/admin/document/${docId}`)
    ElMessage.success('已删除')
    loadDocs()
  } catch {}
}
</script>
