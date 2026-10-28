<template>
  <DataTable
    class="pt-data-table"
    data-testid="pt-data-table"
    :value="value"
    :data-key="dataKey"
    :first="first"
    :rows="rows"
    :total-records="totalRecords"
    :paginator="paginator"
    :always-show-paginator="alwaysShowPaginator"
    :paginator-template="paginatorTemplate"
    :current-page-report-template="currentPageReportTemplate"
    :rows-per-page-options="rowsPerPageOptions"
    :lazy="lazy"
    :loading="loading"
    :selection="selection"
    :row-hover="rowHover"
    :row-class="rowClass"
    :show-gridlines="showGridlines"
    :sort-field="sortField"
    :sort-order="sortOrder"
    :table-props="tableProps"
    @update:first="$emit('update:first', $event)"
    @update:rows="$emit('update:rows', $event)"
    @update:selection="$emit('update:selection', $event)"
    @update:sort-field="$emit('update:sortField', $event)"
    @update:sort-order="$emit('update:sortOrder', $event)"
    @page="$emit('page', $event)"
    @sort="$emit('sort', $event)"
    @row-click="$emit('row-click', $event)"
    @select-all-change="$emit('select-all-change', $event)"
  >
    <slot />
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
    <template v-if="$slots.empty" #empty>
      <slot name="empty" />
    </template>
    <template v-if="$slots.groupheader" #groupheader="slotProps">
      <slot name="groupheader" v-bind="slotProps" />
    </template>
    <template v-if="$slots.groupfooter" #groupfooter="slotProps">
      <slot name="groupfooter" v-bind="slotProps" />
    </template>
    <template v-if="$slots.loading" #loading>
      <slot name="loading" />
    </template>
    <template v-if="$slots.expansion" #expansion="slotProps">
      <slot name="expansion" v-bind="slotProps" />
    </template>
    <template v-if="$slots.loadingicon" #loadingicon>
      <slot name="loadingicon" />
    </template>
    <template v-if="$slots.reorderindicatorupicon" #reorderindicatorupicon>
      <slot name="reorderindicatorupicon" />
    </template>
    <template
      v-if="$slots.rowreorderindicatorupicon"
      #rowreorderindicatorupicon
    >
      <slot name="rowreorderindicatorupicon" />
    </template>
    <template v-if="$slots.reorderindicatordownicon" #reorderindicatordownicon>
      <slot name="reorderindicatordownicon" />
    </template>
    <template
      v-if="$slots.rowreorderindicatordownicon"
      #rowreorderindicatordownicon
    >
      <slot name="rowreorderindicatordownicon" />
    </template>
    <template
      v-if="$slots.rowgrouptogglericon"
      #rowgrouptogglericon="slotProps"
    >
      <slot name="rowgrouptogglericon" v-bind="slotProps" />
    </template>
    <template v-if="$slots.paginatorcontainer" #paginatorcontainer="slotProps">
      <slot name="paginatorcontainer" v-bind="slotProps" />
    </template>
    <template v-if="$slots.paginatorstart" #paginatorstart>
      <slot name="paginatorstart" />
    </template>
    <template v-if="$slots.paginatorend" #paginatorend>
      <slot name="paginatorend" />
    </template>
    <template
      v-if="$slots.paginatorfirstpagelinkicon"
      #paginatorfirstpagelinkicon="slotProps"
    >
      <slot name="paginatorfirstpagelinkicon" v-bind="slotProps" />
    </template>
    <template
      v-if="$slots.paginatorprevpagelinkicon"
      #paginatorprevpagelinkicon="slotProps"
    >
      <slot name="paginatorprevpagelinkicon" v-bind="slotProps" />
    </template>
    <template
      v-if="$slots.paginatornextpagelinkicon"
      #paginatornextpagelinkicon="slotProps"
    >
      <slot name="paginatornextpagelinkicon" v-bind="slotProps" />
    </template>
    <template
      v-if="$slots.paginatorlastpagelinkicon"
      #paginatorlastpagelinkicon="slotProps"
    >
      <slot name="paginatorlastpagelinkicon" v-bind="slotProps" />
    </template>
    <template
      v-if="$slots.paginatorrowsperpagedropdownicon"
      #paginatorrowsperpagedropdownicon="slotProps"
    >
      <slot name="paginatorrowsperpagedropdownicon" v-bind="slotProps" />
    </template>
    <template
      v-if="$slots.paginatorjumptopagedropdownicon"
      #paginatorjumptopagedropdownicon="slotProps"
    >
      <slot name="paginatorjumptopagedropdownicon" v-bind="slotProps" />
    </template>
  </DataTable>
</template>

<script lang="ts">
import type { PropType } from "vue";
import { defineComponent } from "vue";
import DataTable from "primevue/datatable";
import type {
  DataTablePageEvent,
  DataTableRowClickEvent,
  DataTableSortEvent,
} from "primevue/datatable";

type TableRecord = Record<string, unknown>;
type RowClass = (data: TableRecord) => string | object | undefined;

/**
 * Rundeck's styled facade over PrimeVue DataTable.
 */
export default defineComponent({
  name: "PtDataTable",
  components: { DataTable },
  props: {
    value: {
      type: Array as PropType<TableRecord[]>,
      default: () => [],
    },
    dataKey: {
      type: String,
      default: "id",
    },
    first: {
      type: Number,
      default: 0,
    },
    rows: {
      type: Number,
      default: 0,
    },
    totalRecords: {
      type: Number,
      default: 0,
    },
    paginator: {
      type: Boolean,
      default: false,
    },
    alwaysShowPaginator: {
      type: Boolean,
      default: false,
    },
    paginatorTemplate: {
      type: String,
      default:
        "CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown",
    },
    currentPageReportTemplate: {
      type: String,
      default: "",
    },
    rowsPerPageOptions: {
      type: Array as PropType<number[]>,
      default: () => [10, 20, 50],
    },
    lazy: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    selection: {
      type: Array as PropType<TableRecord[]>,
      default: () => [],
    },
    rowHover: {
      type: Boolean,
      default: true,
    },
    rowClass: {
      type: Function as PropType<RowClass>,
      default: undefined,
    },
    showGridlines: {
      type: Boolean,
      default: false,
    },
    sortField: {
      type: String,
      default: undefined,
    },
    sortOrder: {
      type: Number,
      default: undefined,
    },
    tableProps: {
      type: Object as PropType<Record<string, unknown>>,
      default: () => ({}),
    },
  },
  emits: {
    "update:first": (value: number) => Number.isFinite(value),
    "update:rows": (value: number) => Number.isFinite(value),
    "update:selection": (value: TableRecord[]) => Array.isArray(value),
    "update:sortField": (value: string) => typeof value === "string",
    "update:sortOrder": (value: number | undefined) =>
      value === undefined || Number.isFinite(value),
    page: (event: DataTablePageEvent) => Number.isFinite(event.first),
    sort: (event: DataTableSortEvent) => Number.isFinite(event.first),
    "row-click": (event: DataTableRowClickEvent) => Boolean(event.data),
    "select-all-change": (event: { originalEvent: Event; checked: boolean }) =>
      typeof event.checked === "boolean",
  },
});
</script>

<style scoped lang="scss">
.pt-data-table {
  :deep(.p-datatable-table-container) {
    border: var(--sizes-px) solid var(--colors-gray-300-original);
    border-radius: var(--radii-lg);
    overflow: hidden;
  }

  :deep(.p-datatable-table) {
    table-layout: fixed;
  }

  :deep(.p-datatable-thead > tr > th) {
    height: calc(var(--sizes-10) + var(--sizes-1) + var(--sizes-px));
    padding: var(--sizes-3\.5);
    border-width: var(--sizes-none) var(--sizes-none) var(--sizes-px);
    border-color: var(--colors-gray-300-original);
    background: var(--colors-gray-50);
    color: var(--colors-gray-800);
    font-size: 14px;
    font-weight: var(--fontWeights-bold);
    line-height: 17px;
  }

  :deep(.p-datatable-tbody > tr > td) {
    height: calc(var(--sizes-12) + var(--sizes-1));
    padding: var(--sizes-3\.5);
    border-width: var(--sizes-none) var(--sizes-none) var(--sizes-px);
    border-color: var(--colors-gray-300-original);
    background: var(--colors-white);
    color: var(--colors-gray-800);
    font-size: 14px;
    font-weight: var(--fontWeights-regular);
    line-height: 21px;
  }

  :deep(.p-datatable-tbody > tr:last-child > td) {
    border-bottom: var(--sizes-none);
  }

  :deep(.p-datatable-tbody > tr:hover > td) {
    background: var(--colors-white);
  }

  :deep(.p-paginator) {
    justify-content: flex-end;
    min-height: var(--sizes-14);
    gap: var(--sizes-1);
    padding: var(--sizes-2) var(--sizes-none);
    border: var(--sizes-none);
    border-radius: var(--radii-md);
    color: var(--colors-gray-800);
    font-size: 14px;
    line-height: 21px;
  }

  :deep(.p-paginator-current) {
    margin: var(--sizes-none);
    padding: var(--sizes-2);
  }

  :deep(.p-paginator-first),
  :deep(.p-paginator-prev),
  :deep(.p-paginator-page),
  :deep(.p-paginator-next),
  :deep(.p-paginator-last) {
    width: calc(var(--sizes-10) + var(--sizes-0\.5));
    min-width: calc(var(--sizes-10) + var(--sizes-0\.5));
    height: calc(var(--sizes-10) + var(--sizes-0\.5));
    margin: var(--sizes-none);
    border-radius: var(--radii-full);
    color: var(--colors-gray-800);
  }

  :deep(.p-paginator-page.p-paginator-page-selected) {
    background: var(--colors-white);
    color: var(--colors-gray-800);
  }

  :deep(.p-paginator-rpp-dropdown) {
    height: calc(var(--sizes-9) + (var(--sizes-px) * 2));
    margin-left: var(--sizes-2);
    border-color: var(--colors-gray-300-original);
    border-radius: var(--radii-md);
  }
}
</style>
