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
import type { Customer, CreateCustomerRequest } from '@/types/api'
import { Plus, Pencil, Trash2, X } from 'lucide-react'
import { toast } from 'sonner'

export function CustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)
  const [formData, setFormData] = useState<CreateCustomerRequest>({
    cedula: 0,
    name: '',
    phones: [''],
    addresses: [''],
  })

  const fetchCustomers = useCallback(async () => {
    try {
      const { data } = await api.get<Customer[]>('/customers')
      setCustomers(data)
    } catch {
      toast.error('Failed to load customers')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCustomers()
  }, [fetchCustomers])

  const resetForm = () =>
    setFormData({ cedula: 0, name: '', phones: [''], addresses: [''] })

  const handleCreate = async () => {
    try {
      const payload = {
        ...formData,
        phones: formData.phones.filter((p) => p.trim()),
        addresses: formData.addresses.filter((a) => a.trim()),
      }
      await api.post('/customers', payload)
      toast.success('Customer created')
      setCreateOpen(false)
      resetForm()
      fetchCustomers()
    } catch {
      toast.error('Failed to create customer')
    }
  }

  const handleEdit = async () => {
    if (!selectedCustomer) return
    try {
      const payload = {
        cedula: selectedCustomer.cedula,
        name: formData.name,
        phones: formData.phones.filter((p) => p.trim()),
        addresses: formData.addresses.filter((a) => a.trim()),
      }
      await api.put(`/customers/${selectedCustomer.cedula}`, payload)
      toast.success('Customer updated')
      setEditOpen(false)
      resetForm()
      fetchCustomers()
    } catch {
      toast.error('Failed to update customer')
    }
  }

  const handleDelete = async () => {
    if (!selectedCustomer) return
    try {
      await api.delete(`/customers/${selectedCustomer.cedula}`)
      toast.success('Customer deleted')
      setDeleteOpen(false)
      fetchCustomers()
    } catch {
      toast.error('Failed to delete customer')
    }
  }

  const openEdit = (customer: Customer) => {
    setSelectedCustomer(customer)
    setFormData({
      cedula: customer.cedula,
      name: customer.name,
      phones: customer.phones.length > 0 ? customer.phones : [''],
      addresses: customer.addresses.length > 0 ? customer.addresses : [''],
    })
    setEditOpen(true)
  }

  const addPhone = () => setFormData({ ...formData, phones: [...formData.phones, ''] })
  const removePhone = (idx: number) =>
    setFormData({ ...formData, phones: formData.phones.filter((_, i) => i !== idx) })
  const updatePhone = (idx: number, value: string) => {
    const updated = [...formData.phones]
    updated[idx] = value
    setFormData({ ...formData, phones: updated })
  }

  const addAddress = () => setFormData({ ...formData, addresses: [...formData.addresses, ''] })
  const removeAddress = (idx: number) =>
    setFormData({ ...formData, addresses: formData.addresses.filter((_, i) => i !== idx) })
  const updateAddress = (idx: number, value: string) => {
    const updated = [...formData.addresses]
    updated[idx] = value
    setFormData({ ...formData, addresses: updated })
  }

  if (loading) return <div className="text-muted-foreground">Loading customers...</div>

  const CustomerForm = () => (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-sm font-medium">Cedula</label>
          <Input
            type="number"
            value={formData.cedula || ''}
            onChange={(e) => setFormData({ ...formData, cedula: Number(e.target.value) })}
            disabled={!!selectedCustomer}
          />
        </div>
        <div>
          <label className="text-sm font-medium">Name</label>
          <Input
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          />
        </div>
      </div>

      <div>
        <label className="text-sm font-medium">Phones</label>
        <div className="space-y-2">
          {formData.phones.map((phone, idx) => (
            <div key={idx} className="flex gap-2">
              <Input
                value={phone}
                onChange={(e) => updatePhone(idx, e.target.value)}
                placeholder="Phone number"
              />
              {formData.phones.length > 1 && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => removePhone(idx)}
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
          ))}
          <Button variant="outline" size="sm" onClick={addPhone}>
            Add Phone
          </Button>
        </div>
      </div>

      <div>
        <label className="text-sm font-medium">Addresses</label>
        <div className="space-y-2">
          {formData.addresses.map((address, idx) => (
            <div key={idx} className="flex gap-2">
              <Input
                value={address}
                onChange={(e) => updateAddress(idx, e.target.value)}
                placeholder="Address"
              />
              {formData.addresses.length > 1 && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => removeAddress(idx)}
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
          ))}
          <Button variant="outline" size="sm" onClick={addAddress}>
            Add Address
          </Button>
        </div>
      </div>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Customers</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add Customer
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Cedula</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Phones</TableHead>
              <TableHead>Addresses</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {customers.map((customer) => (
              <TableRow key={customer.cedula}>
                <TableCell className="font-medium">{customer.cedula}</TableCell>
                <TableCell>{customer.name}</TableCell>
                <TableCell>{customer.phones.join(', ') || '—'}</TableCell>
                <TableCell>{customer.addresses.join(', ') || '—'}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(customer)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedCustomer(customer)
                        setDeleteOpen(true)
                      }}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
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
            <DialogTitle>Create Customer</DialogTitle>
            <DialogDescription>Add a new customer.</DialogDescription>
          </DialogHeader>
          <CustomerForm />
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
            <DialogTitle>Edit Customer</DialogTitle>
          </DialogHeader>
          <CustomerForm />
          <DialogFooter>
            <Button variant="outline" onClick={() => { setEditOpen(false); resetForm() }}>
              Cancel
            </Button>
            <Button onClick={handleEdit}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Customer</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete &quot;{selectedCustomer?.name}&quot;?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
