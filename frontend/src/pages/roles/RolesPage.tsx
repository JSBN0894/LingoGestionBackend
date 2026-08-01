import { useEffect, useState, useCallback, useMemo } from 'react'
import { api } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
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
import { Badge } from '@/components/ui/badge'
import type { Role, Permission, RoleRequest } from '@/types/api'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

const emptyForm: RoleRequest = { name: '', description: '', permissions: [] }

export function RolesPage() {
  const [roles, setRoles] = useState<Role[]>([])
  const [permissions, setPermissions] = useState<Permission[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [selectedRole, setSelectedRole] = useState<Role | null>(null)
  const [formData, setFormData] = useState<RoleRequest>(emptyForm)

  const fetchData = useCallback(async () => {
    try {
      const [rolesRes, permissionsRes] = await Promise.all([
        api.get<Role[]>('/admin/roles'),
        api.get<Permission[]>('/admin/permissions'),
      ])
      setRoles(rolesRes.data)
      setPermissions(permissionsRes.data)
    } catch {
      toast.error('Failed to load roles')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const permissionGroups = useMemo(() => {
    const groups = new Map<string, Permission[]>()
    for (const perm of permissions) {
      const prefix = perm.code.split('_')[0]
      const group = groups.get(prefix) ?? []
      group.push(perm)
      groups.set(prefix, group)
    }
    return Array.from(groups.entries()).sort(([a], [b]) => a.localeCompare(b))
  }, [permissions])

  const togglePermission = (code: string) => {
    setFormData((prev) => ({
      ...prev,
      permissions: prev.permissions.includes(code)
        ? prev.permissions.filter((p) => p !== code)
        : [...prev.permissions, code],
    }))
  }

  const handleCreate = async () => {
    try {
      await api.post('/admin/roles', formData)
      setCreateOpen(false)
      setFormData(emptyForm)
      await fetchData()
      toast.success('Role created successfully')
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      toast.error(axiosErr.response?.data?.message ?? 'Failed to create role')
    }
  }

  const handleUpdate = async () => {
    if (!selectedRole) return
    try {
      await api.put(`/admin/roles/${selectedRole.id}`, formData)
      setEditOpen(false)
      await fetchData()
      toast.success('Role updated successfully')
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      toast.error(axiosErr.response?.data?.message ?? 'Failed to update role')
    }
  }

  const handleDelete = async () => {
    if (!selectedRole) return
    try {
      await api.delete(`/admin/roles/${selectedRole.id}`)
      toast.success('Role deleted')
      setDeleteOpen(false)
      await fetchData()
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      toast.error(axiosErr.response?.data?.message ?? 'Failed to delete role')
    }
  }

  const openEdit = (role: Role) => {
    setSelectedRole(role)
    setFormData({ name: role.name, description: role.description ?? '', permissions: [...role.permissions] })
    setEditOpen(true)
  }

  const renderPermissionMatrix = () => (
    <div className="space-y-4 max-h-96 overflow-y-auto rounded-md border p-3">
      {permissionGroups.map(([group, perms]) => (
        <div key={group}>
          <p className="mb-2 text-xs font-semibold uppercase text-muted-foreground">{group}</p>
          <div className="space-y-2">
            {perms.map((perm) => (
              <label key={perm.code} className="flex items-start gap-2 text-sm">
                <Checkbox
                  checked={formData.permissions.includes(perm.code)}
                  onCheckedChange={() => togglePermission(perm.code)}
                  className="mt-0.5"
                />
                <span>
                  <span className="font-medium">{perm.code}</span>
                  <span className="block text-xs text-muted-foreground">{perm.description}</span>
                </span>
              </label>
            ))}
          </div>
        </div>
      ))}
    </div>
  )

  if (loading) return <div className="text-muted-foreground">Loading roles...</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Roles & Permissions</h1>
        <Button onClick={() => { setFormData(emptyForm); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add Role
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Permissions</TableHead>
              <TableHead>Users</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {roles.map((role) => (
              <TableRow key={role.id}>
                <TableCell className="font-medium">{role.name}</TableCell>
                <TableCell className="text-muted-foreground">{role.description || '—'}</TableCell>
                <TableCell>
                  <Badge variant="outline">{role.permissions.length} permission{role.permissions.length === 1 ? '' : 's'}</Badge>
                </TableCell>
                <TableCell>{role.userCount}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(role)} title="Edit role">
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedRole(role)
                        setDeleteOpen(true)
                      }}
                      title="Delete role"
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

      {/* Create Role Dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Role</DialogTitle>
            <DialogDescription>Define a role and the permissions it grants.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Name</label>
              <Input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} />
            </div>
            <div>
              <label className="text-sm font-medium">Description</label>
              <Input
                value={formData.description ?? ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Permissions</label>
              {renderPermissionMatrix()}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreate}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Role Dialog */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Role</DialogTitle>
            <DialogDescription>Update {selectedRole?.name}'s name, description and permissions.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Name</label>
              <Input value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} />
            </div>
            <div>
              <label className="text-sm font-medium">Description</label>
              <Input
                value={formData.description ?? ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Permissions</label>
              {renderPermissionMatrix()}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdate}>Update</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Role</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete {selectedRole?.name}? This fails if any user still has this role assigned.
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
