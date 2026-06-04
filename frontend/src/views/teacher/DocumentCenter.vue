<template>
  <div class="document-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Document Pipeline</p>
        <h1>文档中心<span v-if="kbName"> · {{ kbName }}</span></h1>
      </div>
      <el-upload :before-upload="handleUpload" :show-file-list="false" accept=".txt,.md,.pdf,.docx,.doc">
        <el-button type="primary">上传资料</el-button>
      </el-upload>
    </header>

    <div v-if="currentKbId === 0" class="selector-line">
      <el-select v-model="selectedKbId" placeholder="选择知识库" @change="onKbChange" style="width: 360px">
        <el-option v-for="kb in kbList" :key="kb.id" :label="kb.name" :value="kb.id" />
      </el-select>
    </div>

    <section class="pipeline-summary">
      <div><strong>{{ documents.length }}</strong><span>资料文档</span></div>
      <div><strong>{{ doneCount }}</strong><span>已解析</span></div>
      <div><strong>{{ processingCount }}</strong><span>处理中</span></div>
      <div><strong>{{ failedCount }}</strong><span>失败任务</span></div>
    </section>

    <el-table :data="documents" class="document-table" v-loading="loading" empty-text="暂无文档">
      <el-table-column prop="title" label="资料标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fileType" label="类型" width="90">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.fileType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="来源" width="140">
        <template #default="{ row }">{{ sourceName(row.title) }}</template>
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
      <el-table-column prop="processedAt" label="解析时间" width="180" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewChunks(row.id)">片段</el-button>
          <el-button size="small" type="warning" plain @click="reprocessDoc(row.id)" v-if="row.status === 'FAILED'">重试</el-button>
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getDocumentChunks, getDocuments, getTeacherKnowledgeBases, reprocessDocument, uploadDocument } from '../../api/teacher'

const props = defineProps({ kbId: { type: [String, Number], default: '0' } })
const currentKbId = computed(() => Number(props.kbId))

const kbList = ref([])
const kbName = ref('')
const selectedKbId = ref(null)
const documents = ref([])
const chunks = ref([])
const chunksVisible = ref(false)
const loading = ref(false)
let pollTimer = null

const doneCount = computed(() => documents.value.filter(d => d.status === 'DONE').length)
const processingCount = computed(() => documents.value.filter(d => d.status === 'PROCESSING').length)
const failedCount = computed(() => documents.value.filter(d => d.status === 'FAILED').length)

onMounted(async () => {
  try {
    kbList.value = await getTeacherKnowledgeBases() || []
  } catch {}
  if (currentKbId.value > 0) {
    selectedKbId.value = currentKbId.value
    loadDocs(currentKbId.value)
  }
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

watch(() => props.kbId, val => {
  if (Number(val) > 0) {
    selectedKbId.value = Number(val)
    loadDocs(Number(val))
  }
})

function onKbChange(val) {
  loadDocs(val)
}

async function loadDocs(kbId) {
  loading.value = true
  const kb = kbList.value.find(k => k.id === kbId)
  kbName.value = kb?.name || ''
  try {
    documents.value = await getDocuments(kbId) || []
  } catch {}
  finally {
    loading.value = false
  }
  startPolling()
}

function startPolling() {
  if (pollTimer) clearInterval(pollTimer)
  if (documents.value.some(d => d.status === 'PROCESSING') && selectedKbId.value > 0) {
    pollTimer = setInterval(async () => {
      try {
        documents.value = await getDocuments(selectedKbId.value) || []
        if (!documents.value.some(d => d.status === 'PROCESSING')) {
          clearInterval(pollTimer)
          pollTimer = null
        }
      } catch {}
    }, 3000)
  }
}

function statusType(status) {
  if (status === 'DONE') return 'success'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function statusText(status) {
  if (status === 'DONE') return '已完成'
  if (status === 'PROCESSING') return '处理中'
  if (status === 'FAILED') return '失败'
  return status || '未知'
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
  if (title.includes('事务') || title.includes('明华')) return '校内资料'
  return '课程资料'
}

async function handleUpload(file) {
  if (!selectedKbId.value || selectedKbId.value === 0) {
    ElMessage.warning('请先选择知识库')
    return false
  }
  const formData = new FormData()
  formData.append('file', file)
  try {
    await uploadDocument(selectedKbId.value, formData)
    ElMessage.success('上传成功，后台解析中')
    loadDocs(selectedKbId.value)
  } catch {
    ElMessage.error('上传失败')
  }
  return false
}

async function viewChunks(docId) {
  try {
    chunks.value = await getDocumentChunks(docId) || []
    chunksVisible.value = true
  } catch {}
}

async function reprocessDoc(docId) {
  try {
    await reprocessDocument(docId)
    ElMessage.success('已提交重新解析')
    loadDocs(selectedKbId.value)
  } catch {}
}
</script>

<style scoped>
.document-page {
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

.selector-line {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.pipeline-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.pipeline-summary div {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.pipeline-summary strong,
.pipeline-summary span {
  display: block;
}

.pipeline-summary strong {
  font-size: 26px;
}

.pipeline-summary span {
  margin-top: 4px;
  color: #64748b;
}

.document-table {
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

