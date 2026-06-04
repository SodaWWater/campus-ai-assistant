<template>
  <div class="admin-dashboard">
    <header class="page-header">
      <div>
        <p class="eyebrow">Operations</p>
        <h1>平台运营看板</h1>
      </div>
      <el-tag type="success" effect="plain">Campus Knowledge Hub</el-tag>
    </header>

    <section class="metric-grid">
      <article v-for="m in metrics" :key="m.label" class="metric-card">
        <span>{{ m.label }}</span>
        <strong>{{ m.value }}</strong>
        <small>{{ m.hint }}</small>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="panel">
        <div class="panel-title">
          <h2>文档处理任务</h2>
          <el-tag effect="plain">RabbitMQ Pipeline</el-tag>
        </div>
        <div class="status-row">
          <div><strong class="done">{{ docStatus.done }}</strong><span>已完成</span></div>
          <div><strong class="processing">{{ docStatus.processing }}</strong><span>处理中</span></div>
          <div><strong class="failed">{{ docStatus.failed }}</strong><span>失败</span></div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>服务健康</h2>
          <el-tag effect="plain">Runtime</el-tag>
        </div>
        <div class="service-list">
          <div v-for="s in services" :key="s.name">
            <span>{{ s.name }}</span>
            <el-tag :type="s.ok ? 'success' : 'danger'" size="small">{{ s.ok ? '正常' : '异常' }}</el-tag>
          </div>
        </div>
      </article>

      <article class="panel wide">
        <div class="panel-title">
          <h2>开放资料来源</h2>
          <el-tag type="success" effect="plain">RAG Sources</el-tag>
        </div>
        <div class="source-grid">
          <div v-for="source in sources" :key="source.name" class="source-box">
            <strong>{{ source.name }}</strong>
            <span>{{ source.scope }}</span>
            <a :href="source.url" target="_blank" rel="noreferrer">{{ source.license }}</a>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-title">
          <h2>治理关注点</h2>
          <el-tag type="warning" effect="plain">Review</el-tag>
        </div>
        <ul class="watch-list">
          <li>公开知识库需要确认来源与授权说明。</li>
          <li>失败文档应检查文件格式、正文抽取和队列状态。</li>
          <li>高频未命中问题可转交教师补充资料。</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getAdminDashboard, getSystemConfig } from '../../api/admin'

const metrics = ref([
  { label: '用户数', value: 0, hint: '学生、教师、管理员' },
  { label: '知识库', value: 0, hint: '课程与事务资料库' },
  { label: '资料文档', value: 0, hint: '讲义、实验、开放资料' },
  { label: '问答次数', value: 0, hint: 'RAG 与学业查询记录' }
])

const docStatus = ref({ done: 0, processing: 0, failed: 0 })
const services = ref([
  { name: 'MySQL', ok: false },
  { name: 'Redis', ok: false },
  { name: 'RabbitMQ', ok: false },
  { name: 'LLM', ok: false }
])

const sources = [
  { name: 'MIT OCW 6.005', scope: '软件构造、测试、规格说明', license: 'Creative Commons', url: 'https://ocw.mit.edu/courses/6-005-software-construction-spring-2016/' },
  { name: 'Open Data Structures', scope: 'Java 数据结构与算法', license: 'CC BY', url: 'https://opendatastructures.org/' },
  { name: 'OpenStax', scope: '开放教材与学习资料', license: 'CC BY-NC-SA', url: 'https://openstax.org/license/' },
  { name: '智慧高教参考', scope: '中文课程信息链接', license: '课程链接引用', url: 'https://higher.smartedu.cn/courses' }
]

onMounted(async () => {
  try {
    const data = await getAdminDashboard()
    metrics.value[0].value = data.userCount || 0
    metrics.value[1].value = data.kbCount || 0
    metrics.value[2].value = data.docCount || 0
    metrics.value[3].value = data.chatCount || 0
    docStatus.value.done = data.doneCount || 0
    docStatus.value.processing = data.processingCount || 0
    docStatus.value.failed = data.failedCount || 0
  } catch {}

  try {
    const cfg = await getSystemConfig()
    services.value[0].ok = cfg.mysql
    services.value[1].ok = cfg.redis
    services.value[2].ok = cfg.rabbitmq
    services.value[3] = { name: `LLM: ${cfg.llmMode || 'mock'}`, ok: true }
  } catch {}
})
</script>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 20px;
}

.page-header,
.panel-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
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
  font-size: 30px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel {
  padding: 18px;
}

.wide {
  grid-column: span 2;
}

.status-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.status-row div {
  padding: 14px;
  border-radius: 8px;
  background: #f8fafc;
}

.status-row strong,
.status-row span {
  display: block;
}

.status-row strong {
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

.service-list {
  margin-top: 12px;
}

.service-list div {
  display: flex;
  justify-content: space-between;
  padding: 11px 0;
  border-bottom: 1px solid #eef2f7;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.source-box {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.source-box span {
  color: #64748b;
  font-size: 13px;
}

.source-box a {
  color: #2563eb;
  font-size: 13px;
  text-decoration: none;
}

.watch-list {
  margin: 14px 0 0;
  padding-left: 20px;
  color: #475569;
  line-height: 1.9;
}
</style>

