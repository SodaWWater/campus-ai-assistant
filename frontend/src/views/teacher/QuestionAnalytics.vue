<template>
  <div class="analytics-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Question Analytics</p>
        <h1>学生问题分析</h1>
      </div>
      <el-tag effect="plain">最近 100 条记录</el-tag>
    </header>

    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel">
        <div class="panel-title">
          <h2>高频问题</h2>
          <el-tag type="success" effect="plain">教学关注点</el-tag>
        </div>
        <el-empty v-if="topQuestions.length === 0" description="暂无问题记录" />
        <div v-for="item in topQuestions" :key="item.question" class="question-row">
          <span>{{ item.question }}</span>
          <el-tag size="small">{{ item.count }} 次</el-tag>
        </div>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>待补充资料</h2>
          <el-tag type="warning" effect="plain">无引用记录</el-tag>
        </div>
        <el-empty v-if="uncoveredRecords.length === 0" description="暂无无引用问题" />
        <div v-for="record in uncoveredRecords" :key="record.id" class="uncovered-row">
          <strong>{{ record.question }}</strong>
          <span>{{ record.username || '学生' }} · {{ formatTime(record.createdAt) }}</span>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h2>问答记录</h2>
        <el-tag effect="plain">RAG / 学业查询</el-tag>
      </div>
      <el-table :data="records" v-loading="loading" empty-text="暂无问答记录">
        <el-table-column prop="username" label="用户" width="110" />
        <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="answer" label="回答摘要" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ truncate(row.answer, 90) }}</template>
        </el-table-column>
        <el-table-column prop="sourceType" label="类型" width="130">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.sourceType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="引用" width="90">
          <template #default="{ row }">{{ row.matchedChunkIds ? '有' : '无' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="130">
          <template #default="{ row }">{{ totalTime(row) }} ms</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/http'

const loading = ref(false)
const records = ref([])
const topQuestions = ref([])
const summary = ref({
  totalCount: 0,
  ragCount: 0,
  academicCount: 0,
  noCitationCount: 0,
  avgRetrievalTimeMs: 0,
  avgGenerationTimeMs: 0
})

const metrics = computed(() => [
  { label: '问答总数', value: summary.value.totalCount, hint: '最近记录' },
  { label: 'RAG 问答', value: summary.value.ragCount, hint: '命中课程资料' },
  { label: '学业查询', value: summary.value.academicCount, hint: '结构化数据' },
  { label: '无引用问题', value: summary.value.noCitationCount, hint: '可补充资料' }
])

const uncoveredRecords = computed(() => records.value
  .filter(item => !item.matchedChunkIds)
  .slice(0, 6))

onMounted(loadAnalytics)

async function loadAnalytics() {
  loading.value = true
  try {
    const data = await http.get('/teacher/question-analytics')
    records.value = data.records || []
    topQuestions.value = data.topQuestions || []
    summary.value = data
  } catch {}
  finally {
    loading.value = false
  }
}

function truncate(text, len) {
  if (!text) return ''
  return text.length > len ? `${text.substring(0, len)}...` : text
}

function totalTime(row) {
  return (row.retrievalTimeMs || 0) + (row.generationTimeMs || 0)
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.analytics-page {
  display: grid;
  gap: 18px;
}

.page-header,
.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
}

.metric-grid,
.content-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.content-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.metric-card {
  padding: 16px;
}

.metric-card span,
.metric-card small {
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 28px;
}

.panel {
  padding: 18px;
}

.question-row,
.uncovered-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
}

.question-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.uncovered-row {
  display: grid;
}

.uncovered-row span {
  color: #64748b;
  font-size: 13px;
}
</style>

