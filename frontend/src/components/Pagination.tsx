import { Button } from '@/components/ui/button'
import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  const pages: (number | '...')[] = []
  const maxVisible = 5

  if (totalPages <= maxVisible + 2) {
    for (let i = 0; i < totalPages; i++) pages.push(i)
  } else {
    pages.push(0)
    if (currentPage > 2) pages.push('...')
    const start = Math.max(1, currentPage - 1)
    const end = Math.min(totalPages - 2, currentPage + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (currentPage < totalPages - 3) pages.push('...')
    pages.push(totalPages - 1)
  }

  return (
    <div className="flex items-center justify-between gap-2 py-4">
      <p className="text-sm text-muted-foreground">
        Page {currentPage + 1} of {totalPages}
      </p>
      <div className="flex items-center gap-1">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        {pages.map((page, idx) =>
          page === '...' ? (
            <span key={`ellipsis-${idx}`} className="px-2 text-muted-foreground">
              ...
            </span>
          ) : (
            <Button
              key={page}
              variant={currentPage === page ? 'default' : 'outline'}
              size="sm"
              onClick={() => onPageChange(page as number)}
              className="min-w-[2rem]"
            >
              {(page as number) + 1}
            </Button>
          )
        )}
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}
