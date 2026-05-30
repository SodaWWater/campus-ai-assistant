<template>
  <div style="display: flex; height: calc(100vh - 80px); gap: 0">
    <!-- 左侧对话列表 -->
    <div style="width: 260px; border-right: 1px solid #e4e7ed; background: #fff; display: flex; flex-direction: column">
      <div style="padding: 12px">
        <el-button type="primary" style="width: 100%" @click="newChat">＋ 新对话</el-button>
      </div>
      <div style="flex: 1; overflow-y: auto; padding: 0 8px">
        <div v-for="conv in conversations" :key="conv.id"
             :style="{ padding: '10px 12px', cursor: 'pointer', borderRadius: '8px', marginBottom: '4px',
                       background: conv.id === currentConversationId ? '#e8f4ff' : 'transparent' }"
             @click="switchConv(conv)">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1">
              {{ conv.title }}
            </span>
            <el-button size="small" text type="danger" @click.stop="deleteConv(conv.id)">✕</el-button>
          </div>
          <div style="font-size: 11px; color: #909399; margin-top: 2px">{{ formatTime(conv.updatedAt) }}</div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div style="flex: 1; display: flex; flex-direction: column; background: #f5f7fa">
      <!-- 顶部工具栏 -->
      <div style="padding: 12px 16px; background: #fff; border-bottom: 1px solid #e4e7ed; display: flex; align-items: center; gap: 12px">
        <span style="font-weight: bold">{{ currentTitle }}</span>
        <el-select v-model="selectedKb" placeholder="选择知识库(可选)" size="small" style="width: 240px" clearable>
          <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
      </div>

      <!-- 消息区域 -->
      <div ref="msgBox" style="flex: 1; overflow-y: auto; padding: 20px">
        <el-empty v-if="messages.length === 0" description="开始新对话" />
        <div v-for="(msg, i) in messages" :key="i" style="margin-bottom: 20px">
          <!-- 用户消息 -->
          <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
            <div style="max-width: 70%; background: #409eff; color: #fff; padding: 10px 16px; border-radius: 12px 12px 4px 12px; font-size: 14px; white-space: pre-wrap">{{ msg.question }}</div>
          </div>
          <!-- 助手消息 -->
          <div style="display: flex; justify-content: flex-start">
            <div style="max-width: 70%">
              <div style="background: #fff; padding: 10px 16px; border-radius: 12px 12px 12px 4px; font-size: 14px; white-space: pre-wrap; line-height: 1.6; box-shadow: 0 1px 3px rgba(0,0,0,0.08)">{{ msg.answer }}</div>

              <!-- RAG 引用（如果有） -->
              <div v-if="msg.matchedChunks && msg.matchedChunks.length > 0" style="margin-top: 6px">
                <el-collapse>
                  <el-collapse-item title="📎 参考资料 ({{ msg.matchedChunks.length }} 条)">
                    <div v-for="(c, j) in msg.matchedChunks" :key="j" style="margin-bottom: 6px; padding: 6px 8px; background: #fff; border-radius: 4px; font-size: 12px; color: #606266">
                      <strong>{{ c.documentTitle }}</strong> · 片段#{{ c.chunkIndex }} · 得分 {{ c.score }}
                      <p style="margin: 4px 0 0; white-space: pre-wrap">{{ c.content?.substring(0, 200) }}{{ c.content?.length > 200 ? '...' : '' }}</p>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>
        </div>
        <div v-if="loading" style="text-align: center; padding: 20px; color: #909399">AI 正在思考...</div>
      </div>

      <!-- 输入区域 -->
      <div style="padding: 16px; background: #fff; border-top: 1px solid #e4e7ed">
        <div style="display: flex; gap: 12px">
          <el-input v-model="input" placeholder="输入问题..." size="large" @keyup.enter="send" :disabled="loading" />
          <el-button type="primary" size="large" :loading="loading" @click="send" :disabled="!input.trim()">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKnowledgeBases } from '../../api/assistant'
import { askQuestion } from '../../api/chat'

const route = useRoute()
import { listConversations, createConversation, deleteConversation, getMessages } from '../../api/conversation'

const conversations = ref([])
const currentConversationId = ref(null)
const currentTitle = ref('AI 问答')
const knowledgeBases = ref([])
const selectedKb = ref(null)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const msgBox = ref(null)

onMounted(async () => {
  try { knowledgeBases.value = await getKnowledgeBases() || [] } catch {}
  await loadConversations()
  // 从快速提问跳转时预填问题
  if (route.query.q) {
    input.value = route.query.q
  }
  if (route.query.kbId) {
    selectedKb.value = Number(route.query.kbId)
  }
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

async function newChat() {
  currentConversationId.value = null
  currentTitle.value = '新对话'
  messages.value = []
  selectedKb.value = null
}

function switchConv(conv) {
  currentConversationId.value = conv.id
  currentTitle.value = conv.title
  loadMessages(conv.id)
}

async function loadMessages(convId) {
  try {
    const data = await getMessages(convId)
    // 组装成问答对
    const msgs = []
    if (data) {
      for (const r of data) {
        if (r.question && r.answer) {
          msgs.push(r)
        }
      }
    }
    messages.value = msgs
    scrollBottom()
  } catch {}
}

async function deleteConv(id) {
  try {
    await deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentConversationId.value === id) {
      newChat()
    }
  } catch {}
}

async function send() {
  if (!input.value.trim() || loading.value) return
  const q = input.value.trim()
  input.value = ''
  loading.value = true

  // 乐观插入用户消息
  messages.value.push({ question: q, answer: '', matchedChunks: [] })
  scrollBottom()

  try {
    const data = await askQuestion({
      conversationId: currentConversationId.value,
      knowledgeBaseId: selectedKb.value,
      question: q
    })

    // 更新助手消息
    const last = messages.value[messages.value.length - 1]
    last.answer = data.answer
    last.matchedChunks = data.matchedChunks || []

    // 首次对话自动获取 conversationId
    if (!currentConversationId.value && data.conversationId) {
      currentConversationId.value = data.conversationId
      // 更新标题
      currentTitle.value = q.length > 20 ? q.substring(0, 20) + '...' : q
    }
    // 刷新对话列表
    await loadConversations()
    scrollBottom()

  } catch (e) {
    const last = messages.value[messages.value.length - 1]
    last.answer = '抱歉，请求失败，请重试。'
  } finally {
    loading.value = false
  }
}

function scrollBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}
</script>
