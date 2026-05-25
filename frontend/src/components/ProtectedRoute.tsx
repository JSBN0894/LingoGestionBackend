import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

/**
 * Redirects unauthenticated users to /admin/login.
 * Shows nothing while auth state is loading.
 */
export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-muted-foreground">Cargando...</div>
      </div>
    )
  }

  if (!user) {
    // Preserve the intended destination for post-login redirect
    return <Navigate to="/admin/login" state={{ from: location }} replace />
  }

  return <>{children}</>
}
