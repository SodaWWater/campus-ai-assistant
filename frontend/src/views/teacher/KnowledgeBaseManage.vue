<template>
  <div class="kb-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Course Knowledge Base</p>
        <h1>课程知识库管理</h1>
      </div>
      <el-button type="primary" @click="showCreate">创建知识库</el-button>
    </header>

    <section class="summary-grid">
      <div v-for="item in summary" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="kb-grid">
      <article v-for="kb in knowledgeBases" :key="kb.id" class="kb-card">
        <div class="kb-card-header">
          <div>
            <h2>{{ kb.name }}</h2>
            <p>{{ kb.description || '暂无描述' }}</p>
          </div>
          <el-tag :type="visibilityType(kb.visibility)" effect="plain">{{ visibilityText(kb.visibility) }}</el-tag>
        </div>

        <div class="source-line">
          <span v-for="source in sourcesFor(kb.name)" :key="source">{{ source }}</span>
        </div>

        <div class="kb-metrics">
          <div><strong>{{ kb.documentCount || 0 }}</strong><span>文档</span></div>
          <div><strong>{{ kb.chunkCount || 0 }}</strong><span>片段</span></div>
          <div><strong>{{ kb.ownerName || '李老师' }}</strong><span>维护人</span></div>
        </div>

        <div class="kb-actions">
          <el-button size="small" @click="router.push('/teacher/documents/' + kb.id)">文档中心</el-button>
          <el-button size="small" @click="editKb(kb)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="deleteKb(kb.id)">删除</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingKb ? '编辑知识库' : '创建知识库'" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="form.visibility">
            <el-radio value="PUBLIC">公开</el-radio>
            <el-radio value="COURSE_ONLY">课程内</el-radio>
            <el-radio value="PRIVATE">私有</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKb">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createKnowledgeBase, deleteKnowledgeBase, getTeacherKnowledgeBases, updateKnowledgeBase } from '../../api/teacher'

const router = useRouter()
const knowledgeBases = ref([])
const dialogVisible = ref(false)
const editingKb = ref(null)
const form = reactive({ name: '', description: '', visibility: 'PUBLIC' })

const summary = computed(() => {
  const docCount = knowledgeBases.value.reduce((sum, kb) => sum + (kb.documentCount || 0), 0)
  const chunkCount = knowledgeBases.value.reduce((sum, kb) => sum + (kb.chunkCount || 0), 0)
  const publicCount = knowledgeBases.value.filter(kb => kb.visibility === 'PUBLIC').length
  return [
    { label: '课程知识库', value: knowledgeBases.value.length },
    { label: '资料文档', value: docCount },
    { label: '知识片段', value: chunkCount },
    { label: '公开资料库', value: publicCount }
  ]
})

onMounted(() => loadKbs())

async function loadKbs() {
  try {
    knowledgeBases.value = await getTeacherKnowledgeBases() || []
  } catch {}
}

function showCreate() {
  editingKb.value = null
  form.name = ''
  form.description = ''
  form.visibility = 'PUBLIC'
  dialogVisible.value = true
}

function editKb(kb) {
  editingKb.value = kb
  form.name = kb.name
  form.description = kb.description
  form.visibility = kb.visibility || 'PUBLIC'
  dialogVisible.value = true
}

async function saveKb() {
  if (!form.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  try {
    if (editingKb.value) {
      await updateKnowledgeBase(editingKb.value.id, form)
      ElMessage.success('知识库已更新')
    } else {
      await createKnowledgeBase(form)
      ElMessage.success('知识库已创建')
    }
    dialogVisible.value = false
    loadKbs()
  } catch {}
}

async function deleteKb(id) {
  try {
    await ElMessageBox.confirm('删除知识库会同时删除其文档和知识片段，确认继续？', '删除确认', { type: 'warning' })
    await deleteKnowledgeBase(id)
    ElMessage.success('知识库已删除')
    loadKbs()
  } catch {}
}

function visibilityText(value) {
  if (value === 'COURSE_ONLY') return '课程内'
  if (value === 'PRIVATE') return '私有'
  return '公开'
}

function visibilityType(value) {
  if (value === 'COURSE_ONLY') return 'warning'
  if (value === 'PRIVATE') return 'info'
  return 'success'
}

function sourcesFor(name = '') {
  if (name.includes('Java')) return ['MIT OCW', 'OpenDSA', '校内实验指导']
  if (name.includes('数据结构')) return ['Open Data Structures', 'Java Edition']
  if (name.includes('数据库')) return ['智慧高教参考', '校内整理讲义']
  if (name.includes('事务')) return ['校内流程', '学院规则']
  return ['校内资料']
}
</script>

<style scoped>
.kb-page {
  display: grid;
  gap: 20px;
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

h1,
h2 {
  margin: 0;
  color: #1f2937;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card,
.kb-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.summary-card {
  padding: 16px;
}

.summary-card span {
  color: #64748b;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.kb-card {
  padding: 18px;
}

.kb-card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.kb-card p {
  color: #64748b;
  line-height: 1.6;
}

.source-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.source-line span {
  padding: 4px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
}

.kb-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.kb-metrics div {
  padding: 10px;
  border-radius: 8px;
  background: #f8fafc;
}

.kb-metrics strong,
.kb-metrics span {
  display: block;
}

.kb-metrics span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.kb-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>

