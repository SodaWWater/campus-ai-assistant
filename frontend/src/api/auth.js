import http from './http'

export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

export function getMe() {
  return http.get('/auth/me')
}
