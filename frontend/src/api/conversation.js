import http from './http'

export function listConversations() {
  return http.get('/conversations')
}

export function createConversation(data) {
  return http.post('/conversations', data)
}

export function deleteConversation(id) {
  return http.delete(`/conversations/${id}`)
}

export function getMessages(conversationId) {
  return http.get('/chat/messages', { params: { conversationId } })
}
