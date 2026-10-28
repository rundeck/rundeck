<template>
  <div class="activity-ea-table">
    <div class="activity-ea-table__toolbar">
      <span
        class="activity-ea-table__count"
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
      <button
        type="button"
        class="activity-ea-table__filter"
        data-testid="activity-ea-time-filter"
      >
        <i class="far fa-clock" aria-hidden="true"></i>
        <span>{{ $t("activity.table.anyTime") }}</span>
        <i class="fas fa-chevron-down" aria-hidden="true"></i>
      </button>
      <button
        type="button"
        class="activity-ea-table__filter"
        data-testid="activity-ea-saved-filters"
      >
        <i class="fas fa-filter" aria-hidden="true"></i>
        <span>{{ $t("activity.table.savedFilters") }}</span>
        <i class="fas fa-chevron-down" aria-hidden="true"></i>
      </button>
    </div>

    <PtDataTable
      :value="reports"
      data-key="id"
      :first="pagination.offset"
      :rows="pagination.max"
      :total-records="pagination.total"
      :paginator="pagination.total > 0"
      :current-page-report-template="currentPageReportTemplate"
      :lazy="true"
      :loading="loading"
      :table-props="{ 'aria-label': $t('activity.table.ariaLabel') }"
      @page="onPage"
      @row-click="onRowClick"
    >
      <Column field="id" class="activity-ea-table__execution">
        <template #header>
          <div class="activity-ea-table__header-content">
            <input
              type="checkbox"
              :checked="allPageSelected"
              :indeterminate="somePageSelected"
              :aria-label="$t('activity.table.selectAll')"
              data-testid="activity-ea-select-all"
              @click.stop
              @change="togglePageSelection"
            />
            <span data-testid="activity-ea-header-execution">
              {{ $t("activity.table.execution") }}
            </span>
            <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
          </div>
        </template>
        <template #body="{ data }">
          <div class="activity-ea-table__execution-content">
            <input
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
              :data-testid="`activity-ea-execution-${data.id}`"
              @click.stop
              @click.middle.stop
            >
              #{{ data.id }}
            </a>
          </div>
        </template>
      </Column>
      <Column field="dateEnded" class="activity-ea-table__date">
        <template #header>
          <div class="activity-ea-table__header-content">
            <span data-testid="activity-ea-header-date-time">
              {{ $t("activity.table.dateTime") }}
            </span>
            <i class="fas fa-sort" aria-hidden="true"></i>
          </div>
        </template>
        <template #body="{ data }">
          {{ completedDate(data) }}
        </template>
      </Column>
      <Column field="description" class="activity-ea-table__plugin">
        <template #header>
          <div class="activity-ea-table__header-content">
            <span data-testid="activity-ea-header-plugin">
              {{ $t("activity.table.plugin") }}
            </span>
            <i class="fas fa-sort" aria-hidden="true"></i>
          </div>
        </template>
        <template #body="{ data }">
          <div class="activity-ea-table__plugin-content">
            <i
              :class="pluginIconClass(data)"
              class="activity-ea-table__plugin-icon"
              aria-hidden="true"
            ></i>
            <span>{{ executionTitle(data) }}</span>
          </div>
        </template>
      </Column>
      <Column field="node" class="activity-ea-table__nodes">
        <template #header>
          <div class="activity-ea-table__header-content">
            <span data-testid="activity-ea-header-nodes">
              {{ $t("activity.table.nodes") }}
            </span>
            <i class="fas fa-sort" aria-hidden="true"></i>
          </div>
        </template>
        <template #body="{ data }">
          <span
            v-if="data.node && data.node.failed > 0"
            class="activity-ea-table__nodes-failed"
          >
            <i class="far fa-times-circle" aria-hidden="true"></i>
            {{ data.node.failed }} {{ $t("failed") }}
          </span>
          <span
            v-else-if="data.node && data.node.succeeded > 0"
            class="activity-ea-table__nodes-succeeded"
          >
            <i class="far fa-check-circle" aria-hidden="true"></i>
            {{ data.node.succeeded }} {{ $t("ok") }}
          </span>
        </template>
      </Column>
      <Column field="duration" class="activity-ea-table__duration">
        <template #header>
          <div class="activity-ea-table__header-content">
            <span data-testid="activity-ea-header-duration">
              {{ $t("activity.table.duration") }}
            </span>
            <i class="fas fa-sort" aria-hidden="true"></i>
          </div>
        </template>
        <template #body="{ data }">
          <span :data-testid="`activity-ea-duration-${data.id}`">
            {{ executionDuration(data) }}
          </span>
        </template>
      </Column>
      <Column field="user" class="activity-ea-table__executor">
        <template #header>
          <div class="activity-ea-table__header-content">
            <span data-testid="activity-ea-header-executor">
              {{ $t("activity.table.executor") }}
            </span>
            <i class="fas fa-sort" aria-hidden="true"></i>
          </div>
        </template>
        <template #body="{ data }">
          {{ data.user }}
        </template>
      </Column>

      <template #empty>
        <span data-testid="activity-ea-empty">
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
import type { Pageable } from "../../../library/interfaces/UiTypes";
import type { Execution } from "../../../library/types/executions/Execution";
import * as DateTimeFormatters from "../../utilities/DateTimeFormatters";
import type { ActivityPageRequest } from "./ActivityListEaTableTypes";

/**
 * PrimeVue presentation of completed executions for ActivityList's EA mode.
 */
export default defineComponent({
  name: "ActivityListEaTable",
  components: {
    Column,
    PtDataTable,
  },
  props: {
    reports: {
      type: Array as PropType<Execution[]>,
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
    bulkEditMode: {
      type: Boolean,
      required: true,
    },
    selectedIds: {
      type: Array as PropType<number[]>,
      required: true,
    },
    momentJobFormat: {
      type: String,
      default: "M/DD/YY h:mm a",
    },
  },
  emits: {
    page: (request: ActivityPageRequest) =>
      Number.isFinite(request.offset) && Number.isFinite(request.max),
    "row-click": (execution: Execution) => execution.id !== undefined,
    "update:selectedIds": (ids: number[]) => Array.isArray(ids),
  },
  computed: {
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
    currentPageReportTemplate(): string {
      return this.$t("activity.table.pageReport", {
        first: "{first}",
        last: "{last}",
        totalRecords: "{totalRecords}",
      });
    },
  },
  methods: {
    isSelected(execution: Execution): boolean {
      return (
        execution.id !== undefined && this.selectedIds.includes(execution.id)
      );
    },
    toggleSelection(execution: Execution) {
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
      });
    },
    onRowClick(event: { data: Execution }) {
      this.$emit("row-click", event.data);
    },
    completedDate(execution: Execution): string {
      return execution.dateEnded?.date
        ? DateTimeFormatters.formatDate(
            execution.dateEnded.date,
            this.momentJobFormat,
          )
        : "";
    },
    executionTitle(execution: Execution): string {
      if (execution.job) {
        return execution.job.group
          ? `${execution.job.group}/${execution.job.name}`
          : execution.job.name || "";
      }
      return execution.description || "";
    },
    pluginIconClass(execution: Execution): string {
      const title = this.executionTitle(execution).toLowerCase();
      if (title.includes("jira")) {
        return "fab fa-jira";
      }
      if (title.includes("kubernetes")) {
        return "fas fa-dharmachakra";
      }
      if (title.includes("aws")) {
        return "fab fa-aws";
      }
      return "fas fa-puzzle-piece";
    },
    executionDuration(execution: Execution): string {
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
.activity-ea-table__toolbar {
  display: flex;
  align-items: center;
  gap: var(--sizes-4);
  min-height: var(--sizes-10);
  margin-bottom: var(--sizes-3);
}

.activity-ea-table__count {
  flex: 0 0 auto;
  color: var(--colors-gray-800-original);
  font-size: 16px;
  font-weight: var(--fontWeights-medium);
  line-height: 24px;
}

.activity-ea-table__filter {
  display: flex;
  flex: 0 0 calc(var(--sizes-52) + var(--sizes-2));
  align-items: center;
  gap: var(--sizes-2\.5);
  height: calc(var(--sizes-9) + (var(--sizes-px) * 2));
  padding: var(--sizes-2\.5);
  border: var(--sizes-px) solid var(--colors-gray-300-original);
  border-radius: var(--radii-md);
  background: var(--colors-white);
  color: var(--colors-gray-800-original);
  font-size: 14px;
  font-weight: var(--fontWeights-regular);
  line-height: 14px;

  span {
    flex: 1;
    text-align: left;
  }

  i {
    flex: 0 0 var(--sizes-3\.5);
    color: var(--colors-gray-600);
    font-size: 14px;
  }

  &:hover,
  &:focus-visible {
    border-color: var(--colors-blue-500);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 var(--sizes-0\.5) var(--colors-blue-100);
  }
}

:deep(.activity-ea-table__execution) {
  width: calc(var(--sizes-36) + var(--sizes-1\.5));
}

:deep(.activity-ea-table__plugin) {
  width: calc(var(--sizes-72) + var(--sizes-3));
}

:deep(.activity-ea-table__nodes),
:deep(.activity-ea-table__duration) {
  width: calc(var(--sizes-36) + var(--sizes-1\.5));
}

.activity-ea-table__header-content,
.activity-ea-table__execution-content,
.activity-ea-table__plugin-content {
  display: flex;
  align-items: center;
  gap: var(--sizes-2);
}

.activity-ea-table__header-content {
  white-space: nowrap;

  i {
    font-size: 14px;
    font-weight: var(--fontWeights-regular);
  }
}

.activity-ea-table__header-content input,
.activity-ea-table__execution-content input {
  flex: 0 0 var(--sizes-4);
  width: var(--sizes-4);
  height: var(--sizes-4);
  margin: var(--sizes-none);
  accent-color: var(--colors-blue-500);
}

.activity-ea-table__execution-content a {
  color: var(--colors-blue-500);
}

.activity-ea-table__plugin-content {
  white-space: nowrap;
}

.activity-ea-table__plugin-icon {
  flex: 0 0 var(--sizes-4);
  color: var(--colors-blue-500);
  font-size: 16px;
  text-align: center;
}

.activity-ea-table__plugin-icon.fa-aws {
  color: var(--colors-orange-600);
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
  gap: var(--sizes-2);

  i {
    font-size: 14px;
  }
}
</style>
