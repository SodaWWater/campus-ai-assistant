import http from './http'

export function getTeacherDashboard() {
  return http.get('/teacher/dashboard')
}

export function getTeacherKnowledgeBases() {
  return http.get('/teacher/knowledge-bases')
}

export function createKnowledgeBase(data) {
  return http.post('/kb', data)
}

export function updateKnowledgeBase(id, data) {
  return http.put(`/kb/${id}`, data)
}

export function deleteKnowledgeBase(id) {
  return http.delete(`/kb/${id}`)
}

export function getDocuments(kbId) {
  return http.get(`/kb/${kbId}/documents`)
}

export function uploadDocument(kbId, formData) {
  return http.post(`/kb/${kbId}/document/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getDocumentChunks(docId) {
  return http.get(`/document/${docId}/chunks`)
}

export function reprocessDocument(docId) {
  return http.post(`/document/${docId}/reprocess`)
}
