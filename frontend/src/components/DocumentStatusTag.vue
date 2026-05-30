<template>
  <el-tag :type="tagType" size="small">{{ status }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, default: 'PROCESSING' } })

const tagType = computed(() => {
  if (props.status === 'DONE') return 'success'
  if (props.status === 'PROCESSING') return 'warning'
  if (props.status === 'FAILED') return 'danger'
  return 'info'
})
</script>
