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
import type { State, CreateStateRequest, UpdateStateRequest } from '@/types/api'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

export function StatesPage() {
  const [states, setStates] = useState<State[]>([])
  const [loading, setLoading] = useState(true)
  const [noEndpoint, setNoEndpoint] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [selectedState, setSelectedState] = useState<State | null>(null)
  const [formData, setFormData] = useState<CreateStateRequest>({
    name: '',
    priority: 0,
    type: 'OPERATION',
  })

  const fetchStates = useCallback(async () => {
    try {
      const { data } = await api.get<State[]>('/states')
      setStates(data)
    } catch {
      setNoEndpoint(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchStates()
  }, [fetchStates])

  const resetForm = () => setFormData({ name: '', priority: 0, type: 'OPERATION' })

  const handleCreate = async () => {
    try {
      await api.post('/states', formData)
      toast.success('State created')
      setCreateOpen(false)
      resetForm()
      fetchStates()
    } catch {
      toast.error('Failed to create state')
    }
  }

  const handleEdit = async () => {
    if (!selectedState) return
    try {
      const payload: UpdateStateRequest = {
        name: formData.name,
        priority: formData.priority,
        type: formData.type,
      }
      await api.put(`/states/${selectedState.id}`, payload)
      toast.success('State updated')
      setEditOpen(false)
      resetForm()
      fetchStates()
    } catch {
      toast.error('Failed to update state')
    }
  }

  const handleDelete = async () => {
    if (!selectedState) return
    try {
      await api.delete(`/states/${selectedState.id}`)
      toast.success('State deleted')
      setDeleteOpen(false)
      fetchStates()
    } catch {
      toast.error('Failed to delete state')
    }
  }

  const openEdit = (state: State) => {
    setSelectedState(state)
    setFormData({ name: state.name, priority: state.priority, type: state.type })
    setEditOpen(true)
  }

  if (loading) return <div className="text-muted-foreground">Loading states...</div>

  if (noEndpoint) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold">States</h1>
        <div className="rounded-md border p-6 text-center">
          <p className="text-muted-foreground">
            The /api/states endpoint is not available on the backend.
            States are managed through the database directly.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">States</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add State
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Priority</TableHead>
              <TableHead>Type</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {states.map((state) => (
              <TableRow key={state.id}>
                <TableCell className="font-medium">{state.id}</TableCell>
                <TableCell>{state.name}</TableCell>
                <TableCell>{state.priority}</TableCell>
                <TableCell>{state.type}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(state)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedState(state)
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
            <DialogTitle>Create State</DialogTitle>
            <DialogDescription>Add a new state.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Name</label>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Priority</label>
              <Input
                type="number"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: Number(e.target.value) })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Type</label>
              <Input
                value={formData.type ?? 'OPERATION'}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
              />
            </div>
          </div>
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
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit State</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Name</label>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Priority</label>
              <Input
                type="number"
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: Number(e.target.value) })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Type</label>
              <Input
                value={formData.type ?? 'OPERATION'}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
              />
            </div>
          </div>
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
            <DialogTitle>Delete State</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete &quot;{selectedState?.name}&quot;?
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
