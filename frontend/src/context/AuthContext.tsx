import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { api } from '@/lib/api'

export interface User {
  id: string
  username: string
  email: string
  fullName: string
  role: string
  isEnabled: boolean
}

interface AuthContextType {
  user: User | null
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000 // 15 minutes

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Auto-refresh user on mount
  const refresh = useCallback(async () => {
    try {
      const { data } = await api.get<User>('/web/auth/me')
      setUser(data)
    } catch {
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  // Inactivity timeout tracking
  useEffect(() => {
    if (!user) return

    let timeout: ReturnType<typeof setTimeout>

    const resetTimer = () => {
      clearTimeout(timeout)
      timeout = setTimeout(() => {
        setUser(null)
        window.location.href = '/admin/login'
      }, INACTIVITY_TIMEOUT_MS)
    }

    const events: (keyof WindowEventMap)[] = ['mousedown', 'keydown', 'scroll', 'touchstart']
    events.forEach((event) => window.addEventListener(event, resetTimer))
    resetTimer()

    return () => {
      clearTimeout(timeout)
      events.forEach((event) => window.removeEventListener(event, resetTimer))
    }
  }, [user])

  const login = async (username: string, password: string) => {
    // First, ensure we have a CSRF token by hitting the csrf-token endpoint
    await api.get('/web/csrf-token')

    const { data } = await api.post('/web/login', { username, password })
    // After successful login, refresh user state
    await refresh()
    return data
  }

  const logout = async () => {
    try {
      await api.post('/web/logout')
    } finally {
      setUser(null)
      window.location.href = '/admin/login'
    }
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
