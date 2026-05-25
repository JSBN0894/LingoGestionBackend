import { useAuth } from '@/context/AuthContext'

/**
 * Restricts access by user role.
 * If the user's role doesn't match the required role, shows an access denied message.
 */
export function RoleGuard({
  requiredRole,
  children,
}: {
  requiredRole: string
  children: React.ReactNode
}) {
  const { user } = useAuth()

  if (!user || user.role !== requiredRole) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-4">
        <h2 className="text-2xl font-bold text-destructive">Access Denied</h2>
        <p className="text-muted-foreground">
          This area requires <strong>{requiredRole}</strong> privileges.
        </p>
      </div>
    )
  }

  return <>{children}</>
}
