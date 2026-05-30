import http from './http'

export function getAdminDashboard() {
  return http.get('/admin/dashboard')
}

export function getAdminUsers() {
  return http.get('/admin/users')
}

export function getAdminKnowledgeBases() {
  return http.get('/admin/knowledge-bases')
}

export function getSystemConfig() {
  return http.get('/system/config')
}

export function getAdminDocuments() {
  return http.get('/admin/documents')
}

export function adminReprocessDocument(docId) {
  return http.post(`/admin/document/${docId}/reprocess`)
}

export function adminDeleteDocument(docId) {
  return http.delete(`/admin/document/${docId}`)
}
