<template>
  <el-card style="margin-bottom: 8px">
    <p><strong>来源：</strong>{{ chunk.documentTitle || '未知文档' }} (片段 #{{ chunk.chunkIndex }})</p>
    <p><strong>关键词得分：</strong>{{ chunk.score || 0 }}</p>
    <el-collapse>
      <el-collapse-item title="查看片段内容">
        <div style="font-size: 13px; color: #606266; white-space: pre-wrap">{{ chunk.content }}</div>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<script setup>
defineProps({ chunk: { type: Object, required: true } })
</script>
