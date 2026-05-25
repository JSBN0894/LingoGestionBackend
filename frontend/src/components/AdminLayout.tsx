import { useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import {
  LayoutDashboard,
  Users,
  Package,
  Layers,
  UserCheck,
  ShoppingCart,
  Truck,
  MapPin,
  ScrollText,
  Menu,
  X,
  LogOut,
  Shield,
} from 'lucide-react'

interface NavItem {
  label: string
  path: string
  icon: React.ElementType
  requiredRoles?: string[]
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard, requiredRoles: ['ADMIN'] },
  { label: 'Users', path: '/admin/users', icon: Users, requiredRoles: ['ADMIN'] },
  { label: 'Products', path: '/admin/products', icon: Package, requiredRoles: ['ADMIN', 'PRODUCCION', 'VENTAS'] },
  { label: 'Categories', path: '/admin/categories', icon: Layers, requiredRoles: ['ADMIN'] },
  { label: 'Customers', path: '/admin/customers', icon: UserCheck, requiredRoles: ['ADMIN', 'VENTAS'] },
  { label: 'Orders', path: '/admin/orders', icon: ShoppingCart, requiredRoles: ['ADMIN', 'VENTAS'] },
  { label: 'Shipments', path: '/admin/shipments', icon: Truck, requiredRoles: ['ADMIN', 'LOGISTICA'] },
  { label: 'States', path: '/admin/states', icon: MapPin, requiredRoles: ['ADMIN'] },
  { label: 'Audit Log', path: '/admin/audit', icon: ScrollText, requiredRoles: ['ADMIN'] },
]

interface AdminLayoutProps {
  children: React.ReactNode
}

export function AdminLayout({ children }: AdminLayoutProps) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate('/admin/login')
  }

  const filteredItems = NAV_ITEMS.filter((item) => {
    if (!item.requiredRoles) return true
    // ADMIN always sees everything
    if (user?.role === 'ADMIN') return true
    return item.requiredRoles.includes(user?.role ?? '')
  })

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
              <p className="text-xs text-muted-foreground">{user?.role}</p>
            </div>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Logout
            </Button>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">{children}</main>
      </div>
    </div>
  )
}
