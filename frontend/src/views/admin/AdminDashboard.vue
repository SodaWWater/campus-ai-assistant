<template>
  <div>
    <h2>📊 系统看板</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6" v-for="m in metrics" :key="m.label">
        <el-card shadow="hover">
          <div style="text-align: center">
            <div :style="{ fontSize: '36px', color: m.color }">{{ m.value }}</div>
            <div style="color: #909399; margin-top: 8px">{{ m.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><strong>📄 文档状态分布</strong></template>
          <el-row :gutter="12">
            <el-col :span="8" style="text-align: center">
              <div style="font-size: 28px; color: #67c23a">{{ docStatus.done }}</div>
              <div style="color: #909399">已完成</div>
            </el-col>
            <el-col :span="8" style="text-align: center">
              <div style="font-size: 28px; color: #e6a23c">{{ docStatus.processing }}</div>
              <div style="color: #909399">处理中</div>
            </el-col>
            <el-col :span="8" style="text-align: center">
              <div style="font-size: 28px; color: #f56c6c">{{ docStatus.failed }}</div>
              <div style="color: #909399">失败</div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><strong>🔧 服务状态</strong></template>
          <div v-for="s in services" :key="s.name" style="padding: 10px 0; border-bottom: 1px solid #ebeef5; display: flex; justify-content: space-between">
            <span>{{ s.name }}</span>
            <el-tag :type="s.ok ? 'success' : 'danger'" size="small">{{ s.ok ? '正常' : '异常' }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminDashboard, getSystemConfig } from '../../api/admin'

const metrics = ref([
  { label: '用户数', value: 0, color: '#409eff' },
  { label: '知识库数', value: 0, color: '#67c23a' },
  { label: '文档数', value: 0, color: '#e6a23c' },
  { label: '问答次数', value: 0, color: '#909399' }
])
const docStatus = ref({ done: 0, processing: 0, failed: 0 })
const services = ref([
  { name: 'MySQL', ok: false },
  { name: 'Redis', ok: false },
  { name: 'RabbitMQ', ok: false },
  { name: 'LLM 模式', ok: false }
])

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
    services.value[3] = { name: 'LLM: ' + (cfg.llmMode || 'mock'), ok: true }
  } catch {}
})
</script>
