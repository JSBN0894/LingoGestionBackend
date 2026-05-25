import { useAuth } from '@/context/AuthContext'

/**
 * Restricts access by user role.
 * If the user's role doesn't match any of the required roles, shows an access denied message.
 */
export function RoleGuard({
  requiredRoles,
  children,
}: {
  requiredRoles: string[]
  children: React.ReactNode
}) {
  const { user } = useAuth()

  // ADMIN always has access
  if (user?.role === 'ADMIN') {
    return <>{children}</>
  }

  if (!user || !requiredRoles.includes(user.role)) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-4">
        <h2 className="text-2xl font-bold text-destructive">Access Denied</h2>
        <p className="text-muted-foreground">
          This area requires one of: <strong>{requiredRoles.join(', ')}</strong>.
        </p>
      </div>
    )
  }

  return <>{children}</>
}
