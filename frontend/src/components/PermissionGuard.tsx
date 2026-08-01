import { useAuth } from '@/context/AuthContext'
import { hasAnyPermission } from '@/lib/permissions'

/**
 * Restricts access by permission. No hardcoded admin bypass — access comes
 * purely from the user's role(s) granting the required permission, exactly
 * like any other role.
 */
export function PermissionGuard({
  anyOf,
  children,
}: {
  anyOf: string[]
  children: React.ReactNode
}) {
  const { user } = useAuth()

  if (!user || !hasAnyPermission(user.permissions, anyOf)) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-4">
        <h2 className="text-2xl font-bold text-destructive">Access Denied</h2>
        <p className="text-muted-foreground">
          This area requires one of: <strong>{anyOf.join(', ')}</strong>.
        </p>
      </div>
    )
  }

  return <>{children}</>
}
