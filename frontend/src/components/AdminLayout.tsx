import { useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { ChangePasswordDialog } from '@/components/ChangePasswordDialog'
import { PAGE_PERMISSIONS, hasAnyPermission } from '@/lib/permissions'
import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  Package,
  Layers,
  UserCheck,
  ShoppingCart,
  Truck,
  Building2,
  MapPin,
  ScrollText,
  Menu,
  X,
  LogOut,
  Shield,
  KeyRound,
} from 'lucide-react'

interface NavItem {
  label: string
  path: string
  icon: React.ElementType
  requiredPermissions: string[]
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard, requiredPermissions: PAGE_PERMISSIONS['/admin/dashboard'] },
  { label: 'Users', path: '/admin/users', icon: Users, requiredPermissions: PAGE_PERMISSIONS['/admin/users'] },
  { label: 'Roles & Permissions', path: '/admin/roles', icon: ShieldCheck, requiredPermissions: PAGE_PERMISSIONS['/admin/roles'] },
  { label: 'Products', path: '/admin/products', icon: Package, requiredPermissions: PAGE_PERMISSIONS['/admin/products'] },
  { label: 'Categories', path: '/admin/categories', icon: Layers, requiredPermissions: PAGE_PERMISSIONS['/admin/categories'] },
  { label: 'Customers', path: '/admin/customers', icon: UserCheck, requiredPermissions: PAGE_PERMISSIONS['/admin/customers'] },
  { label: 'Orders', path: '/admin/orders', icon: ShoppingCart, requiredPermissions: PAGE_PERMISSIONS['/admin/orders'] },
  { label: 'Shipments', path: '/admin/shipments', icon: Truck, requiredPermissions: PAGE_PERMISSIONS['/admin/shipments'] },
  { label: 'Carriers', path: '/admin/carriers', icon: Building2, requiredPermissions: PAGE_PERMISSIONS['/admin/carriers'] },
  { label: 'States', path: '/admin/states', icon: MapPin, requiredPermissions: PAGE_PERMISSIONS['/admin/states'] },
  { label: 'Audit Log', path: '/admin/audit', icon: ScrollText, requiredPermissions: PAGE_PERMISSIONS['/admin/audit'] },
]

interface AdminLayoutProps {
  children: React.ReactNode
}

export function AdminLayout({ children }: AdminLayoutProps) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate('/admin/login')
  }

  const filteredItems = NAV_ITEMS.filter((item) => hasAnyPermission(user?.permissions, item.requiredPermissions))

  return (
    <div className="flex min-h-screen bg-background">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r bg-card transition-transform duration-200 lg:static lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex h-14 items-center justify-between border-b px-4">
          <Link to="/admin/dashboard" className="flex items-center gap-2 font-bold text-lg">
            <Shield className="h-5 w-5" />
            <span>Linogo Admin</span>
          </Link>
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden"
            onClick={() => setSidebarOpen(false)}
          >
            <X className="h-5 w-5" />
          </Button>
        </div>

        <nav className="flex-1 overflow-y-auto p-4 space-y-1">
          {filteredItems.map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.path
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setSidebarOpen(false)}
                className={cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                )}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Link>
            )
          })}
        </nav>
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col">
        {/* Header */}
        <header className="flex h-14 items-center justify-between border-b bg-card px-4">
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="h-5 w-5" />
          </Button>

          <div className="flex-1" />

          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-sm font-medium">{user?.username}</p>
              <p className="text-xs text-muted-foreground">{user?.roles.map((r) => r.name).join(', ')}</p>
            </div>
            <Button variant="outline" size="sm" onClick={() => setChangePasswordOpen(true)}>
              <KeyRound className="mr-2 h-4 w-4" />
              Change password
            </Button>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </Button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">{children}</main>
      </div>

      <ChangePasswordDialog open={changePasswordOpen} onOpenChange={setChangePasswordOpen} />
    </div>
  )
}
