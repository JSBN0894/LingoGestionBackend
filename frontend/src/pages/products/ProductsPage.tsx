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
import { Pagination } from '@/components/Pagination'
import type { Product, Category, CreateProductRequest, UpdateProductRequest } from '@/types/api'
import { Plus, Pencil, Trash2, Upload } from 'lucide-react'
import { toast } from 'sonner'

const PAGE_SIZE = 20

function ProductForm({
  formData,
  categories,
  imagePreview,
  onFormChange,
  onImageChange,
}: {
  formData: CreateProductRequest
  categories: Category[]
  imagePreview: string
  onFormChange: (data: CreateProductRequest) => void
  onImageChange: (e: React.ChangeEvent<HTMLInputElement>) => void
}) {
  return (
    <div className="space-y-4">
      <div>
        <label className="text-sm font-medium">Name</label>
        <Input
          value={formData.name}
          onChange={(e) => onFormChange({ ...formData, name: e.target.value })}
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="text-sm font-medium">Price</label>
          <Input
            type="text"
            inputMode="numeric"
            value={formData.pricePerUnit === 0 ? '' : String(formData.pricePerUnit)}
            placeholder="0"
            onChange={(e) => {
              const raw = e.target.value
              if (raw === '' || raw === '0') {
                onFormChange({ ...formData, pricePerUnit: 0 })
              } else if (/^\d+$/.test(raw)) {
                onFormChange({ ...formData, pricePerUnit: Number(raw) })
              }
            }}
          />
        </div>
        <div>
          <label className="text-sm font-medium">Stock</label>
          <Input
            type="text"
            inputMode="numeric"
            value={formData.stock === 0 ? '' : String(formData.stock)}
            placeholder="0"
            onChange={(e) => {
              const raw = e.target.value
              if (raw === '' || raw === '0') {
                onFormChange({ ...formData, stock: 0 })
              } else if (/^\d+$/.test(raw)) {
                onFormChange({ ...formData, stock: Number(raw) })
              }
            }}
          />
        </div>
      </div>
      <div>
        <label className="text-sm font-medium">Description</label>
        <Input
          value={formData.description}
          onChange={(e) => onFormChange({ ...formData, description: e.target.value })}
        />
      </div>
      <div>
        <label className="text-sm font-medium">Category</label>
        <Select
          value={formData.categoryId?.toString() ?? 'none'}
          onValueChange={(v) =>
            onFormChange({ ...formData, categoryId: v !== 'none' ? Number(v) : undefined })
          }
        >
          <SelectTrigger>
            <SelectValue placeholder="Select category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="none">No category</SelectItem>
            {categories.map((cat) => (
              <SelectItem key={cat.id} value={cat.id.toString()}>
                {cat.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div>
        <label className="text-sm font-medium">Image</label>
        <div className="flex items-center gap-4">
          <Input type="file" accept="image/*" onChange={onImageChange} />
          {imagePreview && (
            <img
              src={imagePreview}
              alt="Preview"
              className="h-16 w-16 rounded object-cover"
            />
          )}
        </div>
      </div>
    </div>
  )
}

export function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [createOpen, setCreateOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [formData, setFormData] = useState<CreateProductRequest>({
    name: '',
    pricePerUnit: 0,
    stock: 0,
    description: '',
    categoryId: undefined,
  })
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState('')

  const fetchProducts = useCallback(async () => {
    try {
      const { data } = await api.get<Product[]>(`/products?page=${page}&size=${PAGE_SIZE}`)
      // Backend returns a plain array, not paginated
      setProducts(Array.isArray(data) ? data : [])
      setTotalPages(1)
    } catch {
      toast.error('Failed to load products')
    } finally {
      setLoading(false)
    }
  }, [page])

  const fetchCategories = useCallback(async () => {
    try {
      const { data } = await api.get<Category[]>('/categories')
      setCategories(data)
    } catch {
      // Categories might not be accessible
    }
  }, [])

  useEffect(() => {
    fetchProducts()
    fetchCategories()
  }, [fetchProducts, fetchCategories])

  const resetForm = () => {
    setFormData({ name: '', pricePerUnit: 0, stock: 0, description: '', categoryId: undefined })
    setImageFile(null)
    setImagePreview('')
  }

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setImageFile(file)
      setImagePreview(URL.createObjectURL(file))
    }
  }

  const uploadImage = async (): Promise<string | undefined> => {
    if (!imageFile) return undefined
    const form = new FormData()
    form.append('file', imageFile)
    try {
      const { data } = await api.post<{ url: string }>('/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return data.url
    } catch {
      toast.error('Failed to upload image')
      return undefined
    }
  }

  const handleCreate = async () => {
    const imageUrl = await uploadImage()
    try {
      const payload: CreateProductRequest = {
        ...formData,
        imageUrl: imageUrl ?? formData.imageUrl,
      }
      await api.post('/products', payload)
      toast.success('Product created')
      setCreateOpen(false)
      resetForm()
      fetchProducts()
    } catch {
      toast.error('Failed to create product')
    }
  }

  const handleEdit = async () => {
    if (!selectedProduct) return
    const imageUrl = await uploadImage()
    try {
      const payload: UpdateProductRequest = {
        name: formData.name,
        pricePerUnit: formData.pricePerUnit,
        stock: formData.stock,
        description: formData.description,
        categoryId: formData.categoryId,
        imageUrl: imageUrl ?? selectedProduct.imageUrl,
      }
      await api.put(`/products/${selectedProduct.id}`, payload)
      toast.success('Product updated')
      setEditOpen(false)
      resetForm()
      fetchProducts()
    } catch {
      toast.error('Failed to update product')
    }
  }

  const handleDelete = async () => {
    if (!selectedProduct) return
    try {
      await api.delete(`/products/${selectedProduct.id}`)
      toast.success('Product deleted')
      setDeleteOpen(false)
      fetchProducts()
    } catch {
      toast.error('Failed to delete product')
    }
  }

  const openEdit = (product: Product) => {
    setSelectedProduct(product)
    setFormData({
      name: product.name,
      pricePerUnit: product.pricePerUnit,
      stock: product.stock,
      description: product.description,
      categoryId: product.category?.id,
    })
    setImagePreview(product.imageUrl)
    setEditOpen(true)
  }

  const formatPrice = (price: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(price)

  if (loading) return <div className="text-muted-foreground">Loading products...</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Products</h1>
        <Button onClick={() => { resetForm(); setCreateOpen(true) }}>
          <Plus className="mr-2 h-4 w-4" />
          Add Product
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Image</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Price</TableHead>
              <TableHead>Stock</TableHead>
              <TableHead>Category</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {products.map((product) => (
              <TableRow key={product.id}>
                <TableCell>
                  {product.imageUrl ? (
                    <img
                      src={product.imageUrl}
                      alt={product.name}
                      className="h-10 w-10 rounded object-cover"
                    />
                  ) : (
                    <div className="flex h-10 w-10 items-center justify-center rounded bg-muted">
                      <Upload className="h-4 w-4 text-muted-foreground" />
                    </div>
                  )}
                </TableCell>
                <TableCell className="font-medium">{product.name}</TableCell>
                <TableCell>{formatPrice(product.pricePerUnit)}</TableCell>
                <TableCell>{product.stock}</TableCell>
                <TableCell>{product.category?.name ?? '—'}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(product)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedProduct(product)
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

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />

      {/* Create Dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Product</DialogTitle>
            <DialogDescription>Add a new product to the catalog.</DialogDescription>
          </DialogHeader>
          <ProductForm
            formData={formData}
            categories={categories}
            imagePreview={imagePreview}
            onFormChange={setFormData}
            onImageChange={handleImageChange}
          />
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
            <DialogTitle>Edit Product</DialogTitle>
          </DialogHeader>
          <ProductForm
            formData={formData}
            categories={categories}
            imagePreview={imagePreview}
            onFormChange={setFormData}
            onImageChange={handleImageChange}
          />
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
            <DialogTitle>Delete Product</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete &quot;{selectedProduct?.name}&quot;?
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
