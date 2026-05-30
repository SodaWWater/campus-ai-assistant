<template>
  <div>
    <h2>📚 知识库审计</h2>
    <p style="color: #909399">查看和管理所有知识库</p>

    <el-table :data="kbs" style="margin-top: 16px" v-loading="loading" empty-text="暂无知识库">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="ownerName" label="所有者" width="100" />
      <el-table-column prop="visibility" label="可见性" width="90" />
      <el-table-column prop="documentCount" label="文档" width="70" />
      <el-table-column prop="chunkCount" label="片段" width="70" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="$router.push('/admin/documents')">文档</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" title="编辑知识库" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
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
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const kbs = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({ name: '', description: '', visibility: 'PUBLIC' })

onMounted(() => loadKbs())

async function loadKbs() {
  loading.value = true
  try { kbs.value = await http.get('/admin/knowledge-bases') || [] } catch {}
  finally { loading.value = false }
}

function openEdit(row) {
  editId.value = row.id
  form.value = { name: row.name, description: row.description || '', visibility: row.visibility || 'PUBLIC' }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name.trim()) { ElMessage.warning('名称不能为空'); return }
  saving.value = true
  try {
    await http.put(`/admin/knowledge-bases/${editId.value}`, form.value)
    ElMessage.success('更新成功')
    dialogVisible.value = false
    loadKbs()
  } catch {}
  finally { saving.value = false }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除知识库「${row.name}」将同时删除所有文档，确认？`, '警告', { type: 'warning' })
    await http.delete(`/admin/knowledge-bases/${row.id}`)
    ElMessage.success('已删除')
    loadKbs()
  } catch {}
}
</script>
