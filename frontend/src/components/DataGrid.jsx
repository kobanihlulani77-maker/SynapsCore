import { useState } from 'react'

export default function DataGrid({ columns = [], rows = [], className = '', emptyMessage = 'No rows available.' }) {
  const [sortState, setSortState] = useState({ key: '', direction: 'asc' })

  const sortedRows = (() => {
    if (!sortState.key) return rows
    return [...rows].sort((left, right) => {
      const leftValue = columns.find((column) => column.key === sortState.key)?.sortValue?.(left) ?? left[sortState.key]
      const rightValue = columns.find((column) => column.key === sortState.key)?.sortValue?.(right) ?? right[sortState.key]
      if (leftValue === rightValue) return 0
      if (leftValue == null) return 1
      if (rightValue == null) return -1
      const direction = sortState.direction === 'asc' ? 1 : -1
      return leftValue > rightValue ? direction : -direction
    })
  })()

  const toggleSort = (column) => {
    if (!column.sortable) return
    setSortState((current) => {
      if (current.key !== column.key) {
        return { key: column.key, direction: 'asc' }
      }
      return { key: column.key, direction: current.direction === 'asc' ? 'desc' : 'asc' }
    })
  }

  return (
    <div className={`data-grid ${className}`.trim()}>
      <table className="data-grid-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                aria-sort={sortState.key === column.key ? (sortState.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
              >
                <button
                  className={`data-grid-sort ${column.sortable ? 'is-sortable' : ''}`}
                  onClick={() => toggleSort(column)}
                  type="button"
                  aria-label={column.sortable ? `Sort by ${column.label}` : column.label}
                >
                  <span>{column.label}</span>
                  {sortState.key === column.key ? <span aria-hidden="true">{sortState.direction === 'asc' ? 'ASC' : 'DESC'}</span> : null}
                </button>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sortedRows.map((row, rowIndex) => (
            <tr key={row.id || row.key || rowIndex}>
              {columns.map((column) => <td key={column.key}>{column.render ? column.render(row) : row[column.key]}</td>)}
            </tr>
          ))}
          {!sortedRows.length ? (
            <tr className="table-empty-row">
              <td colSpan={Math.max(columns.length, 1)}>{emptyMessage}</td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  )
}
