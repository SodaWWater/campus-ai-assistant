import http from './http'

export function getStudentDashboard() {
  return http.get('/student/dashboard')
}

export function getStudentKnowledgeBases() {
  return http.get('/student/knowledge-bases')
}
