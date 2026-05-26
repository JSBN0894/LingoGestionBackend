import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { createContext, useContext, type ReactNode } from 'react'
import type { User } from '@/context/AuthContext'

// Mock the api module to prevent real HTTP calls
vi.mock('@/lib/api', () => ({
  api: {
    get: vi.fn().mockResolvedValue({ data: null }),
    post: vi.fn().mockResolvedValue({ data: null }),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}))

const mockUser: User = {
  id: '1',
  username: 'admin',
  email: 'admin@linogo.com',
  fullName: 'Admin User',
  role: 'ADMIN',
  isEnabled: true,
}

// Create a test-specific auth context that matches the real AuthContext shape
const TestAuthContext = createContext<{
  user: User | null
  isLoading: boolean
  login: ReturnType<typeof vi.fn>
  logout: ReturnType<typeof vi.fn>
  refresh: ReturnType<typeof vi.fn>
} | undefined>(undefined)

function TestAuthProvider({
  children,
  user,
  isLoading,
}: {
  children: ReactNode
  user: User | null
  isLoading: boolean
}) {
  return (
    <TestAuthContext.Provider value={{ user, isLoading, login: vi.fn(), logout: vi.fn(), refresh: vi.fn() }}>
      {children}
    </TestAuthContext.Provider>
  )
}

// Override useAuth to use our test context
vi.mock('@/context/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/context/AuthContext')>()
  return {
    ...actual,
    useAuth: () => useContext(TestAuthContext),
  }
})

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows loading state when isLoading is true', () => {
    render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <TestAuthProvider user={null} isLoading={true}>
          <ProtectedRoute>
            <div>Protected Content</div>
          </ProtectedRoute>
        </TestAuthProvider>
      </MemoryRouter>
    )

    expect(screen.getByText('Cargando...')).toBeInTheDocument()
  })

  it('redirects to /admin/login when user is null and not loading', () => {
    render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <Routes>
          <Route
            path="/admin/dashboard"
            element={
              <TestAuthProvider user={null} isLoading={false}>
                <ProtectedRoute>
                  <div>Protected Content</div>
                </ProtectedRoute>
              </TestAuthProvider>
            }
          />
          <Route path="/admin/login" element={<div>Login Page</div>} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })

  it('renders children when user is authenticated', () => {
    render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <TestAuthProvider user={mockUser} isLoading={false}>
          <ProtectedRoute>
            <div>Protected Content</div>
          </ProtectedRoute>
        </TestAuthProvider>
      </MemoryRouter>
    )

    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })
})
