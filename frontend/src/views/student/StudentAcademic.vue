<template>
  <div>
    <h2>📋 学业成绩</h2>

    <!-- 学生信息卡片 -->
    <el-row :gutter="20" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; gap: 16px">
            <div style="width: 48px; height: 48px; background: #409eff; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px">🧑‍🎓</div>
            <div>
              <div style="font-size: 18px; font-weight: bold">{{ info.name }}</div>
              <div style="color: #909399; font-size: 13px">{{ info.studentNo }}</div>
            </div>
          </div>
          <el-divider />
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="专业">{{ info.major }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ info.grade }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-row :gutter="16">
          <el-col :span="8" v-for="m in metrics" :key="m.label">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div :style="{ fontSize: '32px', color: m.color }">{{ m.value }}</div>
                <div style="color: #909399; font-size: 14px; margin-top: 4px">{{ m.label }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>

    <!-- 成绩明细 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header><strong>📊 成绩明细</strong></template>
      <el-table :data="scores" empty-text="暂无成绩记录">
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column prop="courseCode" label="课程代码" width="120" />
        <el-table-column prop="credit" label="学分" width="80">
          <template #default="{ row }">{{ row.credit }}分</template>
        </el-table-column>
        <el-table-column prop="score" label="成绩" width="100">
          <template #default="{ row }">
            <el-tag :type="scoreType(row.score)" size="small">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期" width="130" />
      </el-table>
    </el-card>

    <el-empty v-if="!info.name" description="当前账号未关联学生信息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../api/http'

const info = ref({})
const scores = ref([])
const metrics = ref([
  { label: '平均成绩', value: '--', color: '#409eff' },
  { label: '总学分', value: '--', color: '#67c23a' },
  { label: '课程数', value: '--', color: '#e6a23c' }
])

function scoreType(s) {
  if (s >= 90) return 'success'
  if (s >= 80) return 'primary'
  if (s >= 60) return 'warning'
  return 'danger'
}

onMounted(async () => {
  try {
    const data = await http.get('/student/academic')
    if (data) {
      info.value = {
        name: data.name || '',
        studentNo: data.studentNo || '',
        major: data.major || '',
        grade: data.grade || ''
      }
      scores.value = data.scores || []
      metrics.value[0].value = data.averageScore || '--'
      metrics.value[1].value = data.totalCredit || '--'
      metrics.value[2].value = data.courseCount || '--'
    }
  } catch {}
})
</script>
