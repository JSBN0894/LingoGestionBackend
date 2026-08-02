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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Pagination } from '@/components/Pagination'
import type { AuditEntry, AuditFilters } from '@/types/api'
import { Download, Filter } from 'lucide-react'
import { toast } from 'sonner'

const PAGE_SIZE = 20

interface AuditPageResponse {
  content: AuditEntry[]
  totalPages: number
  totalElements: number
  number: number
  size: number
}

export function AuditLogPage() {
  const [entries, setEntries] = useState<AuditEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [filters, setFilters] = useState<AuditFilters>({})
  const [showFilters, setShowFilters] = useState(false)
  const [searchId, setSearchId] = useState('')

  const fetchAudit = useCallback(async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: PAGE_SIZE.toString(),
        sort: 'id,desc',
      })
      if (filters.username) params.set('username', filters.username)
      if (filters.entityType) params.set('entityType', filters.entityType)
      if (filters.action) params.set('action', filters.action)
      if (filters.fromDate) params.set('fromDate', filters.fromDate)
      if (filters.toDate) params.set('toDate', filters.toDate)
      if (searchId) params.set('entityId', searchId)

      const { data } = await api.get<AuditPageResponse>(`/admin/audit?${params.toString()}`)
      setEntries(data.content ?? [])
      setTotalPages(data.totalPages ?? 1)
    } catch {
      toast.error('Failed to load audit log')
    } finally {
      setLoading(false)
    }
  }, [page, filters, searchId])

  useEffect(() => {
    fetchAudit()
  }, [fetchAudit])

  const handleExport = () => {
    const headers = ['Timestamp', 'Username', 'Action', 'Entity Type', 'Entity ID', 'IP Address']
    const rows = entries.map((e) => [
      new Date(e.timestamp).toLocaleString(),
      e.username,
      e.action,
      e.entityType,
      e.entityId,
      e.ipAddress,
    ])
    const csv = [headers, ...rows].map((r) => r.join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `audit-log-${new Date().toISOString().split('T')[0]}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  // Unique values for filter dropdowns
  const uniqueUsernames = Array.from(new Set(entries.map((e) => e.username)))
  const uniqueEntityTypes = Array.from(new Set(entries.map((e) => e.entityType)))
  const uniqueActions = Array.from(new Set(entries.map((e) => e.action)))

  if (loading && entries.length === 0) {
    return <div className="text-muted-foreground">Loading audit log...</div>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Audit Log</h1>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setShowFilters(!showFilters)}>
            <Filter className="mr-2 h-4 w-4" />
            Filters
          </Button>
          <Button variant="outline" onClick={handleExport}>
            <Download className="mr-2 h-4 w-4" />
            Export CSV
          </Button>
        </div>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="rounded-md border p-4">
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
            <div>
              <label className="text-sm font-medium">Username</label>
              <Select
                value={filters.username ?? ''}
                onValueChange={(v) => {
                  setFilters({ ...filters, username: v || undefined })
                  setPage(0)
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All users" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All users</SelectItem>
                  {uniqueUsernames.map((u) => (
                    <SelectItem key={u} value={u}>
                      {u}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-sm font-medium">Entity Type</label>
              <Select
                value={filters.entityType ?? ''}
                onValueChange={(v) => {
                  setFilters({ ...filters, entityType: v || undefined })
                  setPage(0)
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All types" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All types</SelectItem>
                  {uniqueEntityTypes.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-sm font-medium">Action</label>
              <Select
                value={filters.action ?? ''}
                onValueChange={(v) => {
                  setFilters({ ...filters, action: v || undefined })
                  setPage(0)
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All actions" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All actions</SelectItem>
                  {uniqueActions.map((a) => (
                    <SelectItem key={a} value={a}>
                      {a}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-sm font-medium">From Date</label>
              <Input
                type="date"
                value={filters.fromDate ?? ''}
                onChange={(e) => {
                  setFilters({ ...filters, fromDate: e.target.value || undefined })
                  setPage(0)
                }}
              />
            </div>
            <div>
              <label className="text-sm font-medium">To Date</label>
              <Input
                type="date"
                value={filters.toDate ?? ''}
                onChange={(e) => {
                  setFilters({ ...filters, toDate: e.target.value || undefined })
                  setPage(0)
                }}
              />
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <Input
              placeholder="Search by Entity ID..."
              value={searchId}
              onChange={(e) => setSearchId(e.target.value)}
              className="max-w-xs"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setFilters({})
                setSearchId('')
                setPage(0)
              }}
            >
              Clear
            </Button>
          </div>
        </div>
      )}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Timestamp</TableHead>
              <TableHead>Username</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Entity Type</TableHead>
              <TableHead>Entity ID</TableHead>
              <TableHead>IP Address</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {entries.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  No audit entries found.
                </TableCell>
              </TableRow>
            ) : (
              entries.map((entry) => (
                <TableRow key={entry.id}>
                  <TableCell className="text-sm">
                    {new Date(entry.timestamp).toLocaleString()}
                  </TableCell>
                  <TableCell className="font-medium">{entry.username}</TableCell>
                  <TableCell>{entry.action}</TableCell>
                  <TableCell>{entry.entityType}</TableCell>
                  <TableCell className="font-mono text-sm">{entry.entityId}</TableCell>
                  <TableCell className="text-sm">{entry.ipAddress}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  )
}
