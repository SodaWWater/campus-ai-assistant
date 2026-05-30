<template>
  <div>
    <h2>💬 问答预览</h2>
    <p style="color: #909399">以教师视角预览知识库问答效果</p>

    <div style="margin-top: 16px">
      <el-form :inline="true">
        <el-form-item label="知识库">
          <el-select v-model="selectedKb" placeholder="选择知识库" style="width: 300px">
            <el-option v-for="kb in kbs" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div style="max-width: 800px; margin-top: 16px">
      <el-input v-model="question" placeholder="输入测试问题..." size="large" @keyup.enter="ask">
        <template #append>
          <el-button :loading="loading" @click="ask">提问</el-button>
        </template>
      </el-input>

      <el-card v-if="answer" style="margin-top: 16px">
        <template #header><strong>回答</strong></template>
        <div style="white-space: pre-wrap">{{ answer }}</div>
      </el-card>

      <el-card v-if="citations.length" style="margin-top: 12px">
        <template #header><strong>引用来源 ({{ citations.length }})</strong></template>
        <div v-for="(c, i) in citations" :key="i" style="margin-bottom: 8px; padding: 8px; background: #f5f7fa; border-radius: 4px">
          <strong>{{ c.documentTitle }}</strong> · 片段 #{{ c.chunkIndex }} · 得分 {{ c.score }}
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTeacherKnowledgeBases } from '../../api/teacher'
import { askQuestion } from '../../api/chat'

const kbs = ref([])
const selectedKb = ref(null)
const question = ref('')
const answer = ref('')
const citations = ref([])
const loading = ref(false)

onMounted(async () => {
  try { kbs.value = await getTeacherKnowledgeBases() || [] } catch {}
})

async function ask() {
  if (!question.value.trim() || !selectedKb.value) return
  loading.value = true
  try {
    const data = await askQuestion({ knowledgeBaseId: selectedKb.value, question: question.value.trim() })
    answer.value = data.answer
    citations.value = data.matchedChunks || []
  } catch {}
  finally { loading.value = false }
}
</script>
