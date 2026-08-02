import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { createContext, useContext, type ReactNode } from 'react'
import type { User } from '@/context/AuthContext'

// Mock the api module
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

const mockAdminUser: User = {
  id: '1',
  username: 'admin',
  email: 'admin@linogo.com',
  fullName: 'Admin User',
  roles: [{ id: 1, name: 'Administrador' }],
  permissions: ['DASHBOARD_VIEW', 'USERS_MANAGE', 'ROLES_MANAGE'],
  isEnabled: true,
}

// Test auth context
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

vi.mock('@/context/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/context/AuthContext')>()
  return {
    ...actual,
    useAuth: () => useContext(TestAuthContext),
  }
})

// We need to mock PermissionGuard and AdminLayout since they have their own dependencies
vi.mock('@/components/PermissionGuard', () => ({
  PermissionGuard: ({ children }: { children: ReactNode }) => <>{children}</>,
}))

vi.mock('@/components/AdminLayout', () => ({
  AdminLayout: ({ children }: { children: ReactNode }) => <div data-testid="admin-layout">{children}</div>,
}))

vi.mock('@/pages/DashboardPage', () => ({
  DashboardPage: () => <div data-testid="dashboard-page">Dashboard</div>,
}))

vi.mock('@/pages/users/UsersPage', () => ({
  UsersPage: () => <div data-testid="users-page">Users</div>,
}))

vi.mock('@/pages/roles/RolesPage', () => ({
  RolesPage: () => <div data-testid="roles-page">Roles</div>,
}))

vi.mock('@/pages/products/ProductsPage', () => ({
  ProductsPage: () => <div data-testid="products-page">Products</div>,
}))

vi.mock('@/pages/categories/CategoriesPage', () => ({
  CategoriesPage: () => <div data-testid="categories-page">Categories</div>,
}))

vi.mock('@/pages/customers/CustomersPage', () => ({
  CustomersPage: () => <div data-testid="customers-page">Customers</div>,
}))

vi.mock('@/pages/orders/OrdersPage', () => ({
  OrdersPage: () => <div data-testid="orders-page">Orders</div>,
}))

vi.mock('@/pages/shipments/ShipmentsPage', () => ({
  ShipmentsPage: () => <div data-testid="shipments-page">Shipments</div>,
}))

vi.mock('@/pages/states/StatesPage', () => ({
  StatesPage: () => <div data-testid="states-page">States</div>,
}))

vi.mock('@/pages/audit/AuditLogPage', () => ({
  AuditLogPage: () => <div data-testid="audit-page">Audit Log</div>,
}))

vi.mock('@/pages/LoginPage', () => ({
  LoginPage: () => <div data-testid="login-page">Login</div>,
}))

// Import App after mocks are set up
import App from '@/App'

describe('App Routing', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Dashboard route - no duplicate route bug', () => {
    it('renders DashboardPage for authenticated ADMIN user at /admin/dashboard', () => {
      render(
        <MemoryRouter initialEntries={['/admin/dashboard']}>
          <TestAuthProvider user={mockAdminUser} isLoading={false}>
            <App />
          </TestAuthProvider>
        </MemoryRouter>
      )

      // The dashboard should be visible, NOT redirected to login
      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument()
    })

    it('does NOT redirect authenticated user to login when visiting /admin/dashboard', () => {
      render(
        <MemoryRouter initialEntries={['/admin/dashboard']}>
          <TestAuthProvider user={mockAdminUser} isLoading={false}>
            <App />
          </TestAuthProvider>
        </MemoryRouter>
      )

      // Login page should NOT be visible
      expect(screen.queryByTestId('login-page')).not.toBeInTheDocument()
    })

    it('redirects unauthenticated user to /admin/login when visiting /admin/dashboard', () => {
      render(
        <MemoryRouter initialEntries={['/admin/dashboard']}>
          <TestAuthProvider user={null} isLoading={false}>
            <App />
          </TestAuthProvider>
        </MemoryRouter>
      )

      // Should show login page
      expect(screen.getByTestId('login-page')).toBeInTheDocument()
    })
  })

  describe('Root redirects', () => {
    it('redirects / to /admin/login', () => {
      render(
        <MemoryRouter initialEntries={['/']}>
          <TestAuthProvider user={null} isLoading={false}>
            <App />
          </TestAuthProvider>
        </MemoryRouter>
      )

      expect(screen.getByTestId('login-page')).toBeInTheDocument()
    })

    it('redirects /admin to /admin/login', () => {
      render(
        <MemoryRouter initialEntries={['/admin']}>
          <TestAuthProvider user={null} isLoading={false}>
            <App />
          </TestAuthProvider>
        </MemoryRouter>
      )

      expect(screen.getByTestId('login-page')).toBeInTheDocument()
    })
  })

  describe('Route uniqueness - no duplicate paths', () => {
    it('has exactly one route definition for /admin/dashboard', () => {
      // Parse the App.tsx source to verify no duplicate route paths
      // This is a static analysis test to prevent the bug from recurring
      const fs = require('fs')
      const path = require('path')
      const appSource = fs.readFileSync(
        path.resolve(__dirname, './App.tsx'),
        'utf-8'
      )

      // Count occurrences of path="/admin/dashboard"
      const dashboardRouteMatches = appSource.match(/path="\/admin\/dashboard"/g)
      const count = dashboardRouteMatches ? dashboardRouteMatches.length : 0

      expect(count).toBe(1)
    })
  })
})
