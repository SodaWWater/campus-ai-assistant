<template>
  <div>
    <h2>📚 知识库浏览</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8" v-for="kb in knowledgeBases" :key="kb.id">
        <el-card shadow="hover" style="margin-bottom: 16px">
          <template #header>
            <strong>{{ kb.name }}</strong>
            <el-tag size="small" style="float: right" :type="kb.visibility === 'PUBLIC' ? 'success' : 'warning'">{{ kb.visibility === 'PUBLIC' ? '公开' : '课程' }}</el-tag>
          </template>
          <p style="color: #606266">{{ kb.description || '暂无描述' }}</p>
          <div style="color: #909399; font-size: 13px">
            <span>📄 {{ kb.documentCount || 0 }} 文档</span>
            <span style="margin-left: 16px">🧩 {{ kb.chunkCount || 0 }} 片段</span>
            <span style="margin-left: 16px">👤 {{ kb.ownerName || '未知' }}</span>
          </div>
          <div style="margin-top: 12px">
            <el-button type="primary" size="small" @click="goChat(kb.id)">去提问</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="knowledgeBases.length === 0" description="暂无可用的知识库" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentKnowledgeBases } from '../../api/student'

const router = useRouter()
const knowledgeBases = ref([])

onMounted(async () => {
  try {
    const data = await getStudentKnowledgeBases()
    knowledgeBases.value = data || []
  } catch { /* 兼容未实现的后端 */ }
})

function goChat(kbId) {
  router.push({ path: '/student/chat', query: { kbId } })
}
</script>
