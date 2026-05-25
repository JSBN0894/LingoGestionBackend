import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { RoleGuard } from './components/RoleGuard'
import { AdminLayout } from './components/AdminLayout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { UsersPage } from './pages/users/UsersPage'
import { ProductsPage } from './pages/products/ProductsPage'
import { CategoriesPage } from './pages/categories/CategoriesPage'
import { CustomersPage } from './pages/customers/CustomersPage'
import { OrdersPage } from './pages/orders/OrdersPage'
import { ShipmentsPage } from './pages/shipments/ShipmentsPage'
import { StatesPage } from './pages/states/StatesPage'
import { AuditLogPage } from './pages/audit/AuditLogPage'

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
            <RoleGuard requiredRoles={['ADMIN']}>
              <AdminLayout>
                <DashboardPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/users"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN']}>
              <AdminLayout>
                <UsersPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/products"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN', 'PRODUCCION', 'VENTAS']}>
              <AdminLayout>
                <ProductsPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/categories"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN']}>
              <AdminLayout>
                <CategoriesPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/customers"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN', 'VENTAS']}>
              <AdminLayout>
                <CustomersPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/orders"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN', 'VENTAS']}>
              <AdminLayout>
                <OrdersPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/shipments"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN', 'LOGISTICA']}>
              <AdminLayout>
                <ShipmentsPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/states"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN']}>
              <AdminLayout>
                <StatesPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/audit"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRoles={['ADMIN']}>
              <AdminLayout>
                <AuditLogPage />
              </AdminLayout>
            </RoleGuard>
          </ProtectedRoute>
        }
      />

      {/* Redirects */}
      <Route path="/" element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/admin/dashboard" replace />} />
    </Routes>
  )
}

export default App
