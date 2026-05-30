<template>
  <el-card shadow="hover">
    <div style="text-align: center">
      <div :style="{ fontSize: '36px', color: color }">{{ value }}</div>
      <div style="color: #909399; margin-top: 8px">{{ label }}</div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  value: { type: [Number, String], default: 0 },
  label: { type: String, default: '' },
  color: { type: String, default: '#409eff' }
})
</script>
