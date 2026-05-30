<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center">
      <h2>👥 用户管理</h2>
      <el-button type="primary" @click="openCreate">+ 创建用户</el-button>
    </div>

    <el-table :data="users" style="margin-top: 16px" v-loading="loading" empty-text="暂无用户" stripe>
      <el-table-column prop="id" label="ID" width="55" />
      <el-table-column prop="username" label="用户名" min-width="100" />
      <el-table-column prop="nickname" label="昵称" min-width="90" />
      <el-table-column prop="role" label="角色" width="80">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'TEACHER' ? 'warning' : 'success'" size="small">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 'ENABLED'" size="small" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" min-width="140">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="140">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '创建用户'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="editing" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="editing ? '留空不修改' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio value="STUDENT">学生</el-radio>
            <el-radio value="TEACHER">教师</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
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

const users = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const editId = ref(null)
const form = ref({ username: '', password: '', nickname: '', role: 'STUDENT', status: 'ENABLED' })

function roleLabel(r) { return r === 'STUDENT' ? '学生' : r === 'TEACHER' ? '教师' : '管理员' }
function fmt(t) { if (!t) return ''; const d = new Date(t); return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2,'0')}` }

onMounted(() => loadUsers())

async function loadUsers() {
  loading.value = true
  try { users.value = await http.get('/admin/users') || [] } catch {}
  finally { loading.value = false }
}

function openCreate() {
  editing.value = false; editId.value = null
  form.value = { username: '', password: '', nickname: '', role: 'STUDENT', status: 'ENABLED' }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true; editId.value = row.id
  form.value = { username: row.username, password: '', nickname: row.nickname, role: row.role, status: row.status }
  dialogVisible.value = true
}

async function save() {
  if (!editing.value && (!form.value.username || !form.value.password)) {
    ElMessage.warning('用户名和密码不能为空'); return
  }
  saving.value = true
  try {
    if (editing.value) {
      await http.put(`/admin/users/${editId.value}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await http.post('/admin/users', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch {}
  finally { saving.value = false }
}

async function toggleStatus(row) {
  try {
    await http.put(`/admin/users/${row.id}/status`)
    ElMessage.success(row.status === 'ENABLED' ? '已禁用' : '已启用')
    loadUsers()
  } catch {}
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${row.username}？`, '警告', { type: 'warning' })
    await http.delete(`/admin/users/${row.id}`)
    ElMessage.success('已删除')
    loadUsers()
  } catch {}
}
</script>
