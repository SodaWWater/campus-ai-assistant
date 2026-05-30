<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center">
      <h2>📄 文档中心 — {{ kbName }}</h2>
      <el-upload :before-upload="handleUpload" :show-file-list="false" accept=".txt,.md,.pdf,.docx,.doc">
        <el-button type="primary">📤 上传文件 (txt/md/pdf/docx)</el-button>
      </el-upload>
    </div>

    <!-- 知识库选择器（当 kbId 为 0 时显示） -->
    <div v-if="currentKbId === 0" style="margin-top: 16px">
      <el-select v-model="selectedKbId" placeholder="请先选择知识库" @change="onKbChange" style="width: 300px">
        <el-option v-for="kb in kbList" :key="kb.id" :label="kb.name" :value="kb.id" />
      </el-select>
    </div>

    <el-table :data="documents" style="margin-top: 16px" v-loading="loading" empty-text="暂无文档">
      <el-table-column prop="title" label="文档标题" min-width="180" />
      <el-table-column prop="fileName" label="文件名" width="180" />
      <el-table-column prop="fileSize" label="大小" width="80">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="片段数" width="80" />
      <el-table-column prop="processedAt" label="处理时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="viewChunks(row.id)">查看片段</el-button>
          <el-button size="small" type="warning" @click="reprocessDoc(row.id)" v-if="row.status === 'FAILED'">重新解析</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 片段查看弹窗 -->
    <el-dialog v-model="chunksVisible" title="文档片段" width="700px">
      <div v-for="(chunk, i) in chunks" :key="i" style="margin-bottom: 12px; padding: 12px; background: #f5f7fa; border-radius: 8px">
        <p style="color: #909399; font-size: 13px">片段 #{{ chunk.chunkIndex }} | 关键词: {{ chunk.keywords || '无' }}</p>
        <p style="white-space: pre-wrap">{{ chunk.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTeacherKnowledgeBases, getDocuments, uploadDocument, getDocumentChunks, reprocessDocument } from '../../api/teacher'

const props = defineProps({ kbId: { type: [String, Number], default: '0' } })
const currentKbId = computed(() => Number(props.kbId))

const route = useRoute()
const kbList = ref([])
const kbName = ref('')
const selectedKbId = ref(null)
const documents = ref([])
const chunks = ref([])
const chunksVisible = ref(false)
const loading = ref(false)
let pollTimer = null

onMounted(async () => {
  try { kbList.value = await getTeacherKnowledgeBases() || [] } catch {}
  if (currentKbId.value > 0) {
    selectedKbId.value = currentKbId.value
    loadDocs(currentKbId.value)
  }
})

onUnmounted(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
})

watch(() => props.kbId, (val) => {
  if (Number(val) > 0) { selectedKbId.value = Number(val); loadDocs(Number(val)) }
})

function onKbChange(val) {
  const kb = kbList.value.find(k => k.id === val)
  kbName.value = kb?.name || ''
  loadDocs(val)
}

async function loadDocs(kbId) {
  loading.value = true
  const kb = kbList.value.find(k => k.id === kbId)
  kbName.value = kb?.name || ''
  try { documents.value = await getDocuments(kbId) || [] } catch {}
  finally { loading.value = false }
  startPolling()
}

/** 如果有 PROCESSING 状态的文档，每 3 秒自动刷新 */
function startPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  const hasProcessing = documents.value.some(d => d.status === 'PROCESSING')
  if (hasProcessing && selectedKbId.value > 0) {
    pollTimer = setInterval(async () => {
      try {
        documents.value = await getDocuments(selectedKbId.value) || []
        if (!documents.value.some(d => d.status === 'PROCESSING')) {
          // 全部处理完毕，停止轮询
          clearInterval(pollTimer)
          pollTimer = null
        }
      } catch { /* 轮询静默失败 */ }
    }, 3000)
  }
}

function statusType(status) {
  if (status === 'DONE') return 'success'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function formatSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  return (bytes / 1024).toFixed(1) + 'KB'
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
    ElMessage.success('上传成功，后台处理中...')
    loadDocs(selectedKbId.value)  // 立即刷新，触发轮询
  } catch (e) {
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
    ElMessage.success('已提交重新解析，后台处理中...')
    loadDocs(selectedKbId.value)
  } catch {}
}
</script>
