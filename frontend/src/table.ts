import {
  columnFilteringFeature,
  createFilteredRowModel,
  createSortedRowModel,
  createTableHook,
  filterFns,
  globalFilteringFeature,
  rowSortingFeature,
  sortFns,
  tableFeatures,
} from "@tanstack/react-table";

export const { useAppTable, createAppColumnHelper } = createTableHook({
  features: tableFeatures({
    rowSortingFeature,
    columnFilteringFeature,
    globalFilteringFeature,
    sortedRowModel: createSortedRowModel(),
    filteredRowModel: createFilteredRowModel(),
    sortFns,
    filterFns,
  }),
  enableMultiSort: false,
});
