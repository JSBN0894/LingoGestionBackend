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
import type { Carrier, CreateCarrierRequest, UpdateCarrierRequest } from '@/types/api'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

export function CarriersPage() {
  const [carriers, setCarriers] = useState<Carrier[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [selectedCarrier, setSelectedCarrier] = useState<Carrier | null>(null)
  const [formData, setFormData] = useState<CreateCarrierRequest & { active: boolean }>({
    name: '',
    contactPhone: '',
    active: true,
  })

  const fetchCarriers = useCallback(async () => {
    try {
      const { data } = await api.get<Carrier[]>('/carriers?onlyActive=false')
      setCarriers(Array.isArray(data) ? data : [])
    } catch {
      toast.error('No se pudieron cargar las transportadoras')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCarriers()
  }, [fetchCarriers])

  const resetForm = () => setFormData({ name: '', contactPhone: '', active: true })

  const handleCreate = async () => {
    try {
      const payload: CreateCarrierRequest = { name: formData.name, contactPhone: formData.contactPhone || undefined }
      await api.post('/carriers', payload)
      toast.success('Transportadora creada')
      setCreateOpen(false)
      resetForm()
      fetchCarriers()
    } catch {
      toast.error('No se pudo crear la transportadora')
    }
  }

  const handleEdit = async () => {
    if (!selectedCarrier) return
    try {
      const payload: UpdateCarrierRequest = {
        name: formData.name,
        contactPhone: formData.contactPhone || undefined,
        active: formData.active,
      }
      await api.put(`/carriers/${selectedCarrier.id}`, payload)
      toast.success('Transportadora actualizada')
      setEditOpen(false)
      resetForm()
      fetchCarriers()
    } catch {
      toast.error('No se pudo actualizar la transportadora')
    }
  }

  const handleDelete = async () => {
    if (!selectedCarrier) return
    try {
      await api.delete(`/carriers/${selectedCarrier.id}`)
      toast.success('Transportadora eliminada')
      setDeleteOpen(false)
      fetchCarriers()
    } catch {
      toast.error('No se pudo eliminar la transportadora')
    }
  }

  const openEdit = (carrier: Carrier) => {
    setSelectedCarrier(carrier)
    setFormData({ name: carrier.name, contactPhone: carrier.contactPhone ?? '', active: carrier.active })
    setEditOpen(true)
  }

  if (loading) return <div className="text-muted-foreground">Cargando transportadoras...</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Transportadoras</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Agregar transportadora
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Nombre</TableHead>
              <TableHead>Teléfono de contacto</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead className="text-right">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {carriers.map((carrier) => (
              <TableRow key={carrier.id}>
                <TableCell className="font-medium">{carrier.id}</TableCell>
                <TableCell>{carrier.name}</TableCell>
                <TableCell>{carrier.contactPhone ?? '—'}</TableCell>
                <TableCell>
                  <Badge variant={carrier.active ? 'default' : 'outline'}>
                    {carrier.active ? 'Activa' : 'Inactiva'}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(carrier)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedCarrier(carrier)
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
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Crear transportadora</DialogTitle>
            <DialogDescription>Agrega una nueva empresa de transporte.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Nombre</label>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Teléfono de contacto</label>
              <Input
                value={formData.contactPhone}
                onChange={(e) => setFormData({ ...formData, contactPhone: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => { setCreateOpen(false); resetForm() }}>
              Cancelar
            </Button>
            <Button onClick={handleCreate}>Crear</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Editar transportadora</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Nombre</label>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Teléfono de contacto</label>
              <Input
                value={formData.contactPhone}
                onChange={(e) => setFormData({ ...formData, contactPhone: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Estado</label>
              <Select
                value={formData.active ? 'true' : 'false'}
                onValueChange={(v) => setFormData({ ...formData, active: v === 'true' })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">Activa</SelectItem>
                  <SelectItem value="false">Inactiva</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => { setEditOpen(false); resetForm() }}>
              Cancelar
            </Button>
            <Button onClick={handleEdit}>Guardar</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Eliminar transportadora</DialogTitle>
            <DialogDescription>
              ¿Seguro que quieres eliminar &quot;{selectedCarrier?.name}&quot;?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteOpen(false)}>
              Cancelar
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Eliminar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
