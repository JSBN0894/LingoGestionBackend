import axios from 'axios'

/**
 * Extract CSRF token from the XSRF-TOKEN cookie.
 * Spring Security's CookieCsrfTokenRepository sets this cookie with httpOnly=false
 * so it's readable from JavaScript.
 */
function getCsrfToken(): string | undefined {
  const match = document.cookie.match(/(^|;)\s*XSRF-TOKEN\s*=\s*([^;]+)/)
  return match ? decodeURIComponent(match[2]) : undefined
}

/**
 * Axios instance configured for the admin panel.
 * - baseURL: /api (all API endpoints)
 * - withCredentials: true (sends cookies automatically)
 * - Request interceptor: injects X-CSRF-Token header for mutation requests
 * - Response interceptor: handles 401 by redirecting to login
 */
export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: add CSRF token to mutation requests
api.interceptors.request.use((config) => {
  const method = config.method?.toLowerCase() ?? ''
  const csrfMutations = ['post', 'put', 'patch', 'delete']

  if (csrfMutations.includes(method)) {
    const token = getCsrfToken()
    if (token) {
      config.headers['X-CSRF-Token'] = token
    }
  }

  return config
})

// Response interceptor: handle 401 by redirecting to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Avoid redirect loop if already on login page
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(error)
  }
)

export { getCsrfToken }
