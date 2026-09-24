import type { ColumnDef, RowData, SortingState } from "@tanstack/react-table";
import { useAppTable } from "../table";

interface DataTableProps<TData extends RowData> {
  data: TData[];
  columns: Array<ColumnDef<any, TData, any>>;
  getRowId?: (originalRow: TData, index: number) => string;
  initialSorting?: SortingState;
  searchPlaceholder?: string;
  emptyMessage?: string;
}

export function DataTable<TData extends RowData>({
  data,
  columns,
  getRowId,
  initialSorting,
  searchPlaceholder = "Search…",
  emptyMessage = "No rows.",
}: DataTableProps<TData>) {
  const table = useAppTable({
    columns,
    data,
    getRowId,
    initialState: initialSorting ? { sorting: initialSorting } : undefined,
  });

  return (
    <table.AppTable>
      <div className="table-toolbar">
        <label htmlFor="table-search">Search</label>
        <table.Subscribe selector={(state) => state.globalFilter}>
          {(globalFilter) => (
            <input
              id="table-search"
              value={typeof globalFilter === "string" ? globalFilter : ""}
              placeholder={searchPlaceholder}
              onChange={(event) => table.setGlobalFilter(event.target.value)}
            />
          )}
        </table.Subscribe>
      </div>

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  const canSort = header.column.getCanSort();
                  const sorted = header.column.getIsSorted();
                  return (
                    <th key={header.id}>
                      {canSort ? (
                        <button
                          type="button"
                          className="th-sort"
                          onClick={header.column.getToggleSortingHandler()}
                        >
                          <table.FlexRender header={header} />
                          <span className="sort-mark">
                            {sorted === "asc" ? "↑" : sorted === "desc" ? "↓" : ""}
                          </span>
                        </button>
                      ) : (
                        <table.FlexRender header={header} />
                      )}
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>
          <tbody>
            {table.getRowModel().rows.length === 0 ? (
              <tr>
                <td colSpan={columns.length} className="muted">
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              table.getRowModel().rows.map((row) => (
                <tr key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <td key={cell.id}>
                      <table.FlexRender cell={cell} />
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </table.AppTable>
  );
}
