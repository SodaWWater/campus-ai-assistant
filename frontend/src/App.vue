<template>
  <el-config-provider>
    <div class="app-shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-mark">AI</div>
          <div>
            <h1>校园资料智能问答</h1>
            <p>知识库 · RAG · 学业查询</p>
          </div>
        </div>

        <el-menu :default-active="activeView" class="nav-menu" @select="activeView = $event">
          <el-menu-item index="chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>问答调试</span>
          </el-menu-item>
          <el-menu-item index="knowledge">
            <el-icon><Collection /></el-icon>
            <span>知识库</span>
          </el-menu-item>
          <el-menu-item index="academic">
            <el-icon><Notebook /></el-icon>
            <span>学业查询</span>
          </el-menu-item>
        </el-menu>

        <div class="status-panel">
          <span>后端状态</span>
          <el-tag :type="healthStatus === 'ok' ? 'success' : 'warning'">{{ healthStatus }}</el-tag>
        </div>
      </aside>

      <main class="main-panel">
        <header class="topbar">
          <div>
            <h2>{{ viewTitle }}</h2>
            <p>默认 mock 模式可演示完整链路，spring-ai/real 模式由后端配置控制。</p>
          </div>
          <el-button :icon="Refresh" @click="bootstrap">刷新</el-button>
        </header>

        <section v-if="activeView === 'chat'" class="workspace two-column">
          <el-card class="tool-card" shadow="never">
            <template #header>提问</template>
            <el-form label-position="top">
              <el-form-item label="用户 ID">
                <el-input-number v-model="chatForm.userId" :min="1" />
              </el-form-item>
              <el-form-item label="知识库">
                <el-select v-model="chatForm.knowledgeBaseId" clearable placeholder="可选">
                  <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="问题">
                <el-input v-model="chatForm.question" type="textarea" :rows="6" placeholder="例如：帮我解释 Java 集合怎么复习" />
              </el-form-item>
              <el-button type="primary" :icon="ChatDotRound" :loading="chatLoading" @click="submitQuestion">
                发送问题
              </el-button>
            </el-form>
          </el-card>

          <div class="result-stack">
            <el-card class="tool-card" shadow="never">
              <template #header>回答</template>
              <el-empty v-if="!chatResult" description="还没有回答" />
              <div v-else>
                <el-tag>{{ chatResult.sourceType }}</el-tag>
                <p class="answer-text">{{ chatResult.answer }}</p>
                <p class="muted">conversationId: {{ chatResult.conversationId }}</p>
              </div>
            </el-card>

            <el-card class="tool-card" shadow="never">
              <template #header>Prompt 预览</template>
              <el-empty v-if="!chatResult?.promptPreview" description="学业查询或缓存命中时可能没有 Prompt" />
              <pre v-else class="prompt-preview">{{ chatResult.promptPreview }}</pre>
            </el-card>

            <el-card class="tool-card" shadow="never">
              <template #header>matchedChunks</template>
              <el-empty v-if="!chatResult?.matchedChunks?.length" description="暂无命中片段" />
              <el-timeline v-else>
                <el-timeline-item v-for="chunk in chatResult.matchedChunks" :key="chunk.id" :timestamp="`chunk #${chunk.chunkIndex}`">
                  {{ chunk.content }}
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </div>
        </section>

        <section v-if="activeView === 'knowledge'" class="workspace two-column">
          <el-card class="tool-card" shadow="never">
            <template #header>创建知识库</template>
            <el-form label-position="top">
              <el-form-item label="名称">
                <el-input v-model="kbForm.name" placeholder="Java 复习资料库" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="kbForm.description" type="textarea" :rows="3" />
              </el-form-item>
              <el-button type="primary" :icon="Plus" @click="submitKnowledgeBase">创建</el-button>
            </el-form>
          </el-card>

          <el-card class="tool-card" shadow="never">
            <template #header>知识库列表</template>
            <el-table :data="knowledgeBases" height="260">
              <el-table-column prop="id" label="ID" width="72" />
              <el-table-column prop="name" label="名称" />
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <el-button size="small" :icon="DocumentAdd" @click="selectKb(row.id)">选择</el-button>
                  <el-button size="small" type="danger" :icon="Delete" @click="removeKb(row.id)" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card class="tool-card wide" shadow="never">
            <template #header>录入文档</template>
            <el-form label-position="top">
              <el-form-item label="知识库">
                <el-select v-model="documentForm.knowledgeBaseId" placeholder="选择知识库">
                  <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="标题">
                <el-input v-model="documentForm.title" />
              </el-form-item>
              <el-form-item label="正文">
                <el-input v-model="documentForm.content" type="textarea" :rows="6" />
              </el-form-item>
              <el-button type="primary" :icon="DocumentAdd" @click="submitDocument">保存并切分</el-button>
              <el-button :icon="Upload" @click="submitDocumentAsync">RabbitMQ 异步入队</el-button>
              <el-button :icon="Search" @click="loadChunks">查看 chunks</el-button>
            </el-form>
          </el-card>

          <el-card class="tool-card wide" shadow="never">
            <template #header>文档片段</template>
            <el-table :data="chunks" height="320">
              <el-table-column prop="chunkIndex" label="#" width="72" />
              <el-table-column prop="content" label="内容" />
              <el-table-column prop="keywords" label="关键词" width="220" />
            </el-table>
          </el-card>
        </section>

        <section v-if="activeView === 'academic'" class="workspace two-column">
          <el-card class="tool-card" shadow="never">
            <template #header>学生成绩</template>
            <el-form label-position="top">
              <el-form-item label="学号">
                <el-input v-model="studentNo" placeholder="20230001" />
              </el-form-item>
              <el-button type="primary" :icon="Search" @click="queryScores">查询成绩</el-button>
            </el-form>
            <el-table :data="scores" class="inner-table">
              <el-table-column prop="studentName" label="学生" />
              <el-table-column prop="courseName" label="课程" />
              <el-table-column prop="score" label="成绩" width="90" />
              <el-table-column prop="semester" label="学期" />
            </el-table>
          </el-card>

          <el-card class="tool-card" shadow="never">
            <template #header>课程平均分</template>
            <el-form label-position="top">
              <el-form-item label="课程 ID">
                <el-input-number v-model="courseId" :min="1" />
              </el-form-item>
              <el-button type="primary" :icon="Search" @click="queryAverage">查询平均分</el-button>
            </el-form>
            <el-descriptions v-if="courseAverage" class="inner-table" :column="1" border>
              <el-descriptions-item label="课程">{{ courseAverage.courseName }}</el-descriptions-item>
              <el-descriptions-item label="平均分">{{ courseAverage.averageScore }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </section>
      </main>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Collection,
  Delete,
  DocumentAdd,
  Notebook,
  Plus,
  Refresh,
  Search,
  Upload
} from '@element-plus/icons-vue'
import {
  addDocument,
  ask,
  createKnowledgeBase,
  deleteKnowledgeBase,
  getCourseAverage,
  health,
  listChunks,
  listKnowledgeBases,
  listStudentScores,
  uploadDocument
} from './api/assistant'

const activeView = ref('chat')
const healthStatus = ref('checking')
const knowledgeBases = ref([])
const chunks = ref([])
const chatResult = ref(null)
const chatLoading = ref(false)
const scores = ref([])
const courseAverage = ref(null)
const studentNo = ref('20230001')
const courseId = ref(1)

const kbForm = reactive({ name: '', description: '' })
const documentForm = reactive({ knowledgeBaseId: null, title: '', content: '' })
const chatForm = reactive({
  userId: 1,
  question: '帮我解释 Java 集合怎么复习',
  knowledgeBaseId: null
})

const viewTitle = computed(() => {
  if (activeView.value === 'knowledge') return '知识库管理'
  if (activeView.value === 'academic') return '学业数据查询'
  return '问答调试'
})

async function bootstrap() {
  try {
    healthStatus.value = await health()
  } catch {
    healthStatus.value = 'offline'
  }
  await loadKnowledgeBases()
}

async function loadKnowledgeBases() {
  knowledgeBases.value = await listKnowledgeBases()
  if (!documentForm.knowledgeBaseId && knowledgeBases.value.length) {
    selectKb(knowledgeBases.value[0].id)
  }
}

function selectKb(id) {
  documentForm.knowledgeBaseId = id
  chatForm.knowledgeBaseId = id
}

async function submitKnowledgeBase() {
  if (!kbForm.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  await createKnowledgeBase({ ...kbForm })
  kbForm.name = ''
  kbForm.description = ''
  ElMessage.success('知识库已创建')
  await loadKnowledgeBases()
}

async function removeKb(id) {
  await ElMessageBox.confirm('确定删除这个知识库及其文档片段吗？', '确认删除', { type: 'warning' })
  await deleteKnowledgeBase(id)
  ElMessage.success('已删除')
  await loadKnowledgeBases()
}

function validateDocument() {
  if (!documentForm.knowledgeBaseId) {
    ElMessage.warning('请先选择知识库')
    return false
  }
  if (!documentForm.title.trim() || !documentForm.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return false
  }
  return true
}

async function submitDocument() {
  if (!validateDocument()) return
  await addDocument(documentForm.knowledgeBaseId, {
    title: documentForm.title,
    content: documentForm.content
  })
  ElMessage.success('文档已录入并切分')
  resetDocument()
  await loadChunks()
}

async function submitDocumentAsync() {
  if (!validateDocument()) return
  await uploadDocument(documentForm.knowledgeBaseId, {
    title: documentForm.title,
    content: documentForm.content
  })
  ElMessage.success('文档已写入，切分任务已提交到 RabbitMQ')
  resetDocument()
}

function resetDocument() {
  documentForm.title = ''
  documentForm.content = ''
}

async function loadChunks() {
  if (!documentForm.knowledgeBaseId) {
    ElMessage.warning('请选择知识库')
    return
  }
  chunks.value = await listChunks(documentForm.knowledgeBaseId)
}

async function submitQuestion() {
  if (!chatForm.question.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  chatLoading.value = true
  try {
    chatResult.value = await ask({ ...chatForm })
  } finally {
    chatLoading.value = false
  }
}

async function queryScores() {
  scores.value = await listStudentScores(studentNo.value)
}

async function queryAverage() {
  courseAverage.value = await getCourseAverage(courseId.value)
}

onMounted(bootstrap)
</script>
