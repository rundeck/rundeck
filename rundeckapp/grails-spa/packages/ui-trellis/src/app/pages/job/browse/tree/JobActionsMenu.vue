<template>
  <dropdown
    v-if="authz"
    class="btn-group pull-right visibility-hidden"
    menu-right
    append-to-body
  >
    <btn size="xs" class="dropdown-toggle" :data-job-id="job.id">
      {{ $t("actions") }}
      <span class="caret"></span>
    </btn>
    <template #dropdown>
      <li v-if="authz['update']">
        <a
          :title="$t('scheduledExecution.action.edit.button.tooltip')"
          :href="editHref"
        >
          <i class="glyphicon glyphicon-edit"></i>
          {{ $t("scheduledExecution.action.edit.button.label") }}
        </a>
      </li>
      <li v-if="authz['read'] && authz['create']">
        <a
          :title="$t('scheduledExecution.action.duplicate.button.tooltip')"
          :href="duplicateHref"
        >
          <i class="glyphicon glyphicon-plus"></i>
          {{ $t("scheduledExecution.action.duplicate.button.label") }}
        </a>
      </li>
      <li v-if="authz['read']">
        <a
          :title="$t('scheduledExecution.action.duplicate.button.tooltip')"
          :href="duplicateOtherHref"
          data-action="copy_other_project"
          :data-job-id="job.id"
          class="page_action"
        >
          <i class="glyphicon glyphicon-plus"></i>
          {{ $t("scheduledExecution.action.duplicate.other.button.label") }}
        </a>
      </li>

      <li class="divider"></li>
      <li v-if="authz['delete']">
        <a
          :title="$t('delete.this.job')"
          :href="deleteHref"
          @click.prevent="action('delete')"
        >
          <b class="glyphicon glyphicon-remove-circle"></b>
          {{ $t("scheduledExecution.action.delete.button.label") }}
        </a>
      </li>
      <template
        v-if="
          (jobScheduled && authz['toggle_schedule']) ||
          authz['toggle_execution']
        "
      >
        <li class="divider"></li>
        <li
          v-if="jobScheduled && authz['toggle_schedule'] && !jobScheduleEnabled"
        >
          <a
            :title="$t('enable.schedule.this.job')"
            class="page_action"
            data-action="enable_job_schedule_single"
            :href="enableScheduleHref"
            @click.prevent="action('enable_schedule')"
          >
            <b class="glyphicon glyphicon-check"></b>
            {{ $t("scheduledExecution.action.enable.schedule.button.label") }}
          </a>
        </li>
        <li
          v-if="jobScheduled && authz['toggle_schedule'] && jobScheduleEnabled"
        >
          <a
            :title="$t('disable.schedule.this.job')"
            class="page_action"
            data-action="disable_job_schedule_single"
            :href="disableScheduleHref"
            @click.prevent="action('disable_schedule')"
          >
            <b class="glyphicon glyphicon-unchecked"></b>
            {{ $t("scheduledExecution.action.disable.schedule.button.label") }}
          </a>
        </li>
      </template>
      <li v-if="authz['toggle_execution'] && !jobExecutionEnabled">
        <a
          :title="$t('enable.execution.this.job')"
          class="page_action"
          data-action="enable_job_execution_single"
          :href="enableExecutionHref"
          @click.prevent="action('enable_execution')"
        >
          <b class="glyphicon glyphicon-check"></b>
          {{ $t("scheduledExecution.action.enable.execution.button.label") }}
        </a>
      </li>
      <li v-if="authz['toggle_execution'] && jobExecutionEnabled">
        <a
          :title="$t('disable.execution.this.job')"
          class="page_action"
          data-action="disable_job_execution_single"
          :href="disableExecutionHref"
          @click.prevent="action('disable_execution')"
        >
          <b class="glyphicon glyphicon-unchecked"></b>
          {{ $t("scheduledExecution.action.disable.execution.button.label") }}
        </a>
      </li>
      <template v-if="authz['read']">
        <li class="divider"></li>
        <li v-for="format in ['xml', 'yaml', 'json']">
          <a :href="downloadFormatHref(format)">
            <b class="glyphicon glyphicon-file"></b>
            {{
              $t("scheduledExecution.action.downloadformat.button.label", [
                format.toUpperCase(),
              ])
            }}
          </a>
        </li>
      </template>
      <job-scm-actions :job="job"></job-scm-actions>
      <ui-socket
        location="job-actions-menu"
        section="extended"
        :socket-data="{ job }"
      >
      </ui-socket>
    </template>
  </dropdown>
</template>

<script lang="ts">
import JobScmActions from "@/app/pages/job/browse/tree/JobScmActions.vue";
import { getRundeckContext } from "@/library";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent } from "vue";
const context = getRundeckContext();
export default defineComponent({
  name: "JobActionsMenu",
  components: { JobScmActions, UiSocket },
  props: {
    itemData: {
      type: Object,
      default: () => {},
    },
  },
  computed: {
    job(): JobBrowseItem {
      return this.itemData?.job;
    },
    jobScheduled() {
      const schedule = this.findJobMeta("schedule");
      return schedule && schedule.hasSchedule;
    },
    jobScheduleEnabled() {
      const schedule = this.findJobMeta("schedule");
      return schedule && schedule.scheduleEnabled;
    },
    jobExecutionEnabled() {
      const schedule = this.findJobMeta("schedule");
      return schedule && schedule.executionEnabled;
    },
    authz(): Object | undefined {
      return this.findJobMeta("authz");
    },
    editHref() {
      return `${context.rdBase}project/${context.projectName}/job/edit/${this.job.id}`;
    },
    duplicateHref() {
      return `${context.rdBase}project/${context.projectName}/job/copy/${this.job.id}`;
    },
    duplicateOtherHref() {
      return `${context.rdBase}project/${context.projectName}/job/copy/${this.job.id}`;
    },
    deleteHref() {
      return `${context.rdBase}project/${context.projectName}/job/delete/${this.job.id}`;
    },
    disableExecutionHref() {
      return `${context.rdBase}project/${context.projectName}/job/flipExecutionEnabled/${this.job.id}?enabled=false`;
    },
    enableExecutionHref() {
      return `${context.rdBase}project/${context.projectName}/job/flipExecutionEnabled/${this.job.id}?enabled=true`;
    },
    disableScheduleHref() {
      return `${context.rdBase}project/${context.projectName}/job/flipScheduleEnabled/${this.job.id}?enabled=false`;
    },
    enableScheduleHref() {
      return `${context.rdBase}project/${context.projectName}/job/flipScheduleEnabled/${this.job.id}?enabled=true`;
    },
  },
  methods: {
    findJobMeta(key: string) {
      return this.job?.meta?.find((meta: JobBrowseMeta) => meta.name === key)
        ?.data;
    },

    downloadFormatHref(format: string) {
      return `${context.rdBase}project/${context.projectName}/job/show/${this.job.id}?format=${format}`;
    },
    action(name: string) {
      context.eventBus.emit(`job-action-single`, { name, job: this.job });
    },
  },
});
</script>

<style scoped lang="scss"></style>
