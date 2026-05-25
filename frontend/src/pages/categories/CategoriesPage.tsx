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
import type { Category, CreateCategoryRequest } from '@/types/api'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { toast } from 'sonner'

export function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null)
  const [formData, setFormData] = useState<CreateCategoryRequest>({ name: '', parentId: undefined })

  const fetchCategories = useCallback(async () => {
    try {
      const { data } = await api.get<Category[]>('/categories')
      setCategories(data)
    } catch {
      toast.error('Failed to load categories')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchCategories()
  }, [fetchCategories])

  const resetForm = () => setFormData({ name: '', parentId: undefined })

  const handleCreate = async () => {
    try {
      await api.post('/categories', formData)
      toast.success('Category created')
      setCreateOpen(false)
      resetForm()
      fetchCategories()
    } catch {
      toast.error('Failed to create category')
    }
  }

  const handleEdit = async () => {
    if (!selectedCategory) return
    try {
      await api.put(`/categories/${selectedCategory.id}`, formData)
      toast.success('Category updated')
      setEditOpen(false)
      resetForm()
      fetchCategories()
    } catch {
      toast.error('Failed to update category')
    }
  }

  const handleDelete = async () => {
    if (!selectedCategory) return
    try {
      await api.delete(`/categories/${selectedCategory.id}`)
      toast.success('Category deleted')
      setDeleteOpen(false)
      fetchCategories()
    } catch {
      toast.error('Failed to delete category')
    }
  }

  const openEdit = (category: Category) => {
    setSelectedCategory(category)
    setFormData({ name: category.name, parentId: category.parentId ?? undefined })
    setEditOpen(true)
  }

  const getParentName = (parentId: number | null): string => {
    if (!parentId) return '—'
    const parent = categories.find((c) => c.id === parentId)
    return parent?.name ?? '—'
  }

  if (loading) return <div className="text-muted-foreground">Loading categories...</div>

  const CategoryForm = () => (
    <div className="space-y-4">
      <div>
        <label className="text-sm font-medium">Name</label>
        <Input
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
        />
      </div>
      <div>
        <label className="text-sm font-medium">Parent Category (optional)</label>
        <Select
          value={formData.parentId?.toString() ?? ''}
          onValueChange={(v) =>
            setFormData({ ...formData, parentId: v ? Number(v) : undefined })
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="No parent" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">No parent</SelectItem>
            {categories
              .filter((c) => c.id !== selectedCategory?.id)
              .map((cat) => (
                <SelectItem key={cat.id} value={cat.id.toString()}>
                  {cat.name}
                </SelectItem>
              ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Categories</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add Category
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Parent Category</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {categories.map((category) => (
              <TableRow key={category.id}>
                <TableCell className="font-medium">{category.name}</TableCell>
                <TableCell>{getParentName(category.parentId)}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(category)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedCategory(category)
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
            <DialogTitle>Create Category</DialogTitle>
            <DialogDescription>Add a new category.</DialogDescription>
          </DialogHeader>
          <CategoryForm />
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
            <DialogTitle>Edit Category</DialogTitle>
          </DialogHeader>
          <CategoryForm />
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
            <DialogTitle>Delete Category</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete &quot;{selectedCategory?.name}&quot;?
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
