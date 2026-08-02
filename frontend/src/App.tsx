import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { PermissionGuard } from './components/PermissionGuard'
import { AdminLayout } from './components/AdminLayout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { UsersPage } from './pages/users/UsersPage'
import { RolesPage } from './pages/roles/RolesPage'
import { ProductsPage } from './pages/products/ProductsPage'
import { CategoriesPage } from './pages/categories/CategoriesPage'
import { CustomersPage } from './pages/customers/CustomersPage'
import { OrdersPage } from './pages/orders/OrdersPage'
import { ShipmentsPage } from './pages/shipments/ShipmentsPage'
import { CarriersPage } from './pages/carriers/CarriersPage'
import { StatesPage } from './pages/states/StatesPage'
import { AuditLogPage } from './pages/audit/AuditLogPage'
import { PAGE_PERMISSIONS } from './lib/permissions'

function App() {
  return (
    <Routes>
      {/* Public route */}
      <Route path="/admin/login" element={<LoginPage />} />

      {/* Protected routes with layout */}
      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/dashboard']}>
              <AdminLayout>
                <DashboardPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/users"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/users']}>
              <AdminLayout>
                <UsersPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/roles"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/roles']}>
              <AdminLayout>
                <RolesPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/products"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/products']}>
              <AdminLayout>
                <ProductsPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/categories"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/categories']}>
              <AdminLayout>
                <CategoriesPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/customers"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/customers']}>
              <AdminLayout>
                <CustomersPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/orders"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/orders']}>
              <AdminLayout>
                <OrdersPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/shipments"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/shipments']}>
              <AdminLayout>
                <ShipmentsPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/carriers"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/carriers']}>
              <AdminLayout>
                <CarriersPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/states"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/states']}>
              <AdminLayout>
                <StatesPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/audit"
        element={
          <ProtectedRoute>
            <PermissionGuard anyOf={PAGE_PERMISSIONS['/admin/audit']}>
              <AdminLayout>
                <AuditLogPage />
              </AdminLayout>
            </PermissionGuard>
          </ProtectedRoute>
        }
      />

      {/* Redirects */}
      <Route path="/" element={<Navigate to="/admin/login" replace />} />
      <Route path="/admin" element={<Navigate to="/admin/login" replace />} />
      <Route path="*" element={<Navigate to="/admin/login" replace />} />
    </Routes>
  )
}

export default App
