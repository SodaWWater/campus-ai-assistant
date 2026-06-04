<template>
  <div class="chat-workspace">
    <aside class="conversation-panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">AI Workbench</p>
          <h2>智能答疑</h2>
        </div>
        <el-button type="primary" size="small" @click="newChat">新建</el-button>
      </div>

      <el-select v-model="selectedKb" class="kb-select" placeholder="选择课程知识库" clearable>
        <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
      </el-select>

      <div class="conversation-list">
        <button
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conv.id === currentConversationId }"
          @click="switchConv(conv)"
        >
          <span>{{ conv.title }}</span>
          <small>{{ formatTime(conv.updatedAt) }}</small>
        </button>
      </div>
    </aside>

    <main class="dialog-panel">
      <header class="dialog-header">
        <div>
          <p class="eyebrow">Campus Knowledge Hub</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="mode-group">
          <el-tag type="success" effect="plain">溯源回答</el-tag>
          <el-tag effect="plain">{{ currentKbName }}</el-tag>
        </div>
      </header>

      <section ref="msgBox" class="message-stream">
        <div v-if="messages.length === 0" class="empty-state">
          <h3>选择课程资料后开始提问</h3>
          <p>当前知识库引用 MIT OCW、Open Data Structures、OpenStax 与校内课程资料。</p>
          <div class="prompt-grid">
            <button v-for="tip in promptTips" :key="tip" @click="usePrompt(tip)">{{ tip }}</button>
          </div>
        </div>

        <article v-for="(msg, i) in messages" :key="i" class="message-pair">
          <div class="user-message">{{ msg.question }}</div>
          <div class="assistant-message">
            <div v-if="msg.answer" class="answer-text">{{ msg.answer }}</div>
            <div v-else class="answer-loading">正在检索课程资料并组织回答...</div>
            <div v-if="msg.matchedChunks?.length" class="inline-citations">
              <el-tag v-for="chunk in msg.matchedChunks.slice(0, 3)" :key="chunk.id" size="small" effect="plain">
                {{ sourceName(chunk.documentTitle) }} · 片段 {{ chunk.chunkIndex }}
              </el-tag>
            </div>
            <div v-if="msg.answer" class="answer-actions">
              <el-button size="small" text @click="copyAnswer(msg.answer)">复制</el-button>
              <el-button size="small" text @click="retryQuestion(msg.question)">重新提问</el-button>
              <el-button size="small" text :type="msg.feedback === 'up' ? 'success' : ''" @click="markFeedback(msg, 'up')">有帮助</el-button>
              <el-button size="small" text :type="msg.feedback === 'down' ? 'danger' : ''" @click="markFeedback(msg, 'down')">需改进</el-button>
            </div>
          </div>
        </article>
      </section>

      <footer class="composer">
        <el-input
          v-model="input"
          placeholder="输入课程、实验、复习或学业问题"
          size="large"
          @keyup.enter="send"
          :disabled="loading"
        />
        <el-button type="primary" size="large" :loading="loading" @click="send" :disabled="!input.trim()">发送</el-button>
      </footer>
    </main>

    <aside class="source-panel">
      <div class="source-card">
        <p class="eyebrow">Sources</p>
        <h3>引用来源</h3>
        <div v-if="activeSources.length === 0" class="source-empty">回答后会显示命中的课程资料、来源平台和相关片段。</div>
        <div v-for="chunk in activeSources" :key="chunk.id" class="source-item">
          <div class="source-title">
            <strong>{{ chunk.documentTitle || '课程资料' }}</strong>
            <el-tag size="small" :type="licenseType(chunk.documentTitle)">{{ licenseLabel(chunk.documentTitle) }}</el-tag>
          </div>
          <p>{{ chunk.content }}</p>
          <div class="source-meta">
            <span>{{ sourceName(chunk.documentTitle) }}</span>
            <span>Score {{ chunk.score }}</span>
          </div>
        </div>
      </div>

      <div class="source-card">
        <p class="eyebrow">Next</p>
        <h3>推荐追问</h3>
        <button v-for="tip in followUps" :key="tip" class="follow-up" @click="usePrompt(tip)">{{ tip }}</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKnowledgeBases } from '../../api/assistant'
import { askQuestion } from '../../api/chat'
import { createConversation, deleteConversation, getMessages, listConversations } from '../../api/conversation'

const route = useRoute()

const conversations = ref([])
const currentConversationId = ref(null)
const currentTitle = ref('课程知识库问答')
const knowledgeBases = ref([])
const selectedKb = ref(null)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const msgBox = ref(null)

const promptTips = [
  'ArrayList 和 LinkedList 怎么选？',
  'Java 中规格说明有什么作用？',
  '事务 ACID 分别是什么意思？',
  '实验报告迟交会怎么处理？'
]

const followUps = [
  '给我整理成期末复习清单',
  '用一个具体例子解释',
  '哪些地方容易在考试中出错？'
]

const currentKbName = computed(() => {
  const kb = knowledgeBases.value.find(item => item.id === selectedKb.value)
  return kb?.name || '全部知识库'
})

const activeSources = computed(() => {
  const lastWithSources = [...messages.value].reverse().find(msg => msg.matchedChunks?.length)
  return lastWithSources?.matchedChunks || []
})

onMounted(async () => {
  try {
    knowledgeBases.value = await getKnowledgeBases() || []
    selectedKb.value = route.query.kbId ? Number(route.query.kbId) : knowledgeBases.value[0]?.id || null
  } catch {}
  await loadConversations()
  if (route.query.q) input.value = route.query.q
})

async function loadConversations() {
  try {
    conversations.value = await listConversations() || []
  } catch {}
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function newChat() {
  currentConversationId.value = null
  currentTitle.value = '课程知识库问答'
  messages.value = []
}

function switchConv(conv) {
  currentConversationId.value = conv.id
  currentTitle.value = conv.title
  selectedKb.value = conv.knowledgeBaseId || selectedKb.value
  loadMessages(conv.id)
}

async function loadMessages(convId) {
  try {
    const data = await getMessages(convId)
    messages.value = (data || []).filter(item => item.question && item.answer)
    scrollBottom()
  } catch {}
}

async function deleteConv(id) {
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) newChat()
  } catch {}
}

function usePrompt(text) {
  input.value = text
}

async function copyAnswer(answer) {
  try {
    await navigator.clipboard.writeText(answer)
    ElMessage.success('回答已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}

function retryQuestion(question) {
  input.value = question
  send()
}

function markFeedback(msg, value) {
  msg.feedback = value
  ElMessage.success(value === 'up' ? '已标记为有帮助' : '已标记为需改进')
}

async function send() {
  if (!input.value.trim() || loading.value) return
  const q = input.value.trim()
  input.value = ''
  loading.value = true

  messages.value.push({ question: q, answer: '', matchedChunks: [] })
  scrollBottom()

  try {
    const data = await askQuestion({
      conversationId: currentConversationId.value,
      knowledgeBaseId: selectedKb.value,
      question: q
    })

    const last = messages.value[messages.value.length - 1]
    last.answer = data.answer
    last.matchedChunks = data.matchedChunks || []

    if (!currentConversationId.value && data.conversationId) {
      currentConversationId.value = data.conversationId
      currentTitle.value = q.length > 24 ? `${q.substring(0, 24)}...` : q
    }
    await loadConversations()
    scrollBottom()
  } catch (e) {
    const last = messages.value[messages.value.length - 1]
    last.answer = '请求失败，请检查服务状态后重试。'
    ElMessage.error('问答请求失败')
  } finally {
    loading.value = false
  }
}

function scrollBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

function sourceName(title = '') {
  if (title.includes('MIT') || title.includes('软件构造')) return 'MIT OCW'
  if (title.includes('Open') || title.includes('数据结构')) return 'OpenDSA'
  if (title.includes('数据库')) return '智慧高教参考'
  if (title.includes('学习事务') || title.includes('明华')) return '校内资料'
  return '课程资料'
}

function licenseLabel(title = '') {
  if (sourceName(title) === 'MIT OCW') return 'CC'
  if (sourceName(title) === 'OpenDSA') return 'CC BY'
  if (sourceName(title) === '智慧高教参考') return '参考链接'
  return '校内资料'
}

function licenseType(title = '') {
  const source = sourceName(title)
  if (source === 'MIT OCW' || source === 'OpenDSA') return 'success'
  if (source === '智慧高教参考') return 'warning'
  return 'info'
}
</script>

<style scoped>
.chat-workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 340px;
  height: calc(100vh - 80px);
  background: #f4f6f8;
  color: #1f2937;
}

.conversation-panel,
.source-panel {
  background: #fff;
  border-right: 1px solid #e5e7eb;
  padding: 18px;
  overflow: auto;
}

.source-panel {
  border-right: 0;
  border-left: 1px solid #e5e7eb;
}

.panel-header,
.dialog-header,
.source-title,
.source-meta {
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
h2,
h3 {
  margin: 0;
}

.kb-select {
  width: 100%;
  margin: 18px 0;
}

.conversation-list {
  display: grid;
  gap: 8px;
}

.conversation-item,
.prompt-grid button,
.follow-up {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
}

.conversation-item {
  display: grid;
  gap: 5px;
  padding: 11px 12px;
}

.conversation-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-item small {
  color: #94a3b8;
}

.conversation-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.dialog-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
}

.dialog-header,
.composer {
  background: #fff;
  padding: 16px 22px;
  border-bottom: 1px solid #e5e7eb;
}

.mode-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.message-stream {
  overflow: auto;
  padding: 24px;
}

.empty-state {
  max-width: 760px;
  margin: 8vh auto 0;
  text-align: center;
}

.empty-state p {
  color: #64748b;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.prompt-grid button,
.follow-up {
  padding: 12px;
  color: #334155;
}

.message-pair {
  display: grid;
  gap: 12px;
  margin: 0 auto 22px;
  max-width: 880px;
}

.user-message {
  justify-self: end;
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  white-space: pre-wrap;
}

.assistant-message {
  justify-self: start;
  max-width: 78%;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.answer-text {
  line-height: 1.7;
  white-space: pre-wrap;
}

.answer-loading {
  color: #64748b;
}

.inline-citations {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.answer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 12px;
  border-top: 1px solid #eef2f7;
  padding-top: 8px;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  border-top: 1px solid #e5e7eb;
  border-bottom: 0;
}

.source-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
  background: #fff;
}

.source-empty {
  color: #64748b;
  line-height: 1.6;
}

.source-item {
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
  margin-top: 12px;
}

.source-item p {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.source-meta {
  color: #94a3b8;
  font-size: 12px;
}

.follow-up {
  display: block;
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 1180px) {
  .chat-workspace {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .source-panel {
    display: none;
  }
}
</style>
