import { Routes, Route, Navigate } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { RoleGuard } from './components/RoleGuard'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<div>Login page (Phase 5)</div>} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <RoleGuard requiredRole="ADMIN">
              <div>Admin dashboard (Phase 5)</div>
            </RoleGuard>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/admin/" replace />} />
    </Routes>
  )
}

export default App
