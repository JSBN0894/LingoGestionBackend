/**
 * Fuente única de verdad para qué permisos necesita cada página del panel.
 * Reemplaza los arrays `requiredRoles` duplicados que antes vivían por
 * separado en App.tsx y AdminLayout.tsx.
 */
export const PAGE_PERMISSIONS: Record<string, string[]> = {
  '/admin/dashboard': ['DASHBOARD_VIEW'],
  '/admin/users': ['USERS_MANAGE'],
  '/admin/roles': ['ROLES_MANAGE'],
  '/admin/products': ['PRODUCTS_MANAGE', 'ORDERS_MANAGE'],
  '/admin/categories': ['CATEGORIES_MANAGE'],
  '/admin/customers': ['CUSTOMERS_MANAGE', 'CUSTOMERS_DELETE'],
  '/admin/orders': ['ORDERS_MANAGE', 'ORDERS_DELETE', 'ORDERS_SHIP'],
  '/admin/shipments': ['SHIPMENTS_MANAGE', 'SHIPMENTS_DELETE'],
  '/admin/carriers': ['CARRIERS_MANAGE', 'CARRIERS_DELETE'],
  '/admin/states': ['STATES_MANAGE'],
  '/admin/audit': ['AUDIT_VIEW'],
}

/** True si el usuario tiene al menos uno de los permisos requeridos. */
export function hasAnyPermission(userPermissions: string[] | undefined, required: string[]): boolean {
  if (!required || required.length === 0) return true
  return required.some((p) => userPermissions?.includes(p))
}
