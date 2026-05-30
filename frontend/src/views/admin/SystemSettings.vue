<template>
  <div>
    <h2>⚙️ 系统设置</h2>
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header><strong>系统配置信息</strong></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="LLM 模式">{{ config.llmMode || 'mock' }}</el-descriptions-item>
        <el-descriptions-item label="LLM Provider">{{ config.provider || 'deepseek' }}</el-descriptions-item>
        <el-descriptions-item label="MySQL">
          <el-tag :type="config.mysql ? 'success' : 'danger'" size="small">{{ config.mysql ? '已连接' : '未连接' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Redis">
          <el-tag :type="config.redis ? 'success' : 'danger'" size="small">{{ config.redis ? '已连接' : '未连接' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="RabbitMQ">
          <el-tag :type="config.rabbitmq ? 'success' : 'danger'" size="small">{{ config.rabbitmq ? '已连接' : '未连接' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 16px">
        <el-link href="http://localhost:15672" target="_blank" type="primary">RabbitMQ 管理界面</el-link>
        <el-divider direction="vertical" />
        <el-link href="http://localhost:8081/swagger-ui.html" target="_blank" type="primary">Swagger API 文档</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSystemConfig } from '../../api/admin'

const config = ref({})

onMounted(async () => {
  try { config.value = await getSystemConfig() || {} } catch {}
})
</script>
