<template>
  <template v-if="scheduleData && scheduleData.hasSchedule">
    <span
      v-if="
        !jobPageStore.projectExecutionsEnabled || !scheduleData.executionEnabled
      "
      class="scheduletime disabled has_tooltip text-secondary"
      data-toggle="tooltip"
      data-container="#section-content"
      data-placement="auto bottom"
      :title="$t('disabled.schedule.run')"
    >
      <i class="glyphicon glyphicon-pause"></i>
      <span class="schedule-detail">{{ $t("disabled") }}</span>
    </span>
    <span
      v-else-if="scheduleData.executionEnabled && !scheduleData.scheduleEnabled"
      class="scheduletime disabled has_tooltip text-secondary"
      :title="$t('scheduleExecution.schedule.disabled')"
      data-toggle="tooltip"
      data-container="#section-content"
      data-placement="auto bottom"
    >
      <i class="glyphicon glyphicon-pause"></i>
      <span class="schedule-detail">{{ $t("never") }}</span>
    </span>
    <span
      v-else-if="
        scheduleData.scheduleEnabled && !jobPageStore.projectSchedulesEnabled
      "
      class="scheduletime disabled has_tooltip text-secondary"
      :title="$t('project.schedule.disabled')"
      data-toggle="tooltip"
      data-container="#section-content"
      data-placement="auto bottom"
    >
      <i class="glyphicon glyphicon-pause"></i>
      <span class="schedule-detail">{{ $t("never") }}</span>
    </span>

    <span
      v-else-if="!scheduleData.nextExecutionTime"
      class="scheduletime willnotrun has_tooltip text-warning"
      :title="$t('job.schedule.will.never.fire')"
      data-toggle="tooltip"
      data-container="#section-content"
      data-placement="auto bottom"
    >
      <i class="glyphicon glyphicon-time"></i>
      <span class="schedule-detail">{{ $t("never") }}</span>
    </span>
    <span v-else class="scheduletime" :title="title">
      <i class="glyphicon glyphicon-time text-success"></i>

      <span
        v-if="scheduleData && scheduleData.nextExecutionTime"
        class="schedule-detail"
      >
        <i18n-t
          keypath="schedule.time.in.future"
          tag="span"
          class="text-secondary"
        >
          <span class="text-success">
            {{
              formatDurationFromNowMomentHumanize(
                scheduleData["nextExecutionTime"],
              )
            }}
          </span>
        </i18n-t>
      </span>
    </span>
  </template>
</template>

<script lang="ts">
import {
  formatFromNow,
  formatTimeAtDate,
  formatDurationFromNowMomentHumanize,
} from "@/app/utilities/DateTimeFormatters";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject } from "vue";

export default defineComponent({
  name: "JobScheduleInfo",
  props: {
    itemData: {
      type: Object,
      default: () => {},
    },
  },
  setup() {
    return {
      jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
    };
  },
  computed: {
    title() {
      if (
        this.scheduleData["serverNodeUUID"] &&
        this.scheduleData["nextExecutionTime"]
      ) {
        return this.$t("schedule.on.server.x.at.y", [
          this.scheduleData["serverNodeUUID"],
          formatTimeAtDate(this.scheduleData["nextExecutionTime"]),
        ]);
      }
      return "";
    },
    scheduleData(): JobBrowseMeta | undefined {
      return this.itemData?.meta;
    },
    job(): JobBrowseItem | undefined {
      return this.itemData?.job;
    },
  },
  methods: {
    formatDurationFromNowMomentHumanize,
    formatFromNow,
  },
});
</script>

<style scoped lang="scss">
.scheduletime {
  margin-left: var(--spacing-2);
}

.scheduletime {
  .schedule-detail {
    margin-left: var(--spacing-2);
  }
}
</style>
