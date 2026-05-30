import http from './http'

export function askQuestion(data) {
  return http.post('/chat/ask', data)
}

export function getChatHistory(userId) {
  return http.get('/chat/history', { params: { userId } })
}
