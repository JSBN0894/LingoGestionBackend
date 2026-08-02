import { useEffect, useState, useCallback } from 'react'
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
import type { User, CreateUserRequest, Role } from '@/types/api'
import { Plus, Pencil, Trash2, Power, KeyRound } from 'lucide-react'
import { toast } from 'sonner'

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [roles, setRoles] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [editRolesOpen, setEditRolesOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [resetPasswordOpen, setResetPasswordOpen] = useState(false)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [editingRoleIds, setEditingRoleIds] = useState<number[]>([])
  const [formData, setFormData] = useState<CreateUserRequest>({
    fullName: '',
    username: '',
    email: '',
    password: '',
    roleIds: [],
  })

  const fetchUsers = useCallback(async () => {
    try {
      const { data } = await api.get<User[]>('/admin/users')
      setUsers(data)
    } catch {
      toast.error('Failed to load users')
    } finally {
      setLoading(false)
    }
  }, [])

  const fetchRoles = useCallback(async () => {
    try {
      const { data } = await api.get<Role[]>('/admin/roles')
      setRoles(data)
    } catch {
      toast.error('Failed to load roles')
    }
  }, [])

  useEffect(() => {
    fetchUsers()
    fetchRoles()
  }, [fetchUsers, fetchRoles])

  const toggleRoleId = (roleIds: number[], roleId: number): number[] =>
    roleIds.includes(roleId) ? roleIds.filter((id) => id !== roleId) : [...roleIds, roleId]

  const handleCreate = async () => {
    try {
      await api.post('/admin/users', formData)
      setCreateOpen(false)
      setFormData({ fullName: '', username: '', email: '', password: '', roleIds: [] })
      await fetchUsers()
      toast.success('User created successfully')
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string; details?: Array<{ field: string; message: string }> } } }
      const details = axiosErr.response?.data?.details
      if (details && details.length > 0) {
        const messages = details.map((d) => `${d.field}: ${d.message}`)
        toast.error(messages.join('\n'))
      } else {
        toast.error(axiosErr.response?.data?.message ?? 'Failed to create user')
      }
    }
  }

  const handleRolesChange = async () => {
    if (!selectedUser) return
    try {
      await api.put(`/admin/users/${selectedUser.id}/roles`, { roleIds: editingRoleIds })
      toast.success('Roles updated')
      setEditRolesOpen(false)
      fetchUsers()
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      toast.error(axiosErr.response?.data?.message ?? 'Failed to update roles')
    }
  }

  const handleToggleStatus = async (user: User) => {
    try {
      await api.patch(`/admin/users/${user.id}/status`)
      toast.success(`User ${user.isEnabled ? 'disabled' : 'enabled'}`)
      fetchUsers()
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      toast.error(axiosErr.response?.data?.message ?? 'Failed to toggle status')
    }
  }

  const handleDelete = async () => {
    if (!selectedUser) return
    try {
      await api.delete(`/admin/users/${selectedUser.id}`)
      toast.success('User deleted')
      setDeleteOpen(false)
      await fetchUsers()
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      const msg = axiosErr.response?.data?.message
      if (msg?.includes('not found')) {
        // User was already deleted (race condition or stale list)
        toast.info('User was already removed. List refreshed.')
        setDeleteOpen(false)
        await fetchUsers()
      } else {
        toast.error(msg ?? 'Failed to delete user')
      }
    }
  }

  const handleResetPassword = async () => {
    if (!selectedUser) return
    if (newPassword !== confirmPassword) {
      toast.error('Passwords do not match')
      return
    }
    if (newPassword.length < 8) {
      toast.error('Password must be at least 8 characters')
      return
    }
    try {
      await api.patch(`/admin/users/${selectedUser.id}/password`, { newPassword })
      toast.success(`Password reset for ${selectedUser.username}`)
      setResetPasswordOpen(false)
      setNewPassword('')
      setConfirmPassword('')
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      toast.error(axiosErr.response?.data?.message ?? 'Failed to reset password')
    }
  }

  if (loading) return <div className="text-muted-foreground">Loading users...</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Users</h1>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Add User
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Username</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Full Name</TableHead>
              <TableHead>Roles</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell className="font-medium">{user.username}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.fullName}</TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {user.roles.length === 0 ? (
                      <span className="text-xs text-muted-foreground">No roles</span>
                    ) : (
                      user.roles.map((role) => (
                        <Badge key={role.id} variant="outline">
                          {role.name}
                        </Badge>
                      ))
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <Badge variant={user.isEnabled ? 'default' : 'destructive'}>
                    {user.isEnabled ? 'Active' : 'Disabled'}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedUser(user)
                        setEditingRoleIds(user.roles.map((r) => r.id))
                        setEditRolesOpen(true)
                      }}
                      title="Change roles"
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleToggleStatus(user)}
                      title={user.isEnabled ? 'Disable' : 'Enable'}
                    >
                      <Power className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedUser(user)
                        setNewPassword('')
                        setConfirmPassword('')
                        setResetPasswordOpen(true)
                      }}
                      title="Reset password"
                    >
                      <KeyRound className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedUser(user)
                        setDeleteOpen(true)
                      }}
                      title="Delete"
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

      {/* Create User Dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create User</DialogTitle>
            <DialogDescription>Add a new user to the system.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Full Name</label>
              <Input
                value={formData.fullName}
                onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Username</label>
              <Input
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Email</label>
              <Input
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Password</label>
              <Input
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>
            <div>
              <label className="text-sm font-medium">Roles</label>
              <div className="space-y-2 rounded-md border p-3">
                {roles.length === 0 && (
                  <p className="text-xs text-muted-foreground">No roles exist yet. Create one from Roles & Permissions.</p>
                )}
                {roles.map((role) => (
                  <label key={role.id} className="flex items-center gap-2 text-sm">
                    <Checkbox
                      checked={formData.roleIds.includes(role.id)}
                      onCheckedChange={() =>
                        setFormData({ ...formData, roleIds: toggleRoleId(formData.roleIds, role.id) })
                      }
                    />
                    {role.name}
                  </label>
                ))}
              </div>
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

      {/* Edit Roles Dialog */}
      <Dialog open={editRolesOpen} onOpenChange={setEditRolesOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Change Roles</DialogTitle>
            <DialogDescription>Update roles for {selectedUser?.username}.</DialogDescription>
          </DialogHeader>
          <div className="space-y-2 rounded-md border p-3">
            {roles.map((role) => (
              <label key={role.id} className="flex items-center gap-2 text-sm">
                <Checkbox
                  checked={editingRoleIds.includes(role.id)}
                  onCheckedChange={() => setEditingRoleIds((prev) => toggleRoleId(prev, role.id))}
                />
                {role.name}
              </label>
            ))}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditRolesOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleRolesChange}>Update</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reset Password Dialog */}
      <Dialog open={resetPasswordOpen} onOpenChange={setResetPasswordOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reset Password</DialogTitle>
            <DialogDescription>
              Set a new password for {selectedUser?.username}. This will sign them out of all active sessions.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">New Password</label>
              <Input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoComplete="new-password"
              />
            </div>
            <div>
              <label className="text-sm font-medium">Confirm New Password</label>
              <Input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setResetPasswordOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleResetPassword}>Reset Password</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete User</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete {selectedUser?.username}? This action cannot be undone.
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
