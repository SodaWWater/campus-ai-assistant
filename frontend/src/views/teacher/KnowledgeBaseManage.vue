<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center">
      <h2>📚 知识库管理</h2>
      <el-button type="primary" @click="showCreate">创建知识库</el-button>
    </div>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8" v-for="kb in knowledgeBases" :key="kb.id">
        <el-card shadow="hover" style="margin-bottom: 16px">
          <template #header>
            <strong>{{ kb.name }}</strong>
              <el-tag size="small" style="float: right; margin-right: 8px">{{ kb.visibility || 'PUBLIC' }}</el-tag>
            <el-dropdown style="float: right" trigger="click">
              <el-button size="small" circle>⋯</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="editKb(kb)">编辑</el-dropdown-item>
                  <el-dropdown-item @click="deleteKb(kb.id)" style="color: #f56c6c">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <p style="color: #606266">{{ kb.description || '暂无描述' }}</p>
          <div style="color: #909399; font-size: 13px">
            <span>📄 {{ kb.documentCount || 0 }} 文档</span>
            <span style="margin-left: 16px">🧩 {{ kb.chunkCount || 0 }} 片段</span>
          </div>
          <div style="margin-top: 12px">
            <el-button size="small" @click="$router.push('/teacher/documents/' + kb.id)">📂 文档中心</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingKb ? '编辑知识库' : '创建知识库'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="form.visibility">
            <el-radio value="PUBLIC">公开</el-radio>
            <el-radio value="PRIVATE">私有</el-radio>
            <el-radio value="COURSE_ONLY">仅课程</el-radio>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase } from '../../api/teacher'

const knowledgeBases = ref([])
const dialogVisible = ref(false)
const editingKb = ref(null)
const form = reactive({ name: '', description: '', visibility: 'PUBLIC' })

onMounted(() => loadKbs())

async function loadKbs() {
  try { const data = await getTeacherKnowledgeBases(); knowledgeBases.value = data || [] } catch {}
}

function showCreate() {
  editingKb.value = null
  form.name = ''; form.description = ''; form.visibility = 'PUBLIC'
  dialogVisible.value = true
}

function editKb(kb) {
  editingKb.value = kb
  form.name = kb.name; form.description = kb.description; form.visibility = kb.visibility
  dialogVisible.value = true
}

async function saveKb() {
  if (!form.name.trim()) { ElMessage.warning('名称不能为空'); return }
  try {
    if (editingKb.value) {
      await updateKnowledgeBase(editingKb.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createKnowledgeBase(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadKbs()
  } catch {}
}

async function deleteKb(id) {
  try {
    await ElMessageBox.confirm('删除知识库会同时删除所有文档，确认删除？', '警告', { type: 'warning' })
    await deleteKnowledgeBase(id)
    ElMessage.success('删除成功')
    loadKbs()
  } catch {}
}
</script>
