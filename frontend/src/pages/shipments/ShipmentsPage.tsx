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
import type { Shipment, ShipmentState, Order, CreateShipmentRequest, UpdateShipmentRequest, AssignGuideRequest } from '@/types/api'
import { Plus, Pencil, Tag } from 'lucide-react'
import { toast } from 'sonner'

export function ShipmentsPage() {
  const [shipments, setShipments] = useState<Shipment[]>([])
  const [shipmentStates, setShipmentStates] = useState<ShipmentState[]>([])
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [guideOpen, setGuideOpen] = useState(false)
  const [selectedShipment, setSelectedShipment] = useState<Shipment | null>(null)
  const [formData, setFormData] = useState<CreateShipmentRequest>({
    orderId: 0,
    shippingStateId: 0,
    carrier: 'Inter rapidisimo',
    isCashOnDelivery: true,
  })
  const [guideNumber, setGuideNumber] = useState('')

  const fetchShipments = useCallback(async () => {
    try {
      const { data } = await api.get<Shipment[]>('/shipments')
      setShipments(Array.isArray(data) ? data : [])
    } catch {
      toast.error('Failed to load shipments')
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchShipmentStates = useCallback(async () => {
    try {
      const { data } = await api.get<ShipmentState[]>('/shipment-states')
      setShipmentStates(Array.isArray(data) ? data : [])
    } catch {
      // No shipment states endpoint available
    }
  }, [])

  const fetchOrders = useCallback(async () => {
    try {
      const { data } = await api.get<Order[]>('/orders?page=0&size=100')
      setOrders(Array.isArray(data) ? data : [])
    } catch {
      // Orders might not be accessible
    }
  }, [])

  useEffect(() => {
    fetchShipments()
    fetchShipmentStates()
    fetchOrders()
  }, [fetchShipments, fetchShipmentStates, fetchOrders])

  const resetForm = () =>
    setFormData({ orderId: 0, shippingStateId: 0, carrier: 'Inter rapidisimo', isCashOnDelivery: true })

  const handleCreate = async () => {
    try {
      await api.post('/shipments', formData)
      toast.success('Shipment created')
      setCreateOpen(false)
      resetForm()
      fetchShipments()
    } catch {
      toast.error('Failed to create shipment')
    }
  }

  const handleEdit = async () => {
    if (!selectedShipment) return
    try {
      const payload: UpdateShipmentRequest = {
        shippingStateId: formData.shippingStateId,
        carrier: formData.carrier,
        isCashOnDelivery: formData.isCashOnDelivery,
        shippingCost: formData.shippingCost,
        estimateDeliveryDate: formData.estimateDeliveryDate,
        weight: formData.weight,
      }
      await api.put(`/shipments/${selectedShipment.id}`, payload)
      toast.success('Shipment updated')
      setEditOpen(false)
      resetForm()
      fetchShipments()
    } catch {
      toast.error('Failed to update shipment')
    }
  }

  const handleAssignGuide = async () => {
    if (!selectedShipment) return
    try {
      const payload: AssignGuideRequest = { guideNumber }
      await api.patch(`/shipments/${selectedShipment.id}/guide`, payload)
      toast.success('Guide number assigned')
      setGuideOpen(false)
      setGuideNumber('')
      fetchShipments()
    } catch {
      toast.error('Failed to assign guide')
    }
  }

  const openEdit = (shipment: Shipment) => {
    setSelectedShipment(shipment)
    setFormData({
      orderId: shipment.orderId,
      shippingStateId: shipment.shippingState?.id ?? 0,
      carrier: shipment.carrier,
      isCashOnDelivery: shipment.isCashOnDelivery,
      shippingCost: shipment.shippingCost ?? undefined,
      estimateDeliveryDate: shipment.estimateDeliveryDate ?? undefined,
      weight: shipment.weight ?? undefined,
    })
    setEditOpen(true)
  }

  if (loading) return <div className="text-muted-foreground">Loading shipments...</div>

  const ShipmentForm = ({ isEdit }: { isEdit: boolean }) => (
    <div className="space-y-4">
      {!isEdit && (
        <div>
          <label className="text-sm font-medium">Order</label>
          <Select
            value={formData.orderId?.toString() ?? ''}
            onValueChange={(v) => setFormData({ ...formData, orderId: Number(v) })}
          >
            <SelectTrigger>
              <SelectValue placeholder="Select order" />
            </SelectTrigger>
            <SelectContent>
              {orders
                .filter((o) => !shipments.some((s) => s.orderId === o.id))
                .map((o) => (
                  <SelectItem key={o.id} value={o.id.toString()}>
                    #{o.id} — {o.customerName}
                  </SelectItem>
                ))}
            </SelectContent>
          </Select>
        </div>
      )}
      <div>
        <label className="text-sm font-medium">Shipping State</label>
        <Select
          value={formData.shippingStateId?.toString() ?? ''}
          onValueChange={(v) => setFormData({ ...formData, shippingStateId: Number(v) })}
        >
          <SelectTrigger>
            <SelectValue placeholder="Select state" />
          </SelectTrigger>
          <SelectContent>
            {shipmentStates.map((s) => (
              <SelectItem key={s.id} value={s.id.toString()}>
                {s.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div>
        <label className="text-sm font-medium">Carrier</label>
        <Input
          value={formData.carrier ?? ''}
          onChange={(e) => setFormData({ ...formData, carrier: e.target.value })}
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-sm font-medium">Shipping Cost</label>
          <Input
            type="number"
            value={formData.shippingCost ?? ''}
            onChange={(e) =>
              setFormData({ ...formData, shippingCost: e.target.value ? Number(e.target.value) : undefined })
            }
          />
        </div>
        <div>
          <label className="text-sm font-medium">Weight (g)</label>
          <Input
            type="number"
            value={formData.weight ?? ''}
            onChange={(e) =>
              setFormData({ ...formData, weight: e.target.value ? Number(e.target.value) : undefined })
            }
          />
        </div>
      </div>
      <div>
        <label className="text-sm font-medium">Est. Delivery Date</label>
        <Input
          type="datetime-local"
          value={formData.estimateDeliveryDate ?? ''}
          onChange={(e) =>
            setFormData({ ...formData, estimateDeliveryDate: e.target.value || undefined })
          }
        />
      </div>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Shipments</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add Shipment
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Order</TableHead>
              <TableHead>Carrier</TableHead>
              <TableHead>Guide</TableHead>
              <TableHead>State</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {shipments.map((shipment) => (
              <TableRow key={shipment.id}>
                <TableCell className="font-medium">#{shipment.id}</TableCell>
                <TableCell>#{shipment.orderId}</TableCell>
                <TableCell>{shipment.carrier}</TableCell>
                <TableCell>
                  {shipment.guideNumber ? (
                    <Badge variant="outline">{shipment.guideNumber}</Badge>
                  ) : (
                    <span className="text-muted-foreground">—</span>
                  )}
                </TableCell>
                <TableCell>
                  <Badge variant="outline">
                    {shipment.shippingState?.name ?? 'Unknown'}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedShipment(shipment)
                        setGuideOpen(true)
                      }}
                      title="Assign guide"
                    >
                      <Tag className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => openEdit(shipment)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Create Dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Create Shipment</DialogTitle>
            <DialogDescription>Create a new shipment for an order.</DialogDescription>
          </DialogHeader>
          <ShipmentForm isEdit={false} />
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
            <DialogTitle>Edit Shipment #{selectedShipment?.id}</DialogTitle>
          </DialogHeader>
          <ShipmentForm isEdit />
          <DialogFooter>
            <Button variant="outline" onClick={() => { setEditOpen(false); resetForm() }}>
              Cancel
            </Button>
            <Button onClick={handleEdit}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Assign Guide Dialog */}
      <Dialog open={guideOpen} onOpenChange={setGuideOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Assign Guide Number</DialogTitle>
            <DialogDescription>
              Set the guide number for shipment #{selectedShipment?.id}.
            </DialogDescription>
          </DialogHeader>
          <Input
            value={guideNumber}
            onChange={(e) => setGuideNumber(e.target.value)}
            placeholder="Guide number"
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => { setGuideOpen(false); setGuideNumber('') }}>
              Cancel
            </Button>
            <Button onClick={handleAssignGuide}>Assign</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
