/** API response and request types for the admin panel. */

// --- Auth ---
export interface User {
  id: string
  username: string
  email: string
  fullName: string
  role: string
  isEnabled: boolean
}

export interface LoginRequest {
  username: string
  password: string
}

export interface CreateUserRequest {
  fullName: string
  username: string
  email: string
  password: string
  role: string
}

export interface UpdateRoleRequest {
  role: string
}

// --- Dashboard ---
export interface SalesSummary {
  totalOrders: number
  totalRevenue: number
  avgOrderValue: number
  totalProducts: number
  totalUsers: number
  ordersByState: StateCount[]
}

export interface StateCount {
  state: string
  count: number
}

// --- Products ---
export interface Product {
  id: number
  name: string
  pricePerUnit: number
  stock: number
  imageUrl: string
  description: string
  category: Category | null
  createdAt: string
  updatedAt: string
}

export interface CreateProductRequest {
  name: string
  pricePerUnit: number
  stock: number
  imageUrl?: string
  description: string
  categoryId?: number
}

export interface UpdateProductRequest {
  name: string
  pricePerUnit: number
  stock: number
  imageUrl?: string
  description: string
  categoryId?: number
}

// --- Categories ---
export interface Category {
  id: number
  name: string
  parentId: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateCategoryRequest {
  name: string
  parentId?: number
}

export interface UpdateCategoryRequest {
  name: string
  parentId?: number
}

// --- Customers ---
export interface Customer {
  cedula: number
  name: string
  phones: string[]
  addresses: string[]
}

export interface CreateCustomerRequest {
  cedula: number
  name: string
  phones: string[]
  addresses: string[]
}

// --- Orders ---
export interface Order {
  id: number
  customerId: number
  customerName: string
  operationState: OrderState
  orderPrice: number
  orderAddress: string
  orderPhone: string
  orderCity: string
  observation: string | null
  createdAt: string
  updatedAt: string
}

export interface OrderState {
  id: number
  name: string
  priority: number
  type: string
}

export interface CreateOrderRequest {
  customerId: number
  customerName: string
  operationStateId: number
  orderPrice: number
  orderAddress: string
  orderPhone: string
  orderCity: string
  observation?: string
}

export interface UpdateOrderRequest {
  operationStateId: number
  orderPrice: number
  orderAddress: string
  orderPhone: string
  orderCity: string
  observation?: string
}

// --- Shipments ---
export interface Shipment {
  id: number
  orderId: number
  orderCustomerName: string
  carrier: string
  guideNumber: string | null
  shippingState: ShipmentState | null
  isCashOnDelivery: boolean
  shippingCost: number | null
  estimateDeliveryDate: string | null
  weight: number | null
  createdAt: string
  updatedAt: string
}

export interface ShipmentState {
  id: number
  name: string
  state: StateSummary
}

export interface StateSummary {
  id: number
  name: string
}

export interface CreateShipmentRequest {
  orderId: number
  shippingStateId: number
  carrier?: string
  isCashOnDelivery?: boolean
  shippingCost?: number
  estimateDeliveryDate?: string
  weight?: number
}

export interface UpdateShipmentRequest {
  shippingStateId: number
  carrier?: string
  isCashOnDelivery?: boolean
  shippingCost?: number
  estimateDeliveryDate?: string
  weight?: number
}

export interface AssignGuideRequest {
  guideNumber: string
}

// --- States ---
export interface State {
  id: number
  name: string
  priority: number
  type: string
  createdAt: string
  updatedAt: string
}

export interface CreateStateRequest {
  id: number
  name: string
  priority: number
  type?: string
}

export interface UpdateStateRequest {
  name: string
  priority: number
  type?: string
}

// --- Audit Log ---
export interface AuditEntry {
  id: number
  timestamp: string
  username: string
  action: string
  entityType: string
  entityId: string
  ipAddress: string
  details: string | null
}

export interface AuditFilters {
  username?: string
  entityType?: string
  action?: string
  fromDate?: string
  toDate?: string
  entityId?: string
  page?: number
  size?: number
}

// --- Upload ---
export interface UploadResponse {
  url: string
}

// --- Pagination ---
export interface PaginatedResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
}
