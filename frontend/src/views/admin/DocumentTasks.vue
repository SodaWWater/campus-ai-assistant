<template>
  <div class="task-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Document Tasks</p>
        <h1>文档任务监控</h1>
      </div>
      <el-button @click="loadDocs">刷新</el-button>
    </header>

    <section class="status-grid">
      <article v-for="item in statusCards" :key="item.label" class="status-card">
        <span>{{ item.label }}</span>
        <strong :class="item.className">{{ item.value }}</strong>
      </article>
    </section>

    <section class="toolbar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 180px">
        <el-option label="已完成" value="DONE" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索标题或文件名" clearable />
    </section>

    <el-table :data="filteredDocuments" class="task-table" v-loading="loading" empty-text="暂无文档任务">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="资料标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileName" label="文件名" min-width="190" show-overflow-tooltip />
      <el-table-column label="来源" width="140">
        <template #default="{ row }">{{ sourceName(row.title) }}</template>
      </el-table-column>
      <el-table-column prop="fileType" label="类型" width="90">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.fileType }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="片段" width="90" />
      <el-table-column prop="errorMessage" label="失败原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewChunks(row.id)">片段</el-button>
          <el-button size="small" type="warning" plain @click="reprocess(row.id)" v-if="row.status === 'FAILED'">重试</el-button>
          <el-button size="small" type="danger" plain @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="chunksVisible" title="知识片段" width="760px">
      <div v-for="chunk in chunks" :key="chunk.id" class="chunk-item">
        <div class="chunk-meta">
          <span>片段 #{{ chunk.chunkIndex }}</span>
          <span>{{ chunk.keywords || '无关键词' }}</span>
        </div>
        <p>{{ chunk.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const documents = ref([])
const chunks = ref([])
const chunksVisible = ref(false)
const loading = ref(false)
const statusFilter = ref('')
const keyword = ref('')
let pollTimer = null

const statusCards = computed(() => [
  { label: '全部任务', value: documents.value.length, className: '' },
  { label: '已完成', value: documents.value.filter(d => d.status === 'DONE').length, className: 'done' },
  { label: '处理中', value: documents.value.filter(d => d.status === 'PROCESSING').length, className: 'processing' },
  { label: '失败', value: documents.value.filter(d => d.status === 'FAILED').length, className: 'failed' }
])

const filteredDocuments = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return documents.value.filter(doc => {
    const matchesStatus = !statusFilter.value || doc.status === statusFilter.value
    const matchesText = !text ||
      (doc.title || '').toLowerCase().includes(text) ||
      (doc.fileName || '').toLowerCase().includes(text)
    return matchesStatus && matchesText
  })
})

onMounted(() => loadDocs())
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function loadDocs() {
  loading.value = true
  try {
    documents.value = await http.get('/admin/documents') || []
    startPolling()
  } catch {}
  finally {
    loading.value = false
  }
}

function startPolling() {
  if (pollTimer) clearInterval(pollTimer)
  if (documents.value.some(d => d.status === 'PROCESSING')) {
    pollTimer = setInterval(async () => {
      try {
        documents.value = await http.get('/admin/documents') || []
        if (!documents.value.some(d => d.status === 'PROCESSING')) {
          clearInterval(pollTimer)
          pollTimer = null
        }
      } catch {}
    }, 3000)
  }
}

function statusType(s) {
  if (s === 'DONE') return 'success'
  if (s === 'PROCESSING') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}

function statusText(s) {
  if (s === 'DONE') return '已完成'
  if (s === 'PROCESSING') return '处理中'
  if (s === 'FAILED') return '失败'
  return s || '未知'
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return `${bytes}B`
  return `${(bytes / 1024).toFixed(1)}KB`
}

function sourceName(title = '') {
  if (title.includes('MIT')) return 'MIT OCW'
  if (title.includes('Open') || title.includes('数据结构')) return 'OpenDSA'
  if (title.includes('数据库')) return '智慧高教参考'
  if (title.includes('明华') || title.includes('学习事务')) return '校内资料'
  return '课程资料'
}

async function viewChunks(docId) {
  try {
    chunks.value = await http.get(`/document/${docId}/chunks`) || []
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
    await ElMessageBox.confirm('确定删除该文档及其知识片段？', '删除确认', { type: 'warning' })
    await http.delete(`/admin/document/${docId}`)
    ElMessage.success('文档已删除')
    loadDocs()
  } catch {}
}
</script>

<style scoped>
.task-page {
  display: grid;
  gap: 18px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
}

h1 {
  margin: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.status-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.status-card span,
.status-card strong {
  display: block;
}

.status-card span {
  color: #64748b;
}

.status-card strong {
  margin-top: 8px;
  font-size: 28px;
}

.done {
  color: #16a34a;
}

.processing {
  color: #d97706;
}

.failed {
  color: #dc2626;
}

.toolbar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.task-table {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.chunk-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #f8fafc;
}

.chunk-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.chunk-item p {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>

