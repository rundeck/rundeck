<script lang="ts">
import { formatDurationHumanize } from "@/app/utilities/DateTimeFormatters";
import { getRundeckContext } from "@/library";
import UiSocket from '@/library/components/utils/UiSocket.vue'
import { getJobMeta } from "@/library/services/jobBrowse";
import { defineComponent } from "vue";

const rundeckContext = getRundeckContext();

export default defineComponent({
  name: "JobStatsBasic",
  components:{
    UiSocket,
  },
  props: {
    itemData: {
      type: Object as () => Record<string, any>,
      required: true,
    },
  },
  data() {
    return {
      jobStats: null as Record<string, any> | null,
    };
  },
  computed: {
    successColor(): string {
      const ratecolors = [
        "text-success",
        "text-info",
        "text-warning",
        "text-danger",
      ];
      const ratelevels = [0.9, 0.75, 0.5];
      const idx = ratelevels.findIndex((it) => it <= this.successrate);
      return idx >= 0 ? ratecolors[idx] : ratecolors[ratecolors.length - 1];
    },
    execCount() {
      return this.jobStats?.executionCount ?? 0;
    },
    successrate() {
      return this.jobStats?.successRate ?? -1;
    },
    avgduration() {
      return this.jobStats?.averageDuration ?? 0;
    },
  },
  async mounted() {
    await this.loadStats();
  },
  methods: {
    formatNumber(val: number) {
      return val.toLocaleString();
    },
    formatPercent(val: number) {
      return (val * 100).toFixed(1) + "%";
    },
    formatDuration(val: number) {
      return formatDurationHumanize(val);
    },
    async loadStats() {
      const metadata = await getJobMeta(
        rundeckContext.projectName,
        this.itemData.uuid,
        "stats",
      );
      const stats = metadata.find((v) => v.name == "stats");
      if (stats) {
        this.jobStats = stats.data;
      } else {
        this.jobStats = null;
      }
    },
  },
});
</script>

<template>
  <div class="jobstats row">
    <ui-socket section="job-stats-basic" location="exec-count">
      <div class="col-xs-12 col-sm-4 job-stats-item">
        <span
          id="jobstat_execcount_total"
          class="job-stats-value"
          :data-execcount="execCount"
        >
          {{ formatNumber(execCount) }}
        </span>
        <span class="text-table-header">
          {{ $tc("execution", execCount) }}
        </span>
      </div>
    </ui-socket>

    <ui-socket section="job-stats-basic" location="success-rate">
      <div v-if="successrate > -1" class="col-xs-12 col-sm-4 job-stats-item">
        <span
          class="job-stats-value"
          :class="successColor"
          :data-successrate="successrate"
        >
          {{ formatPercent(successrate) }}
        </span>
        <span class="text-table-header">
          {{ $t("success.rate") }}
        </span>
      </div>
    </ui-socket>
    <ui-socket section="job-stats-basic" location="average-duration">
      <div class="col-xs-12 col-sm-4 job-stats-item">
        <span class="job-stats-value" :data-avgduration="avgduration">
          <span v-if="avgduration > 0">
            {{ formatDuration(avgduration) }}
          </span>
          <span v-else class="text-muted">-</span>
        </span>
        <span class="text-table-header">
          {{ $t("average.duration") }}
        </span>
      </div>
    </ui-socket>
  </div>
</template>

<style scoped lang="scss"></style>
