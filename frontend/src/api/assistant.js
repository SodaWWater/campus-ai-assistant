import http from './http'

export function health() {
  return http.get('/health')
}

export function createKnowledgeBase(payload) {
  return http.post('/kb', payload)
}

export function listKnowledgeBases() {
  return http.get('/kb/list')
}

export function getKnowledgeBases() {
  return http.get('/kb')
}

export function getKnowledgeBase(id) {
  return http.get(`/kb/${id}`)
}

export function deleteKnowledgeBase(id) {
  return http.delete(`/kb/${id}`)
}

export function addDocument(knowledgeBaseId, payload) {
  return http.post(`/kb/${knowledgeBaseId}/document`, payload)
}

export function uploadDocument(knowledgeBaseId, payload) {
  return http.post(`/kb/${knowledgeBaseId}/document/upload`, payload)
}

export function listChunks(knowledgeBaseId) {
  return http.get(`/kb/${knowledgeBaseId}/chunks`)
}

export function deleteDocument(documentId) {
  return http.delete(`/kb/document/${documentId}`)
}

export function ask(payload) {
  return http.post('/chat/ask', payload)
}

export function listStudentScores(studentNo) {
  return http.get(`/academic/student/${studentNo}/scores`)
}

export function getCourseAverage(courseId) {
  return http.get(`/academic/course/${courseId}/average`)
}
