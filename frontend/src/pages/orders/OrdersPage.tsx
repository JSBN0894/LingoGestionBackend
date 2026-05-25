import { useEffect, useState, useCallback } from 'react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Pagination } from '@/components/Pagination'
import type { Order, CreateOrderRequest, UpdateOrderRequest, Customer } from '@/types/api'
import { Plus, Pencil, Eye } from 'lucide-react'
import { toast } from 'sonner'

const PAGE_SIZE = 20

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [viewOpen, setViewOpen] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [formData, setFormData] = useState<CreateOrderRequest>({
    customerId: 0,
    customerName: '',
    operationStateId: 0,
    orderPrice: 0,
    orderAddress: '',
    orderPhone: '',
    orderCity: '',
    observation: '',
  })

  const fetchOrders = useCallback(async () => {
    try {
      const { data } = await api.get<Order[]>(`/orders?page=${page}&size=${PAGE_SIZE}`)
      setOrders(Array.isArray(data) ? data : [])
      setTotalPages(1)
    } catch {
      toast.error('Failed to load orders')
    } finally {
      setLoading(false)
    }
  }, [page])

  const fetchStates = useCallback(async () => {
    try {
      // States are stored in the states table — we extract unique states from order data
      // since there's no dedicated states API endpoint for admin
    } catch {
      // No states endpoint
    }
  }, [])

  const fetchCustomers = useCallback(async () => {
    try {
      const { data } = await api.get<Customer[]>('/customers')
      setCustomers(data)
    } catch {
      // Customers might not be accessible
    }
  }, [])

  useEffect(() => {
    fetchOrders()
    fetchStates()
    fetchCustomers()
  }, [fetchOrders, fetchStates, fetchCustomers])

  // Extract unique states from loaded orders for the select dropdown
  const uniqueStates = Array.from(
    new Map(orders.map((o) => [o.operationState.id, o.operationState])).values()
  )

  const resetForm = () =>
    setFormData({
      customerId: 0,
      customerName: '',
      operationStateId: 0,
      orderPrice: 0,
      orderAddress: '',
      orderPhone: '',
      orderCity: '',
      observation: '',
    })

  const handleCreate = async () => {
    try {
      await api.post('/orders', formData)
      toast.success('Order created')
      setCreateOpen(false)
      resetForm()
      fetchOrders()
    } catch {
      toast.error('Failed to create order')
    }
  }

  const handleEdit = async () => {
    if (!selectedOrder) return
    try {
      const payload: UpdateOrderRequest = {
        operationStateId: formData.operationStateId,
        orderPrice: formData.orderPrice,
        orderAddress: formData.orderAddress,
        orderPhone: formData.orderPhone,
        orderCity: formData.orderCity,
        observation: formData.observation,
      }
      await api.put(`/orders/${selectedOrder.id}`, payload)
      toast.success('Order updated')
      setEditOpen(false)
      resetForm()
      fetchOrders()
    } catch {
      toast.error('Failed to update order')
    }
  }

  const openEdit = (order: Order) => {
    setSelectedOrder(order)
    setFormData({
      customerId: order.customerId,
      customerName: order.customerName,
      operationStateId: order.operationState.id,
      orderPrice: order.orderPrice,
      orderAddress: order.orderAddress,
      orderPhone: order.orderPhone,
      orderCity: order.orderCity,
      observation: order.observation ?? '',
    })
    setEditOpen(true)
  }

  const openView = (order: Order) => {
    setSelectedOrder(order)
    setViewOpen(true)
  }

  const formatPrice = (value: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)

  const formatDate = (dateStr: string) => new Date(dateStr).toLocaleDateString()

  if (loading) return <div className="text-muted-foreground">Loading orders...</div>

  const OrderForm = ({ isEdit }: { isEdit: boolean }) => (
    <div className="space-y-4">
      {!isEdit && (
        <div>
          <label className="text-sm font-medium">Customer</label>
          <Select
            value={formData.customerId?.toString() ?? ''}
            onValueChange={(v) => {
              const num = Number(v)
              const cust = customers.find((c) => c.cedula === num)
              setFormData({
                ...formData,
                customerId: num,
                customerName: cust?.name ?? formData.customerName,
              })
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="Select customer" />
            </SelectTrigger>
            <SelectContent>
              {customers.map((c) => (
                <SelectItem key={c.cedula} value={c.cedula.toString()}>
                  {c.name} ({c.cedula})
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}
      <div>
        <label className="text-sm font-medium">Status</label>
        <Select
          value={formData.operationStateId?.toString() ?? ''}
          onValueChange={(v) =>
            setFormData({ ...formData, operationStateId: Number(v) })
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="Select status" />
          </SelectTrigger>
          <SelectContent>
            {uniqueStates.map((s: Order['operationState']) => (
              <SelectItem key={s.id} value={s.id.toString()}>
                {s.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-sm font-medium">Price</label>
          <Input
            type="number"
            value={formData.orderPrice}
            onChange={(e) => setFormData({ ...formData, orderPrice: Number(e.target.value) })}
          />
        </div>
        <div>
          <label className="text-sm font-medium">City</label>
          <Input
            value={formData.orderCity}
            onChange={(e) => setFormData({ ...formData, orderCity: e.target.value })}
          />
        </div>
      </div>
      <div>
        <label className="text-sm font-medium">Address</label>
        <Input
          value={formData.orderAddress}
          onChange={(e) => setFormData({ ...formData, orderAddress: e.target.value })}
        />
      </div>
      <div>
        <label className="text-sm font-medium">Phone</label>
        <Input
          value={formData.orderPhone}
          onChange={(e) => setFormData({ ...formData, orderPhone: e.target.value })}
        />
      </div>
      <div>
        <label className="text-sm font-medium">Observation</label>
        <Input
          value={formData.observation}
          onChange={(e) => setFormData({ ...formData, observation: e.target.value })}
        />
      </div>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Orders</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add Order
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Customer</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Price</TableHead>
              <TableHead>Date</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {orders.map((order) => (
              <TableRow key={order.id}>
                <TableCell className="font-medium">#{order.id}</TableCell>
                <TableCell>{order.customerName}</TableCell>
                <TableCell>
                  <Badge variant="outline">{order.operationState.name}</Badge>
                </TableCell>
                <TableCell>{formatPrice(order.orderPrice)}</TableCell>
                <TableCell>{formatDate(order.createdAt)}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openView(order)}>
                      <Eye className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => openEdit(order)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />

      {/* View Detail Dialog */}
      <Dialog open={viewOpen} onOpenChange={setViewOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Order #{selectedOrder?.id}</DialogTitle>
          </DialogHeader>
          {selectedOrder && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-2 text-sm">
                <span className="text-muted-foreground">Customer:</span>
                <span>{selectedOrder.customerName}</span>
                <span className="text-muted-foreground">Status:</span>
                <span><Badge variant="outline">{selectedOrder.operationState.name}</Badge></span>
                <span className="text-muted-foreground">Price:</span>
                <span>{formatPrice(selectedOrder.orderPrice)}</span>
                <span className="text-muted-foreground">Address:</span>
                <span>{selectedOrder.orderAddress}</span>
                <span className="text-muted-foreground">City:</span>
                <span>{selectedOrder.orderCity}</span>
                <span className="text-muted-foreground">Phone:</span>
                <span>{selectedOrder.orderPhone}</span>
                {selectedOrder.observation && (
                  <>
                    <span className="text-muted-foreground">Observation:</span>
                    <span>{selectedOrder.observation}</span>
                  </>
                )}
                <span className="text-muted-foreground">Created:</span>
                <span>{new Date(selectedOrder.createdAt).toLocaleString()}</span>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button onClick={() => setViewOpen(false)}>Close</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Create Order</DialogTitle>
            <DialogDescription>Add a new order.</DialogDescription>
          </DialogHeader>
          <OrderForm isEdit={false} />
          <DialogFooter>
            <Button variant="outline" onClick={() => { setCreateOpen(false); resetForm() }}>
              Cancel
            </Button>
            <Button onClick={handleCreate}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Edit Order #{selectedOrder?.id}</DialogTitle>
          </DialogHeader>
          <OrderForm isEdit />
          <DialogFooter>
            <Button variant="outline" onClick={() => { setEditOpen(false); resetForm() }}>
              Cancel
            </Button>
            <Button onClick={handleEdit}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
