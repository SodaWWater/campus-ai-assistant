<template>
  <div>
    <h2>📊 学习首页</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div style="font-size: 36px; color: #409eff">{{ dashboard.kbCount || 0 }}</div>
            <div style="color: #909399; margin-top: 8px">可用知识库</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div style="font-size: 36px; color: #67c23a">{{ dashboard.chatCount || 0 }}</div>
            <div style="color: #909399; margin-top: 8px">我的问答次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div style="font-size: 36px; color: #e6a23c">{{ dashboard.docCount || 0 }}</div>
            <div style="color: #909399; margin-top: 8px">资料文档数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><strong>📚 最近知识库</strong></template>
          <el-empty v-if="!recentKbs || recentKbs.length === 0" description="暂无知识库" />
          <div v-for="kb in recentKbs" :key="kb.id" style="padding: 8px 0; border-bottom: 1px solid #ebeef5">
            <router-link :to="'/student/knowledge-bases'">{{ kb.name }}</router-link>
            <span style="float: right; color: #909399; font-size: 13px">{{ kb.documentCount || 0 }} 篇文档</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><strong>💬 快速提问</strong></template>
          <div v-for="q in quickQuestions" :key="q" style="padding: 8px 0; cursor: pointer; color: #409eff" @click="goAsk(q)">{{ q }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentDashboard } from '../../api/student'

const router = useRouter()
const dashboard = ref({})
const recentKbs = ref([])
const quickQuestions = [
  'Java 集合框架有哪些核心接口？',
  'HashMap 的底层原理是什么？',
  '如何复习 Java 基础知识？'
]

onMounted(async () => {
  try {
    const data = await getStudentDashboard()
    dashboard.value = data
    recentKbs.value = data.recentKnowledgeBases || []
  } catch { /* 服务端未实现时使用空数据 */ }
})

function goAsk(q) {
  router.push({ path: '/student/chat', query: { q } })
}
</script>
