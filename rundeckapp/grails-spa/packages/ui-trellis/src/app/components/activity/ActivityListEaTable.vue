<template>
  <div class="activity-ea-table" @auxclick="onAuxClick">
    <div v-if="showToolbar" class="activity-ea-table__toolbar">
      <span
        class="activity-ea-table__count text-heading--md"
        data-testid="activity-ea-execution-count"
      >
        {{ Math.max(0, pagination.total) }}
        {{
          $t(
            pagination.total === 1
              ? "activity.table.execution"
              : "activity.table.executions",
          )
        }}
      </span>
      <PtSelect
        v-model="selectedRecentFilter"
        class="activity-ea-table__filter text-body"
        :options="timeFilterOptions"
        option-label="label"
        option-value="value"
        append-to="self"
        :aria-label="$t('activity.table.timeFilter')"
      >
        <template #value="{ value }">
          <span
            class="activity-ea-table__filter-value"
            data-testid="activity-ea-time-filter"
          >
            <i class="far fa-clock" aria-hidden="true"></i>
            <span>{{ timeFilterLabel(value) }}</span>
          </span>
        </template>
      </PtSelect>
      <PtSelect
        v-model="selectedSavedFilterName"
        class="activity-ea-table__filter text-body"
        :options="savedFilterOptions"
        option-label="label"
        option-value="value"
        :placeholder="$t('activity.table.savedFilters')"
        :disabled="savedFilters.length === 0"
        append-to="self"
        :aria-label="$t('activity.table.savedFilters')"
      >
        <template #value="{ value, placeholder }">
          <span
            class="activity-ea-table__filter-value"
            data-testid="activity-ea-saved-filters"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
            <span>{{ value || placeholder }}</span>
          </span>
        </template>
      </PtSelect>
    </div>

    <PtDataTable
      class="text-body"
      :value="loading ? [] : reports"
      data-key="id"
      :first="pagination.offset"
      :rows="pagination.max"
      :total-records="pagination.total"
      :paginator="loading || pagination.total > 0"
      :always-show-paginator="true"
      paginator-template="CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
      :current-page-report-template="currentPageReportTemplate"
      :rows-per-page-options="rowsPerPageOptions"
      :row-class="rowClass"
      :lazy="true"
      :sort-field="sortField"
      :sort-order="sortOrder"
      :table-props="{
        'aria-label': $t('activity.table.ariaLabel'),
        'aria-busy': loading,
      }"
      data-testid="activity-ea-data-table"
      @page="onPage"
      @sort="onSort"
      @row-click="onRowClick"
    >
      <template v-for="column in columns" :key="column">
        <Column
          v-if="column === 'status'"
          field="status"
          class="activity-ea-table__status"
        >
          <template #header>
            <span class="sr-only" data-testid="activity-ea-header-status">
              {{ $t("activity.table.status") }}
            </span>
          </template>
          <template #body="{ data }">
            <b
              class="exec-status icon"
              :title="statusState(data)"
              :data-execstate="statusState(data).toUpperCase()"
              :data-statusstring="statusState(data)"
              :data-testid="`activity-ea-status-${data.id}`"
            ></b>
          </template>
        </Column>
        <Column
          v-if="column === 'id'"
          field="id"
          :sortable="sortable"
          class="activity-ea-table__execution"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <input
                v-if="selectable"
                type="checkbox"
                :checked="allPageSelected"
                :indeterminate="somePageSelected"
                :disabled="loading"
                :aria-label="$t('activity.table.selectAll')"
                data-testid="activity-ea-select-all"
                @click.stop
                @change="togglePageSelection"
              />
              <span data-testid="activity-ea-header-execution">
                {{ $t("activity.table.execution") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <div class="activity-ea-table__execution-content">
              <input
                v-if="selectable"
                type="checkbox"
                :checked="isSelected(data)"
                :aria-label="
                  $t('activity.table.selectExecution', { id: data.id })
                "
                :data-testid="`activity-ea-select-${data.id}`"
                @click.stop
                @change="toggleSelection(data)"
              />
              <a
                :href="data.permalink"
                target="_blank"
                rel="noopener noreferrer"
                :data-testid="`activity-ea-execution-${data.id}`"
                @click.stop
                @click.middle.stop
              >
                #{{ data.id }}
              </a>
            </div>
          </template>
        </Column>
        <Column
          v-if="column === 'dateEnded'"
          field="dateEnded"
          :sortable="sortable"
          class="activity-ea-table__date"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-date-time">
                {{ $t("activity.table.dateTime") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <span
              class="activity-ea-table__truncate"
              :title="completedTooltip(data)"
            >
              <span class="timeabs">{{ completedDate(data) }}</span>
              <span
                v-if="showRelativeDate && data.dateEnded && data.dateEnded.date"
                class="timerel text-muted"
                :data-testid="`activity-ea-date-relative-${data.id}`"
              >
                {{ relativeDate(data.dateEnded.date) }}
              </span>
            </span>
          </template>
        </Column>
        <Column
          v-if="column === 'plugin'"
          field="plugin"
          class="activity-ea-table__plugin"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-plugin">
                {{ $t("activity.table.plugin") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <div class="activity-ea-table__plugin-content">
              <PluginIcon
                v-if="hasPluginIcon(data)"
                :detail="data.plugin"
                icon-class="activity-ea-table__plugin-icon"
              />
              <span
                class="activity-ea-table__truncate"
                :title="executionTitle(data)"
              >
                {{ executionTitle(data) }}
              </span>
            </div>
          </template>
        </Column>
        <Column
          v-if="column === 'nodes'"
          field="nodes"
          class="activity-ea-table__nodes"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-nodes">
                {{ $t("activity.table.nodes") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <span
              v-if="data.node && data.node.failed > 0"
              class="activity-ea-table__nodes-failed"
              :title="nodesTooltip(data)"
            >
              <i class="far fa-times-circle" aria-hidden="true"></i>
              {{ data.node.failed }} {{ $t("failed") }}
            </span>
            <span
              v-else-if="data.node && data.node.succeeded > 0"
              class="activity-ea-table__nodes-succeeded"
              :title="nodesTooltip(data)"
            >
              <i class="far fa-check-circle" aria-hidden="true"></i>
              {{ data.node.succeeded }} {{ $t("ok") }}
            </span>
          </template>
        </Column>
        <Column
          v-if="column === 'duration'"
          field="duration"
          class="activity-ea-table__duration"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-duration">
                {{ $t("activity.table.duration") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <span :data-testid="`activity-ea-duration-${data.id}`">
              {{ executionDuration(data) }}
            </span>
          </template>
        </Column>
        <Column
          v-if="column === 'user'"
          field="user"
          :sortable="sortable"
          class="activity-ea-table__executor"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-executor">
                {{ $t("activity.table.executor") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <span class="activity-ea-table__truncate" :title="data.user">
              {{ data.user }}
            </span>
          </template>
        </Column>

        <Column
          v-if="column === 'title'"
          field="title"
          class="activity-ea-table__title"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-title">
                {{ $t("activity.table.title") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <div class="activity-ea-table__title-content">
              <span
                class="activity-ea-table__truncate"
                :title="titleTooltip(data)"
                :data-testid="`activity-ea-title-${data.id}`"
              >
                {{ reportTitle(data) }}
              </span>
              <span
                v-if="isReferenced(data)"
                class="text-secondary"
                :data-testid="`activity-ea-referenced-${data.id}`"
              >
                <i class="fas fa-arrow-circle-right-alt" aria-hidden="true"></i>
                {{ $t("Referenced") }}
              </span>
              <span
                v-if="data.jobDeleted"
                class="text-strong"
                :data-testid="`activity-ea-job-deleted-${data.id}`"
              >
                {{ $t("job.has.been.deleted.0", [""]) }}
              </span>
              <span
                v-if="hasCustomStatus(data)"
                class="exec-status-text custom-status"
                :data-testid="`activity-ea-custom-status-${data.id}`"
              >
                {{ data.customStatus }}
              </span>
            </div>
          </template>
        </Column>
        <Column
          v-if="column === 'args'"
          field="argstring"
          class="activity-ea-table__args"
        >
          <template #header>
            <div class="activity-ea-table__header-content text-button">
              <span data-testid="activity-ea-header-args">
                {{ $t("activity.table.options") }}
              </span>
            </div>
          </template>
          <template #body="{ data }">
            <span
              class="activity-ea-table__truncate"
              :title="data.argstring"
              :data-testid="`activity-ea-args-${data.id}`"
            >
              {{ data.argstring }}
            </span>
          </template>
        </Column>
      </template>

      <template #empty>
        <div
          v-if="loading"
          class="activity-ea-table__loading"
          role="status"
          aria-live="polite"
          data-testid="activity-ea-loading"
        >
          <i
            class="pi pi-spinner pi-spin activity-ea-table__loading-icon"
            aria-hidden="true"
            data-testid="activity-ea-loading-icon"
          ></i>
          <span>{{ $t("activity.table.loading") }}</span>
        </div>
        <span v-else data-testid="activity-ea-empty">
          {{ $t("results.empty.text") }}
        </span>
      </template>
    </PtDataTable>
  </div>
</template>

<script lang="ts">
import type { PropType } from "vue";
import { defineComponent } from "vue";
import moment from "moment";
import Column from "primevue/column";
import PtDataTable from "../../../library/components/primeVue/PtDataTable/PtDataTable.vue";
import PtSelect from "../../../library/components/primeVue/PtSelect/PtSelect.vue";
import PluginIcon from "../../../library/components/plugins/PluginIcon.vue";
import type { Pageable } from "../../../library/interfaces/UiTypes";
import * as DateTimeFormatters from "../../utilities/DateTimeFormatters";
import { executionState, isCustomStatus } from "./executionState";
import {
  DEFAULT_ACTIVITY_COLUMNS,
  type ActivityColumn,
  type ActivityExecution,
  ActivityPageRequest,
  ActivitySavedFilter,
  ActivitySavedFilterOption,
  ActivitySortField,
  ActivitySortOrder,
} from "./ActivityListEaTableTypes";

/**
 * PrimeVue presentation of completed executions for ActivityList's EA mode.
 */
export default defineComponent({
  name: "ActivityListEaTable",
  components: {
    Column,
    PluginIcon,
    PtDataTable,
    PtSelect,
  },
  props: {
    reports: {
      type: Array as PropType<ActivityExecution[]>,
      required: true,
    },
    pagination: {
      type: Object as PropType<Pageable>,
      required: true,
    },
    loading: {
      type: Boolean,
      required: true,
    },
    /** Built-in columns to render, in display order. */
    columns: {
      type: Array as PropType<ActivityColumn[]>,
      default: () => [...DEFAULT_ACTIVITY_COLUMNS],
    },
    /** Shows the row and select-all checkboxes used for bulk actions. */
    selectable: {
      type: Boolean,
      default: true,
    },
    /** Allows server-backed sorting on the id, date and executor columns. */
    sortable: {
      type: Boolean,
      default: true,
    },
    /** Shows the count, time filter and saved filter toolbar. */
    showToolbar: {
      type: Boolean,
      default: true,
    },
    /** Adds the relative ("2 days ago") time beside the completion date. */
    showRelativeDate: {
      type: Boolean,
      default: false,
    },
    /** "compact" (1m 5s) or "humanize" (a minute) duration text. */
    durationFormat: {
      type: String as PropType<"compact" | "humanize">,
      default: "compact",
    },
    /** Execution row to highlight, e.g. the one the page was opened for. */
    highlightId: {
      type: [Number, String] as PropType<number | string | null>,
      default: null,
    },
    /** Adds the legacy activity row classes (status, job/adhoc) to rows. */
    legacyRowClasses: {
      type: Boolean,
      default: false,
    },
    /** Opens the execution in a new tab when a row is middle-clicked. */
    middleClickOpensTab: {
      type: Boolean,
      default: false,
    },
    /** Job ID filter of the current query, used to flag referenced jobs. */
    jobIdFilter: {
      type: String,
      default: "",
    },
    selectedIds: {
      type: Array as PropType<number[]>,
      required: true,
    },
    recentFilter: {
      type: String,
      default: "",
    },
    savedFilters: {
      type: Array as PropType<ActivitySavedFilter[]>,
      default: () => [],
    },
    savedFilterName: {
      type: String,
      default: "",
    },
    momentJobFormat: {
      type: String,
      default: "M/DD/YY h:mm a",
    },
  },
  emits: {
    page: (request: ActivityPageRequest) =>
      Number.isFinite(request.offset) &&
      Number.isFinite(request.max) &&
      (request.sortOrder === undefined ||
        request.sortOrder === 1 ||
        request.sortOrder === -1),
    "row-click": (execution: ActivityExecution) => execution.id !== undefined,
    "update:selectedIds": (ids: number[]) => Array.isArray(ids),
    "update:recentFilter": (value: string) => typeof value === "string",
    "select-saved-filter": (filter: ActivitySavedFilter) =>
      typeof filter.filterName === "string",
    "clear-saved-filter": () => true,
  },
  data() {
    return {
      sortField: "dateEnded" as ActivitySortField,
      sortOrder: -1 as ActivitySortOrder,
    };
  },
  computed: {
    timeFilterOptions(): Array<{ label: string; value: string }> {
      return [
        { label: this.$t("activity.table.anyTime"), value: "" },
        { label: this.$t("activity.table.lastHour"), value: "1h" },
        { label: this.$t("activity.table.lastDay"), value: "1d" },
        { label: this.$t("activity.table.lastWeek"), value: "1w" },
        { label: this.$t("activity.table.lastMonth"), value: "1m" },
      ];
    },
    /** Offers the unfiltered state alongside the project's saved filters. */
    savedFilterOptions(): ActivitySavedFilterOption[] {
      return [
        { label: this.$t("activity.table.savedFilters"), value: "" },
        ...this.savedFilters.map((filter) => ({
          label: filter.filterName,
          value: filter.filterName,
        })),
      ];
    },
    selectedRecentFilter: {
      get(): string {
        return this.recentFilter;
      },
      set(value: string) {
        this.$emit("update:recentFilter", value);
      },
    },
    selectedSavedFilterName: {
      get(): string {
        return this.savedFilterName;
      },
      set(value: string) {
        if (!value) {
          this.$emit("clear-saved-filter");
          return;
        }
        const filter = this.savedFilters.find(
          (candidate) => candidate.filterName === value,
        );
        if (filter) {
          this.$emit("select-saved-filter", filter);
        }
      },
    },
    pageIds(): number[] {
      return this.reports
        .map((report) => report.id)
        .filter((id): id is number => id !== undefined);
    },
    allPageSelected(): boolean {
      return (
        this.pageIds.length > 0 &&
        this.pageIds.every((id) => this.selectedIds.includes(id))
      );
    },
    somePageSelected(): boolean {
      return (
        !this.allPageSelected &&
        this.pageIds.some((id) => this.selectedIds.includes(id))
      );
    },
    /** Standard page sizes plus the configured page size. */
    rowsPerPageOptions(): number[] {
      return Array.from(new Set([10, 20, 50, this.pagination.max]))
        .filter((size) => size > 0)
        .sort((left, right) => left - right);
    },
    currentPageReportTemplate(): string {
      return this.$t("activity.table.pageReport", {
        first: "{first}",
        last: "{last}",
        totalRecords: "{totalRecords}",
      });
    },
  },
  methods: {
    /** Row classes for highlighting and the optional legacy markers. */
    rowClass(execution: ActivityExecution): Record<string, boolean> {
      const highlight =
        this.highlightId !== null &&
        String(this.highlightId) === String(execution.id);
      if (!this.legacyRowClasses) {
        return { highlight };
      }
      return {
        link: true,
        activity_row: true,
        autoclickable: true,
        [`ali-${execution.id}`]: true,
        succeed: execution.status === "succeeded",
        fail: execution.status === "failed",
        missed: execution.status === "missed",
        job: Boolean(execution.job),
        adhoc: !execution.job,
        highlight,
      };
    },
    /** Opens the middle-clicked row's execution in a new tab. */
    onAuxClick(event: MouseEvent) {
      if (!this.middleClickOpensTab || event.button !== 1) {
        return;
      }
      const target = event.target as HTMLElement | null;
      if (!target || target.closest("a")) {
        return;
      }
      const index = Number(
        target.closest("tr[data-p-index]")?.getAttribute("data-p-index"),
      );
      const execution = Number.isInteger(index)
        ? this.reports[index]
        : undefined;
      if (execution?.permalink) {
        window.open(execution.permalink, "_blank");
      }
    },
    /** Relative completion time; calendar wording within the last week. */
    relativeDate(date: string): string {
      return moment().diff(moment(date), "days") <= 7
        ? DateTimeFormatters.formatCalendar(date)
        : DateTimeFormatters.formatFromNow(date);
    },
    /** Completion tooltip matching the legacy Activity wording. */
    completedTooltip(execution: ActivityExecution): string {
      if (!this.showRelativeDate || !execution.dateEnded?.date) {
        return this.completedDate(execution);
      }
      const ended = moment(execution.dateEnded.date);
      return this.$t(
        execution.status === "missed"
          ? "info.missed.0.1"
          : "info.completed.0.1",
        [ended.toISOString(), ended.fromNow()],
      );
    },
    /** Total node count tooltip for the nodes column. */
    nodesTooltip(execution: ActivityExecution): string {
      return this.$t("0.total", [execution.node?.total ?? 0]);
    },
    /** Full job path for jobs, otherwise the displayed title. */
    titleTooltip(execution: ActivityExecution): string {
      return this.reportTitle(execution);
    },
    /** Normalized state name driving the status icon. */
    statusState(execution: ActivityExecution): string {
      return executionState(execution.status);
    },
    /** Job path for existing jobs, otherwise the execution description. */
    reportTitle(execution: ActivityExecution): string {
      if (execution.job && !execution.jobDeleted) {
        return execution.job.group
          ? `${execution.job.group}/${execution.job.name}`
          : execution.job.name || "";
      }
      return execution.description || "";
    },
    /** Whether the row ran through a job reference of the filtered job. */
    isReferenced(execution: ActivityExecution): boolean {
      return Boolean(
        this.jobIdFilter &&
        execution.job &&
        this.jobIdFilter !== execution.job.id &&
        this.jobIdFilter !== "!null",
      );
    },
    /** Whether the row finished with a user-defined custom status. */
    hasCustomStatus(execution: ActivityExecution): boolean {
      return isCustomStatus(execution.status);
    },
    /** Returns the translated label for a relative-time filter value. */
    timeFilterLabel(value: string): string {
      return (
        this.timeFilterOptions.find((option) => option.value === value)
          ?.label || this.$t("activity.table.anyTime")
      );
    },
    isSelected(execution: ActivityExecution): boolean {
      return (
        execution.id !== undefined && this.selectedIds.includes(execution.id)
      );
    },
    toggleSelection(execution: ActivityExecution) {
      if (execution.id === undefined) {
        return;
      }
      const selectedIds = this.isSelected(execution)
        ? this.selectedIds.filter((id) => id !== execution.id)
        : [...this.selectedIds, execution.id];
      this.$emit("update:selectedIds", selectedIds);
    },
    togglePageSelection() {
      const pageIds = new Set(this.pageIds);
      const retainedIds = this.selectedIds.filter((id) => !pageIds.has(id));
      this.$emit(
        "update:selectedIds",
        this.allPageSelected ? retainedIds : [...retainedIds, ...this.pageIds],
      );
    },
    onPage(event: { first: number; rows: number }) {
      this.$emit("page", {
        offset: event.first,
        max: event.rows,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
      });
    },
    /** Resets paging and requests the newly selected single-column order. */
    onSort(event: { sortField?: string; sortOrder?: number | null }) {
      if (!event.sortField) {
        return;
      }
      this.sortField = event.sortField as ActivitySortField;
      this.sortOrder = event.sortOrder === -1 ? -1 : 1;
      this.$emit("page", {
        offset: 0,
        max: this.pagination.max,
        sortField: this.sortField,
        sortOrder: this.sortOrder,
      });
    },
    onRowClick(event: { data: ActivityExecution }) {
      this.$emit("row-click", event.data);
    },
    completedDate(execution: ActivityExecution): string {
      return execution.dateEnded?.date
        ? DateTimeFormatters.formatDate(
            execution.dateEnded.date,
            this.momentJobFormat,
          )
        : "";
    },
    executionTitle(execution: ActivityExecution): string {
      if (execution.plugin?.title) {
        return execution.plugin.title;
      }
      if (execution.job) {
        return execution.job.group
          ? `${execution.job.group}/${execution.job.name}`
          : execution.job.name || "";
      }
      return execution.description || "";
    },
    hasPluginIcon(execution: ActivityExecution): boolean {
      const metadata = execution.plugin?.providerMetadata;
      return Boolean(
        execution.plugin?.iconUrl ||
        metadata?.glyphicon ||
        metadata?.faicon ||
        metadata?.fabicon,
      );
    },
    executionDuration(execution: ActivityExecution): string {
      if (execution.status === "missed") {
        return this.$t("missed");
      }
      const startTime = execution.dateStarted?.unixtime;
      const endTime = execution.dateEnded?.unixtime;
      if (startTime === undefined || endTime === undefined) {
        return "";
      }
      const duration = moment.duration(endTime - startTime);
      if (duration.asMilliseconds() < 0) {
        return "";
      }
      if (this.durationFormat === "humanize") {
        moment.relativeTimeThreshold("ss", -1);
        return duration.humanize();
      }
      const totalSeconds = Math.floor(duration.asSeconds());
      if (totalSeconds < 60) {
        return `${totalSeconds}s`;
      }
      const totalMinutes = Math.floor(totalSeconds / 60);
      const seconds = totalSeconds % 60;
      if (totalMinutes < 60) {
        return `${totalMinutes}m${seconds > 0 ? ` ${seconds}s` : ""}`;
      }
      const hours = Math.floor(totalMinutes / 60);
      const minutes = totalMinutes % 60;
      return `${hours}h${minutes > 0 ? ` ${minutes}m` : ""}`;
    },
  },
});
</script>

<style scoped lang="scss">
.activity-ea-table {
  --sizes-activity-table-top-offset: 22px;
  --sizes-activity-table-toolbar-height: 40px;
  --sizes-activity-table-toolbar-gap: 16px;
  --sizes-activity-table-content-gap: 8px;
  --sizes-activity-table-header-gap: 7px;
  --sizes-activity-table-table-gap: 12px;
  --sizes-activity-table-filter-width: 216px;
  --sizes-activity-table-control-height: 38px;
  --sizes-activity-table-control-padding: 10px;
  --radii-activity-table-control: 6px;
  --sizes-activity-table-checkbox: 16px;
  --sizes-activity-table-compact-column: 150px;
  --sizes-activity-table-status-column: 48px;
  --sizes-activity-table-date-column: 277px;
  --sizes-activity-table-plugin-column: 300px;
  --sizes-activity-table-loading-height: 220px;
  --sizes-activity-table-loading-gap: 4px;
  --sizes-activity-table-loading-icon: 48px;

  margin-top: var(--sizes-activity-table-top-offset);
}

.activity-ea-table__toolbar {
  display: flex;
  align-items: center;
  gap: var(--sizes-activity-table-toolbar-gap);
  min-height: var(--sizes-activity-table-toolbar-height);
  margin-bottom: var(--sizes-activity-table-table-gap);
}

.activity-ea-table__count {
  flex: 0 0 auto;
  color: var(--colors-gray-800-original);
}

.activity-ea-table__filter {
  flex: 0 0 var(--sizes-activity-table-filter-width);
  width: var(--sizes-activity-table-filter-width);

  :deep(.p-select) {
    height: var(--sizes-activity-table-control-height);
  }
}

.activity-ea-table__filter-value {
  display: flex;
  align-items: center;
  gap: var(--sizes-activity-table-control-padding);
  width: var(--sizes-full);

  span {
    flex: 1;
    overflow: hidden;
    text-align: left;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  i {
    flex: 0 0 auto;
    color: var(--colors-gray-600);
  }
}

:deep(.activity-ea-table__execution) {
  width: var(--sizes-activity-table-compact-column);
}

:deep(.activity-ea-table__date) {
  width: var(--sizes-activity-table-date-column);
}

:deep(.activity-ea-table__plugin) {
  width: var(--sizes-activity-table-plugin-column);
}

:deep(.activity-ea-table__nodes),
:deep(.activity-ea-table__duration) {
  width: var(--sizes-activity-table-compact-column);
}

:deep(.activity-ea-table__status) {
  width: var(--sizes-activity-table-status-column);
}

:deep(.activity-ea-table__title) {
  width: var(--sizes-activity-table-plugin-column);
}

:deep(.activity-ea-table__args) {
  width: var(--sizes-activity-table-date-column);
}

:deep(.activity-ea-table__executor) {
  width: var(--sizes-activity-table-date-column);
}

.activity-ea-table__title-content {
  display: flex;
  align-items: center;
  gap: var(--sizes-activity-table-content-gap);
  min-width: var(--sizes-none);
  overflow: hidden;
  white-space: nowrap;
}

.activity-ea-table__header-content,
.activity-ea-table__execution-content,
.activity-ea-table__plugin-content {
  display: flex;
  align-items: center;
  gap: var(--sizes-activity-table-content-gap);
}

.activity-ea-table__header-content {
  gap: var(--sizes-activity-table-header-gap);
  white-space: nowrap;

  i {
    font-weight: var(--fontWeights-regular);
  }
}

.activity-ea-table__header-content input,
.activity-ea-table__execution-content input {
  flex: 0 0 var(--sizes-activity-table-checkbox);
  width: var(--sizes-activity-table-checkbox);
  height: var(--sizes-activity-table-checkbox);
  margin: var(--sizes-none);
  accent-color: var(--colors-blue-500);
}

.activity-ea-table__execution-content a {
  color: var(--colors-blue-500);
}

.activity-ea-table__plugin-content {
  min-width: var(--sizes-none);
  overflow: hidden;
  white-space: nowrap;
}

.activity-ea-table__truncate {
  min-width: var(--sizes-none);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.activity-ea-table__plugin-icon) {
  display: inline-flex;
  flex: 0 0 var(--sizes-activity-table-checkbox);
  align-items: center;
  justify-content: center;
  width: var(--sizes-activity-table-checkbox);
  height: var(--sizes-activity-table-checkbox);
  color: var(--colors-blue-500);
  text-align: center;

  .plugin-icon {
    width: var(--sizes-full);
    height: var(--sizes-full);
    object-fit: contain;
  }
}

.activity-ea-table__nodes-failed {
  color: var(--colors-red-500);
}

.activity-ea-table__nodes-succeeded {
  color: var(--colors-green-500);
}

.activity-ea-table__nodes-failed,
.activity-ea-table__nodes-succeeded {
  display: inline-flex;
  align-items: center;
  gap: var(--sizes-activity-table-header-gap);
}

.activity-ea-table__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--sizes-activity-table-loading-gap);
  min-height: var(--sizes-activity-table-loading-height);
  color: var(--colors-gray-800);
  font-size: 14px;
  font-weight: var(--fontWeights-regular);
  line-height: 21px;
}

.activity-ea-table__loading-icon {
  width: var(--sizes-activity-table-loading-icon);
  height: var(--sizes-activity-table-loading-icon);
  color: var(--colors-gray-800);
  font-size: var(--sizes-activity-table-loading-icon);
  line-height: var(--sizes-activity-table-loading-icon);
}
</style>
